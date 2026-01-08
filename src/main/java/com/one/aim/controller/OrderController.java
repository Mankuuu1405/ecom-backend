package com.one.aim.controller;

import com.one.aim.bo.*;
import com.one.aim.mapper.OrderMapper;
import com.one.aim.repo.AddressRepo;
import com.one.aim.repo.CartRepo;
import com.one.aim.repo.OrderRepo;
import com.one.aim.rs.OrderSummaryRs;
import com.one.aim.rs.UserRs;
import com.one.aim.service.*;
import com.one.utils.AuthUtils;
import com.one.vm.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.one.aim.constants.MessageCodes;
import com.one.aim.rq.OrderRq;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final FileService fileService;
    private final AdminSettingService adminSettingService;
    private final CartRepo cartRepo;
    private final ChargesService chargesService;
    private final AddressRepo addressRepo;

    // ---------------------------------------------------------
// PLACE ORDER (COD ONLY)
// ---------------------------------------------------------
    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRq rq) throws Exception {

        Long userId = AuthUtils.findLoggedInUser().getDocId();
        if (userId == null) {
            return ResponseEntity
                    .badRequest()
                    .body("User not authenticated");
        }

        // Block ONLINE here
        if (!"COD".equalsIgnoreCase(rq.getPaymentMethod())) {
            return ResponseEntity
                    .badRequest()
                    .body("ONLINE payment is not allowed here");
        }

        //  NEW: Resolve address before placing order
        AddressBO shippingAddress = resolveShippingAddress(rq, userId);

        OrderBO order = orderService.placeOrderAfterPayment(
                userId,
                "COD",
                null,          // no PaymentBO for COD
                shippingAddress  //  Pass the address
        );

        OrderSummaryRs summary =
                OrderMapper.toOrderSummary(order, fileService);

        return ResponseEntity.ok(
                ResponseUtils.success(summary)
        );
    }

    //  NEW: Add this helper method to your controller or move to service
    private AddressBO resolveShippingAddress(OrderRq rq, Long userId) {

        // If addressId explicitly provided → use it
        if (rq.getAddressId() != null) {
            AddressBO address = addressRepo.findById(rq.getAddressId())
                    .orElseThrow(() -> new RuntimeException("Invalid addressId"));

            if (!address.getUserid().equals(userId))
                throw new RuntimeException("Address does not belong to user");

            return address;
        }

        //  Try default address
        Optional<AddressBO> defaultAddress =
                addressRepo.findFirstByUseridAndIsDefault(userId, true);

        if (defaultAddress.isPresent()) {
            return defaultAddress.get();
        }

        //  NO DEFAULT → create TEMP address from checkout payload
        AddressBO temp = new AddressBO();
        temp.setUserid(userId);
        temp.setFullName(rq.getFullName());
        temp.setStreet(rq.getStreet());
        temp.setCity(rq.getCity());
        temp.setState(rq.getState());
        temp.setZip(rq.getZip());
        temp.setCountry(rq.getCountry());
        temp.setPhone(rq.getPhone());

        temp.setIsDefault(false); //  not saved as default
//        temp.setEnabled(false);   //  optional: not reusable

        return temp;
    }



    // ---------------------------------------------------------
    // GET SINGLE ORDER
    // ---------------------------------------------------------
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> retrieveOrder(@PathVariable String orderId) throws Exception {

        log.debug("Executing RESTfulService [GET /orders/{}]", orderId);

        return ResponseEntity.ok(orderService.retrieveOrder(orderId));
    }



    // ---------------------------------------------------------
    // GET ALL ORDERS (ADMIN)
    // ---------------------------------------------------------
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status
    ) throws Exception {
        return ResponseEntity.ok(
                orderService.retrieveOrders(page, size, sortBy, direction, status , paymentMethod)
        );
    }


    // ---------------------------------------------------------
    // UPDATE DELIVERY STATUS
    // ---------------------------------------------------------
    @GetMapping("/status/update")
    public ResponseEntity<?> updateDeliveryStatus(
            @RequestParam String orderId,
            @RequestParam String status) throws Exception {

        orderService.updateDeliveryStatus(orderId, status);
        return ResponseEntity.ok(MessageCodes.MC_UPDATED_SUCCESSFULLY);
    }

    // ---------------------------------------------------------
    // GET ORDERS OF LOGGED-IN USER
    // ---------------------------------------------------------
    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders() throws Exception {
        return ResponseEntity.ok(orderService.retrieveOrdersUser());
    }

    // ---------------------------------------------------------
    // GET ALL USERS' ORDERS (ADMIN)
    // ---------------------------------------------------------
    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsersOrders() throws Exception {
        return ResponseEntity.ok(orderService.retrieveAllOrders());
    }

    // ---------------------------------------------------------
    // CANCEL ORDER (USER)
    // ---------------------------------------------------------
    @DeleteMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) throws Exception {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @GetMapping("/calculate-charges")
    public ResponseEntity<?> calculateCharges(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) String promoCode
    ) throws Exception {

        return chargesService.calculate(userId, addressId, promoCode);
    }



