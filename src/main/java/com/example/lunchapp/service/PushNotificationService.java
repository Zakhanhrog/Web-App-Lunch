package com.example.lunchapp.service;

public interface PushNotificationService {
    void subscribe(Long userId, String token);
    void sendNotificationToAll(String title, String body);
}