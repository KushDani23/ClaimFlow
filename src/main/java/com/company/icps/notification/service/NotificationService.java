package com.company.icps.notification.service;

import com.company.icps.notification.entity.Notification;
import com.company.icps.notification.repository.NotificationRepository;
import com.company.icps.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void notifyUser(User recipient, String message, String subject) {
        // 1. Save In-App Notification
        Notification notification = Notification.builder()
                .recipient(recipient)
                .message(message)
                .build();
        notificationRepository.save(notification);

        // 2. Send Async Email
        sendEmailAsync(recipient.getEmail(), subject, message);
    }

    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@icps.com");
            
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
