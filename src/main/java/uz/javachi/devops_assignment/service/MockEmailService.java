package uz.javachi.devops_assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        // Mock implementation - just logs the email
        log.info("=== EMAIL NOTIFICATION ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("=== END EMAIL ===");
        
        // In real implementation, this would:
        // 1. Connect to SMTP server
        // 2. Send actual email
        // 3. Handle errors and retries
    }
}
