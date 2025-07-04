package com.example.lunchapp.service.impl;

import com.example.lunchapp.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${LUNCH_APP_UPLOAD_DIR:#{null}}")
    private String uploadDirFromEnv;

    public static final String WEB_UPLOAD_PATH = "/uploads";
    private static final String FOOD_DIR_NAME = "food";
    private static final String CHAT_DIR_NAME = "chat";

    private Path rootUploadPath;
    private Path foodImageUploadPath;
    private Path chatImageUploadPath;

    @PostConstruct
    public void init() throws IOException {
        if (uploadDirFromEnv != null && !uploadDirFromEnv.isEmpty()) {
            // Chạy trên server (Railway), sử dụng biến môi trường
            this.rootUploadPath = Paths.get(uploadDirFromEnv);
        } else {
            // Chạy ở local, sử dụng thư mục home của người dùng
            String userHome = System.getProperty("user.home");
            this.rootUploadPath = Paths.get(userHome, ".lunch-app-data");
        }

        this.rootUploadPath = this.rootUploadPath.toAbsolutePath().normalize();
        Files.createDirectories(this.rootUploadPath);

        this.foodImageUploadPath = this.rootUploadPath.resolve(FOOD_DIR_NAME);
        Files.createDirectories(this.foodImageUploadPath);

        this.chatImageUploadPath = this.rootUploadPath.resolve(CHAT_DIR_NAME);
        Files.createDirectories(this.chatImageUploadPath);
    }

    @Override
    public String getUploadDirectory() {
        return this.rootUploadPath.toString();
    }

    @Override
    public String getWebUploadPath() {
        return WEB_UPLOAD_PATH;
    }

    @Override
    public Path getFoodImageUploadPath() {
        return this.foodImageUploadPath;
    }

    @Override
    public Path getChatImageUploadPath() {
        return this.chatImageUploadPath;
    }
}