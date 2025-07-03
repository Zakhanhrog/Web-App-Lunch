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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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
    private static final String UPLOAD_DIR_NAME = "chat";

    @PostMapping("/image/chat")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ServletContext servletContext = request.getServletContext();
            String webappRoot = servletContext.getRealPath("/");
            Path projectRoot = Paths.get(webappRoot).getParent().getParent();
            Path uploadPath = projectRoot.resolve("lunch-data").resolve("images").resolve(UPLOAD_DIR_NAME);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created directory: {}", uploadPath.toAbsolutePath());
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = "chat_" + UUID.randomUUID().toString() + fileExtension;

            File dest = new File(uploadPath.toFile(), newFileName);
            file.transferTo(dest);

            logger.info("File uploaded successfully: {}", dest.getAbsolutePath());

            String fileUrl = "/lunch-data/images/" + UPLOAD_DIR_NAME + "/" + newFileName;

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}