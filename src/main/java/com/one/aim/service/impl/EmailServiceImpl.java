package com.one.aim.service.impl;

import com.one.aim.bo.AdminBO;
import com.one.aim.bo.SellerBO;
import com.one.aim.bo.UserBO;
import com.one.aim.constants.ErrorCodes;
import com.one.aim.repo.AdminRepo;
import com.one.aim.repo.SellerRepo;
import com.one.aim.repo.UserRepo;
import com.one.aim.service.EmailService;
import com.one.exception.EmailSendFailedException;
import com.one.service.impl.UserDetailsImpl;
import com.one.utils.TokenUtils;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserRepo userRepo;
    private final SellerRepo sellerRepo;
    private final AdminRepo adminRepo;

    @Value("${app.verify.email.url}")
    private String verifyEmailUrl;

    @Value("${app.reset.password.url}")
    private String resetPasswordUrl;

    @Value("${spring.mail.username:noreply@oneaim.com}")
    private String fromEmail;

    @Value("${token.expiry.hours}")
    private int tokenExpiryHours;

    @Async
    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            String html = buildWelcomeEmailTemplate(fullName);
            sendHtmlEmail(toEmail, "Welcome to Affordable 🎉", html);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            // don't rethrow — welcome email failure shouldn't break flow
        }
    }

    @Override
    public void checkEmailVerified(UserDetailsImpl userDetails) {
        if (userDetails.isEmailVerified()) return;
        throw new RuntimeException("Please verify your email before logging in.");
    }

    @Override
    @Transactional
    public BaseRs resendVerificationEmail(String email) {
        String cleanEmail = email.trim().toLowerCase();
        String newToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(tokenExpiryHours);

        Optional<UserBO> userOpt = userRepo.findByEmail(cleanEmail);
        if (userOpt.isPresent()) {
            UserBO user = userOpt.get();
            if (user.getEmailVerified())
                return ResponseUtils.failure(ErrorCodes.EC_EMAIL_ALREADY_VERIFIED);

            user.setVerificationToken(newToken);
            user.setVerificationTokenExpiry(expiry);
            userRepo.save(user);

            sendVerificationEmail(user.getEmail(), user.getFullName(), newToken);
            return ResponseUtils.success("Verification email sent.");
        }

        Optional<SellerBO> sellerOpt = sellerRepo.findByEmailIgnoreCase(cleanEmail);
        if (sellerOpt.isPresent()) {
            SellerBO seller = sellerOpt.get();
            if (seller.isEmailVerified())
                return ResponseUtils.failure(ErrorCodes.EC_EMAIL_ALREADY_VERIFIED);

            seller.setVerificationToken(newToken);
            seller.setVerificationTokenExpiry(expiry);
            sellerRepo.save(seller);

            sendVerificationEmail(seller.getEmail(), seller.getFullName(), newToken);
            return ResponseUtils.success("Verification email sent.");
        }

        Optional<AdminBO> adminOpt = adminRepo.findByEmailIgnoreCase(cleanEmail);
        if (adminOpt.isPresent()) {
            AdminBO admin = adminOpt.get();
            if (admin.isEmailVerified())
                return ResponseUtils.failure(ErrorCodes.EC_EMAIL_ALREADY_VERIFIED);

            admin.setVerificationToken(newToken);
            admin.setVerificationTokenExpiry(expiry);
            adminRepo.save(admin);

            sendVerificationEmail(admin.getEmail(), admin.getFullName(), newToken);
            return ResponseUtils.success("Verification email sent.");
        }

        return ResponseUtils.failure(ErrorCodes.EC_USER_NOT_FOUND, "Email not registered.");
    }

    @Override
    public void initiateUserEmailChange(UserBO user, String newEmail) {
        String token = UUID.randomUUID().toString();
        user.setPendingEmail(newEmail);
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(tokenExpiryHours));
        userRepo.save(user);
        sendVerificationEmail(newEmail, user.getFullName(), token);
    }

    @Override
    public void initiateSellerEmailChange(SellerBO seller, String newEmail) {
        String token = UUID.randomUUID().toString();
        seller.setPendingEmail(newEmail);
        seller.setVerificationToken(token);
        seller.setVerificationTokenExpiry(LocalDateTime.now().plusHours(tokenExpiryHours));
        seller.setEmailVerified(false);
        seller.setLocked(true);
        sellerRepo.save(seller);
        sendVerificationEmail(newEmail, seller.getFullName(), token);
    }

    @Override
    public void initiateAdminEmailChange(AdminBO admin, String newEmail) {
        String token = UUID.randomUUID().toString();
        admin.setEmail(newEmail);
        admin.setVerificationToken(token);
        admin.setVerificationTokenExpiry(LocalDateTime.now().plusHours(tokenExpiryHours));
        admin.setEmailVerified(false);
        adminRepo.save(admin);
        sendVerificationEmail(newEmail, admin.getFullName(), token);
    }

    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            String subject = "Verify Your Email - Affordable Platform";
            String link = verifyEmailUrl + "?token=" + token;
            String htmlContent = buildVerificationEmailTemplate(fullName, link);
            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new EmailSendFailedException("Unable to send verification email.");
        }
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String token) {
        try {
            String resetURL = resetPasswordUrl + "?token=" + token;
            String html = buildResetPasswordEmailTemplate(resetURL);
            sendHtmlEmail(toEmail, "Reset Password", html);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", toEmail, e.getMessage(), e);
            throw new EmailSendFailedException("Unable to send reset password email.");
        }
    }

    @Async
    public void sendSellerUnderReviewEmail(String toEmail, String fullName) {
        String subject = "Seller Account Under Review";
        String html = """
            <html>
                                    <body style="font-family: Arial, sans-serif; color: #333;">
                                      <h2>Hello %s,</h2>
                
                                      <p>
                                        Your email address has been <b>successfully verified</b>.
                                      </p>
                
                                      <p>
                                        Your seller account is now <b>under review</b> by our admin team.
                                      </p>
                
                                      <p>
                                        The review process typically takes <b>3–7 business days</b>. We’ll notify you once a decision has been made.
                                      </p>
                
                                      <br>
                
                                      <p>
                                        Regards,<br>
                                        <b>Affordable Team</b>
                                      </p>
                                    </body>
                                  </html>
                
        """.formatted(fullName);

        try {
            sendHtmlEmail(toEmail, subject, html);
        } catch (Exception e) {
            log.error("Failed to send Seller Under Review email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendSellerApprovalEmail(String toEmail, String fullName) {
        String subject = "Seller Account Approved 🎉";
        String html = """
            <html>
                                    <body style="font-family: Arial, sans-serif; color: #333;">
                                      <h2>🎉 Congratulations, %s!</h2>
                
                                      <p>We’re excited to inform you that your <b>seller account has been successfully approved</b> by our admin team.</p>
                
                                      <p>You can now log in and start publishing your products on <b>OneAim</b>.</p>
                
                                      <p>We’re thrilled to have you on board and look forward to your success with us.</p>
                
                                      <br>
                
                                      <p>
                                        Best regards,<br>
                                        <b>Team Affordable</b>
                                      </p>
                                    </body>
                                  </html>
                
        """.formatted(fullName);

        try {
            sendHtmlEmail(toEmail, subject, html);
        } catch (Exception e) {
            log.error("Failed to send Seller Approval email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendSellerRejectionEmail(String to, String name) {
        try {
            String subject = "Your Seller Application Has Been Rejected";

            String body = """
                    <html>
                                     <body style="font-family: Arial, sans-serif; color: #333;">
                                       <p>Hello %s,</p>
                    
                                       <p>
                                         We regret to inform you that your seller application has been <b>rejected</b>.
                                       </p>
                    
                                       <p>
                                         This decision was made due to incorrect or incomplete document verification.
                                       </p>
                    
                                       <p>
                                         You may reapply after reviewing and correcting the identified issues.
                                       </p>
                    
                                       <br>
                    
                                       <p>
                                         Regards,<br>
                                         <b>Affordable Team</b>
                                       </p>
                                     </body>
                                   </html>
                    
                    """.formatted(name);

            sendHtmlEmail(to, subject, body);

        } catch (Exception e) {
            log.error("Failed to send rejection email: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String buildVerificationEmailTemplate(String fullName, String link) {
        return """
            <html>
                                    <body style="font-family: Arial, sans-serif; color: #333;">
                                      <h2>Hello %s,</h2>
                
                                      <p>
                                        Thank you for signing up! Please confirm your email address by clicking the button below.
                                      </p>
                
                                      <p style="text-align: center; margin: 20px 0;">
                                        <a href="%s"
                                           style="padding: 12px 24px; background-color: #007BFF; color: #ffffff;
                                                  border-radius: 5px; text-decoration: none; font-weight: bold;">
                                          Verify Email
                                        </a>
                                      </p>
                
                                      <p>
                                        This verification link will expire in <b>24 hours</b>. If you did not create an account,
                                        you can safely ignore this email.
                                      </p>
                
                                      <br>
                
                                      <p>
                                        Regards,<br>
                                        <b>Affordable Team</b>
                                      </p>
                                    </body>
                                  </html>
                
        """.formatted(fullName, link);
    }

    private String buildResetPasswordEmailTemplate(String resetURL) {
        return """
            <html>
                                    <body style="font-family: Arial, sans-serif; color: #333;">
                                      <h2>Reset Your Password</h2>
                
                                      <p>
                                        We received a request to reset your account password. Click the button below to proceed.
                                      </p>
                
                                      <p style="text-align: center; margin: 20px 0;">
                                        <a href="%s"
                                           style="padding: 12px 24px; background-color: #28a745; color: #ffffff;
                                                  text-decoration: none; border-radius: 5px; font-weight: bold;">
                                          Reset Password
                                        </a>
                                      </p>
                
                                      <p>
                                        This link is valid for a limited time. If you did not request a password reset,
                                        you can safely ignore this email.
                                      </p>
                
                                      <br>
                
                                      <p>
                                        Regards,<br>
                                        <b>Affordable Team</b>
                                      </p>
                                    </body>
                                  </html>
                
        """.formatted(resetURL);
    }

    private String buildWelcomeEmailTemplate(String fullName) {
        return """
            <html>
                                    <body style="font-family: Arial, sans-serif; color: #333;">
                                      <h2>🎉 Welcome, %s!</h2>
                
                                      <p>
                                        Thank you for joining <b>Affordable</b>.
                                      </p>
                
                                      <p>
                                        We’re excited to have you on board and look forward to helping you get started.
                                      </p>
                
                                      <br>
                
                                      <p>
                                        Best regards,<br>
                                        <b>Affordable Team</b>
                                      </p>
                                    </body>
                                  </html>
                
        """.formatted(fullName);
    }

    private boolean isExpired(LocalDateTime expiry) {
        return expiry == null || expiry.isBefore(LocalDateTime.now());
    }
}
