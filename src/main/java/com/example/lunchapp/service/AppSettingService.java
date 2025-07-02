package com.example.lunchapp.service;

import java.time.LocalTime;

public interface AppSettingService {
    LocalTime getOrderStartTime();
    void setOrderStartTime(LocalTime startTime);
    LocalTime getOrderCutoffTime();
    void setOrderCutoffTime(LocalTime cutoffTime);
}