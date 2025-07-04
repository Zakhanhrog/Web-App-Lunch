package com.example.lunchapp.service;

import java.nio.file.Path;

public interface FileStorageService {
    String getUploadDirectory();
    String getWebUploadPath();
    Path getFoodImageUploadPath();
    Path getChatImageUploadPath();
}