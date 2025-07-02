package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.entity.FcmToken;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.repository.FcmTokenRepository;
import com.example.lunchapp.repository.UserRepository;
import com.example.lunchapp.service.PushNotificationService;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public PushNotificationServiceImpl(FcmTokenRepository fcmTokenRepository, UserRepository userRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void subscribe(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                existingToken -> {
                    existingToken.setUser(user);
                    fcmTokenRepository.save(existingToken);
                    logger.info("Updated existing FCM token for user: {}", user.getUsername());
                },
                () -> {
                    FcmToken newToken = new FcmToken();
                    newToken.setUser(user);
                    newToken.setToken(token);
                    fcmTokenRepository.save(newToken);
                    logger.info("Saved new FCM token for user: {}", user.getUsername());
                }
        );
    }

    @Override
    public void sendNotificationToAll(String title, String body) {
        List<String> tokens = fcmTokenRepository.findAll().stream()
                .map(FcmToken::getToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            logger.info("No FCM tokens found. Skipping notification sending.");
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(new WebpushNotification(title, body, "/assets/images/codegym_logo.png"))
                        .build())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            logger.info("{} messages were sent successfully", response.getSuccessCount());
            if (response.getFailureCount() > 0) {
                logger.warn("{} messages failed to send.", response.getFailureCount());
            }
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending multicast notification", e);
        }
    }
}