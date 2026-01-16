package com.one.aim.service.impl;

import java.time.LocalDateTime;

import com.one.aim.bo.AddressBO;
import com.one.aim.bo.UserBO;
import com.one.aim.repo.AddressRepo;
import com.one.aim.rq.CancelPaymentRq;
import com.one.aim.rq.CreatePaymentRq;
import com.one.aim.rq.VerifyPaymentRq;
import com.one.aim.rs.CreatePaymentRs;
import com.one.aim.rs.VerifyPaymentRs;
import com.one.aim.service.OrderService;
import com.one.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.one.aim.bo.OrderBO;
import com.one.aim.bo.PaymentBO;
import com.one.aim.repo.OrderRepo;
import com.one.aim.repo.PaymentRepo;
import com.one.aim.repo.UserRepo;
import com.one.aim.service.PaymentService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import com.razorpay.RazorpayClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserRepo userRepo;
    private final PaymentRepo paymentRepo;
    private final OrderService orderService;
    private final UserActivityService userActivityService;
    private final AddressRepo addressRepo;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    // ============================================================
    // 1. CREATE RAZORPAY ORDER (NO ORDER CREATED HERE)
    // ============================================================
    @Override
    @Transactional
    public BaseRs createRazorpayOrder(CreatePaymentRq rq) throws Exception {

        Long userId = AuthUtils.findLoggedInUser().getDocId();
        UserBO user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Validate addressId
        if (rq.getAddressId() == null) {
            return ResponseUtils.failure("ADDRESS_ID_REQUIRED");
        }

        // ✅ Verify address belongs to user
        AddressBO address = addressRepo.findById(rq.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUserid().equals(userId)) {
            return ResponseUtils.failure("ADDRESS_NOT_AUTHORIZED");
        }

        // Calculate amount from CART
        long amount = orderService.calculateCartTotal(userId);

        if (amount <= 0) {
            return ResponseUtils.failure("CART_EMPTY");
        }

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject options = new JSONObject();
        options.put("amount", amount * 100); // paise
        options.put("currency", "INR");
        options.put("receipt", "PAY_" + System.currentTimeMillis());

        com.razorpay.Order razorpayOrder = razorpay.orders.create(options);
        String razorpayOrderId = razorpayOrder.get("id");

        // ✅ Save PAYMENT with addressId
        PaymentBO payment = new PaymentBO();
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setStatus("CREATED");
        payment.setPaymentMethod("ONLINE");
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAddressId(rq.getAddressId()); // ✅ STORE THE ADDRESS ID
        paymentRepo.save(payment);

        userActivityService.log(
                userId,
                "PAYMENT_INITIATED",
                "Payment initiated. RazorpayOrderId=" + razorpayOrderId + ", AddressId=" + rq.getAddressId()
        );

        CreatePaymentRs rs = new CreatePaymentRs(
                razorpayOrderId,
                amount * 100,
                "INR",
                razorpayKeyId,
                null
        );

        return ResponseUtils.success(rs);
    }

    // ============================================================
    // 2. VERIFY PAYMENT → PLACE ORDER ONLY ON SUCCESS
    // ============================================================
    @Override
    @Transactional
    public BaseRs verifyRazorpayPayment(VerifyPaymentRq rq) throws Exception {

        PaymentBO payment = paymentRepo
                .findByRazorpayOrderId(rq.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        String payload = rq.getRazorpayOrderId() + "|" + rq.getRazorpayPaymentId();
        String generatedSignature = hmacSHA256_HEX(payload, razorpayKeySecret);

        if (!generatedSignature.equals(rq.getRazorpaySignature())) {

            payment.setStatus("FAILED");
            paymentRepo.save(payment);

            userActivityService.log(
                    payment.getUser().getId(),
                    "PAYMENT_FAILED",
                    "Signature mismatch for RazorpayOrderId=" + rq.getRazorpayOrderId()
            );

            return ResponseUtils.failure("PAYMENT_VERIFICATION_FAILED");
        }

        // Payment already processed guard
        if ("PAID".equals(payment.getStatus())) {
            return ResponseUtils.failure("PAYMENT_ALREADY_PROCESSED");
        }

// PAYMENT SUCCESS
        payment.setStatus("PAID");
        payment.setRazorpayPaymentId(rq.getRazorpayPaymentId());
        payment.setRazorpaySignature(rq.getRazorpaySignature());
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepo.save(payment);

        // ✅ Resolve shipping address
        Long addressId = payment.getAddressId();
        AddressBO shippingAddress;

        if (addressId != null) {
            shippingAddress = addressRepo.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Address not found"));
        } else {
            // Fallback to default address
            shippingAddress = addressRepo.findFirstByUseridAndIsDefault(
                    payment.getUser().getId(),
                    true
            ).orElseThrow(() -> new RuntimeException("No address found"));
        }

// CREATE ORDER
        OrderBO order = orderService.placeOrderAfterPayment(
                payment.getUser().getId(),
                "ONLINE",
                payment,
                shippingAddress  //  Pass the address
        );


        userActivityService.log(
                payment.getUser().getId(),
                "PAYMENT_SUCCESS",
                "Payment successful, Order created: " + order.getOrderId()
        );

        return ResponseUtils.success(
                new VerifyPaymentRs(order.getOrderId(), "PAID")
        );
    }

    // ============================================================
    // CANCEL PAYMENT (NO ORDER EXISTS)
    // ============================================================
    @Override
    @Transactional
    public BaseRs cancelPayment(CancelPaymentRq rq) throws Exception {

        PaymentBO payment = paymentRepo
                .findByRazorpayOrderId(rq.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus("CANCELLED");
        paymentRepo.save(payment);

        userActivityService.log(
                payment.getUser().getId(),
                "PAYMENT_CANCELLED",
                "User cancelled payment. RazorpayOrderId=" + rq.getOrderId()
        );

        return ResponseUtils.success("Payment cancelled");
    }

    // ============================================================
    // Razorpay uses HEX-HMAC
    // ============================================================
    private String hmacSHA256_HEX(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKey);

        byte[] hash = mac.doFinal(data.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
