package com.example.lunchapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/upload")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    // Sử dụng thư mục gốc của Volume hiện có
    private static final String UPLOAD_ROOT_DIR = "/data/food";
    private static final String CHAT_UPLOAD_DIR_NAME = "chat";

    @PostMapping("/image/chat")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Tạo đường dẫn tuyệt đối cho thư mục upload ảnh chat bên trong Volume
            Path uploadPath = Paths.get(UPLOAD_ROOT_DIR, CHAT_UPLOAD_DIR_NAME);

            // Tạo thư mục nếu nó chưa tồn tại
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created persistent storage directory for chat: {}", uploadPath.toAbsolutePath());
            }

            // Tạo tên file mới để tránh trùng lặp
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = "chat_" + UUID.randomUUID().toString() + fileExtension;

            // Tạo file đích và lưu file upload vào đó
            File dest = new File(uploadPath.toFile(), newFileName);
            file.transferTo(dest);

            logger.info("File uploaded successfully to persistent storage: {}", dest.getAbsolutePath());

            // URL để truy cập file từ trình duyệt
            String fileUrl = "/uploads/" + CHAT_UPLOAD_DIR_NAME + "/" + newFileName;

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("File upload to persistent storage failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}