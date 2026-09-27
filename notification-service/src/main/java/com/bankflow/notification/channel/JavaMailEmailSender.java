package com.bankflow.notification.channel;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.exception.DeliveryException;
import com.bankflow.notification.model.NotificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Email Dispatcher using Spring JavaMailSender with safe fallback.
 */
@Component
public class JavaMailEmailSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailSender.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:alerts@bankflow.internal}")
    private String fromEmail;

    @Value("${bankflow.mail.mock:true}")
    private boolean mockMode;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRecord record) {
        log.info("[JAVAMAIL] Dispatching Email to <{}> Subject: '{}'", record.getRecipient(), record.getSubject());

        if (mockMode || mailSender == null) {
            log.info("[JAVAMAIL (Mock)] Emulated SMTP dispatch to {}. Message body:\n{}", record.getRecipient(), record.getBody());
            record.setStatus("DELIVERED");
            record.setDeliveredAt(Instant.now());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(record.getRecipient());
            message.setSubject(record.getSubject());
            message.setText(record.getBody());
            mailSender.send(message);

            record.setStatus("DELIVERED");
            record.setDeliveredAt(Instant.now());
            log.info("[JAVAMAIL] Successfully sent email to {}", record.getRecipient());
        } catch (Exception ex) {
            log.error("[JAVAMAIL] Failed to send email to {}: {}", record.getRecipient(), ex.getMessage());
            throw new DeliveryException("JavaMail SMTP error: " + ex.getMessage(), ex);
        }
    }
}
