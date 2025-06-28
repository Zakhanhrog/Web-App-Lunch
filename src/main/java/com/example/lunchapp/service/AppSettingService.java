package com.example.lunchapp.service;

import java.time.LocalTime;

public interface AppSettingService {
    LocalTime getOrderCutoffTime();
    void setOrderCutoffTime(LocalTime cutoffTime);
}