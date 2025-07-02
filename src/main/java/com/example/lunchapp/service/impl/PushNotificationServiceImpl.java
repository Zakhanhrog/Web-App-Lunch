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

import java.util.ArrayList;
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
                    if (!existingToken.getUser().getId().equals(userId)) {
                        existingToken.setUser(user);
                        fcmTokenRepository.save(existingToken);
                        logger.info("Updated existing FCM token for user: {}", user.getUsername());
                    } else {
                        logger.info("Token already subscribed for user: {}", user.getUsername());
                    }
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
                        .setFcmOptions(WebpushFcmOptions.builder().setLink("/order/menu").build())
                        .build())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            logger.info("{} messages were sent successfully.", response.getSuccessCount());
            if (response.getFailureCount() > 0) {
                handleFailedTokens(response.getResponses(), tokens);
            }
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending multicast notification", e);
        }
    }

    @Transactional
    public void handleFailedTokens(List<SendResponse> responses, List<String> tokens) {
        List<String> tokensToDelete = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                String failedToken = tokens.get(i);
                FirebaseMessagingException exception = responses.get(i).getException();
                MessagingErrorCode errorCode = exception.getMessagingErrorCode();

                // Các lỗi này cho thấy token không còn hợp lệ hoặc đã bị thu hồi
                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                        errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                    tokensToDelete.add(failedToken);
                    logger.warn("Token {} is invalid (Reason: {}) and will be deleted.", failedToken, errorCode);
                } else {
                    logger.error("Failed to send to token {}: {}", failedToken, exception.getMessage());
                }
            }
        }

        if (!tokensToDelete.isEmpty()) {
            fcmTokenRepository.deleteAllByTokenIn(tokensToDelete);
            logger.info("Deleted {} invalid/stale tokens from the database.", tokensToDelete.size());
        }
    }
}