//    @GetMapping("/invoice/{fileName:.+}")
//    public ResponseEntity<?> downloadInvoice(@PathVariable String fileName) {
//        try {
//            UserRs logged = AuthUtils.findLoggedInUser();
//            String role = logged.getRoll();
//            Long loggedUserDbId = logged.getDocId();
//
//            // prevent traversal
//            if (!fileName.matches("^invoice-[A-Za-z0-9_-]+(\\.pdf)$")) {
//                return ResponseEntity.badRequest().body("Invalid filename");
//            }
//
//            // determine orderId from filename
//            String orderId = null;
//            Long sellerDbIdFromFile = null;
//
//            if (fileName.contains("-SEL-")) {
//                // SELLER file: invoice-<orderId>-SEL-<sellerId>.pdf
//                String[] parts = fileName.replace(".pdf","").split("-SEL-");
//                orderId = parts[0].replace("invoice-","");
//                sellerDbIdFromFile = Long.valueOf(parts[1]);
//            }
//            else if (fileName.contains("-ADMIN.pdf")) {
//                // ADMIN file: invoice-<orderId>-ADMIN.pdf
//                orderId = fileName.replace(".pdf","").replace("invoice-","").replace("-ADMIN","");
//            }
//            else {
//                // USER file: invoice-<orderId>.pdf
//                orderId = fileName.replace(".pdf","").replace("invoice-","");
//            }
//
//            InvoiceBO invoice = invoiceService.getInvoiceByOrderId(orderId);
//            if (invoice == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Invoice not found.");
//            }
//
//            // ===================== ROLE VALIDATION =====================
//            byte[] pdfBytes;
//
//            switch (role.toUpperCase()) {
//
//                case "USER": {
//                    if (!invoice.getUser().getId().equals(loggedUserDbId)) {
//                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not your invoice");
//                    }
//                    pdfBytes = invoiceService.downloadInvoicePdf(orderId);
//                    break;
//                }
//
//                case "SELLER": {
//                    if (sellerDbIdFromFile == null || !sellerDbIdFromFile.equals(loggedUserDbId)) {
//                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                                .body("Not your seller invoice file");
//                    }
//                    pdfBytes = invoiceService.downloadSellerInvoicePdf(orderId, loggedUserDbId);
//                    if (pdfBytes == null) {
//                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                                .body("This seller is not part of this order");
//                    }
//                    break;
//                }
//
//                case "ADMIN": {
//                    if (!fileName.contains("-ADMIN.pdf")) {
//                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                                .body("Admins must download ADMIN invoices only");
//                    }
//                    pdfBytes = invoiceService.downloadAdminInvoice(orderId);
//                    break;
//                }
//
//                default:
//                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                            .body("Invalid user role");
//            }
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_PDF);
//            headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
//            headers.add("Access-Control-Expose-Headers", "Content-Disposition");
//
//            return ResponseEntity.ok().headers(headers).body(pdfBytes);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Unable to process invoice request");
//        }
//    }


}
