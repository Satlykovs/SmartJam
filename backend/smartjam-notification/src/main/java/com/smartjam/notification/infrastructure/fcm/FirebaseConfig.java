package com.smartjam.notification.infrastructure.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;

/**
 * Configuration class responsible for initializing the Firebase Admin SDK. Loads security
 * credentials from the
 * 'firebase-adminsdk.json' resource file.
 */
@Slf4j
@Configuration
@Profile("!debug")
public class FirebaseConfig
{
    @PostConstruct
    public void init()
    {
        try (InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-adminsdk.json");)
        {


            if (serviceAccount == null)
            {

                throw new IllegalStateException("firebase-adminsdk.json not found in resources");

            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty())
            {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }
        } catch (Exception e)
        {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase bootstrapper failed", e);
        }
    }
}
