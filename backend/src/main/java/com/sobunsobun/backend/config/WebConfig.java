package com.sobunsobun.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.url-prefix:/files}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // OS 경로를 file: 스킴으로 노출
        String location = "file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/");
        String pattern  = (urlPrefix.endsWith("/**") ? urlPrefix : urlPrefix + "/**");
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
