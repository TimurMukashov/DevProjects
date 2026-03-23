package com.example.devprojects.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:uploads/}")
    private String uploadPath;

    @Value("${app.upload.avatar-path:uploads/avatars/}")
    private String avatarPath;

    @Value("${app.upload.document-path:uploads/documents/}")
    private String documentPath; // Путь для документов (резюме и вложения)

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Общий обработчик для папки uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        // Обработчик для аватаров
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + avatarPath);

        // Обработчик для документов (вложения к заявкам)
        // Позволяет обращаться к файлам по URL /uploads/documents/...
        registry.addResourceHandler("/uploads/documents/**")
                .addResourceLocations("file:" + documentPath);
    }
}