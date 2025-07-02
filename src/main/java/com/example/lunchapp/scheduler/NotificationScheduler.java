package com.example.lunchapp.scheduler;

import com.example.lunchapp.service.AppSettingService;
import com.example.lunchapp.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final PushNotificationService pushNotificationService;
    private final AppSettingService appSettingService;

    public NotificationScheduler(PushNotificationService pushNotificationService, AppSettingService appSettingService) {
        this.pushNotificationService = pushNotificationService;
        this.appSettingService = appSettingService;
    }

    // Chạy vào lúc 8:00 sáng hàng ngày (từ thứ 2 đến thứ 6)
    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "Asia/Ho_Chi_Minh")
    public void sendOrderOpeningNotification() {
        LocalTime startTime = appSettingService.getOrderStartTime();
        logger.info("Scheduler is running to send order opening notifications. Configured start time: {}", startTime);

        String title = "Đến giờ đặt cơm trưa rồi!";
        String body = "Hệ thống mở cửa lúc " + startTime.toString() + ". Mời bạn vào đặt món ngay!";

        pushNotificationService.sendNotificationToAll(title, body);
    }
}