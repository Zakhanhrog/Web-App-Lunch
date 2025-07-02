package com.example.lunchapp.service.impl;

import com.example.lunchapp.service.AppSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class AppSettingServiceImpl implements AppSettingService {
    private static final Logger logger = LoggerFactory.getLogger(AppSettingServiceImpl.class);

    private LocalTime orderStartTime = LocalTime.of(10, 0);
    private LocalTime orderCutoffTime = LocalTime.of(11, 15);

    @Override
    public LocalTime getOrderStartTime() {
        return this.orderStartTime;
    }

    @Override
    public void setOrderStartTime(LocalTime startTime) {
        if (startTime != null) {
            this.orderStartTime = startTime;
            logger.info("Application order start time set to: {}", this.orderStartTime);
        } else {
            logger.warn("Attempted to set null order start time. Keeping current value: {}", this.orderStartTime);
        }
    }

    @Override
    public LocalTime getOrderCutoffTime() {
        return this.orderCutoffTime;
    }

    @Override
    public void setOrderCutoffTime(LocalTime cutoffTime) {
        if (cutoffTime != null) {
            this.orderCutoffTime = cutoffTime;
            logger.info("Application order cutoff time set to: {}", this.orderCutoffTime);
        } else {
            logger.warn("Attempted to set null order cutoff time. Keeping current value: {}", this.orderCutoffTime);
        }
    }
}