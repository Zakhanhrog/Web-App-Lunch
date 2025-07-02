package com.example.lunchapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Attempting to initialize Firebase...");

                InputStream serviceAccountStream = getServiceAccountStream();

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase has been initialized successfully.");
            } else {
                logger.info("Firebase app is already initialized.");
            }
        } catch (IOException e) {
            logger.error("Error initializing Firebase", e);
            throw new RuntimeException(e);
        }
    }

    private InputStream getServiceAccountStream() throws IOException {
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
        if (credentialsJson != null && !credentialsJson.isEmpty()) {
            logger.info("Initializing Firebase from GOOGLE_CREDENTIALS_JSON environment variable.");
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        logger.info("GOOGLE_CREDENTIALS_JSON not found. Falling back to serviceAccountKey.json file for local development.");
        InputStream serviceAccountFile = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
        if (serviceAccountFile == null) {
            logger.error("!!! CRITICAL: serviceAccountKey.json not found in resources. Firebase features will not work in local environment.");
            throw new IOException("serviceAccountKey.json not found in the classpath.");
        }
        return serviceAccountFile;
    }
}