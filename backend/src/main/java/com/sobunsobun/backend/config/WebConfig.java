package com.sobunsobun.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // UTF-8 인코딩 설정 (한글 깨짐 방지)
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // Accept-Charset 헤더 생성 방지
        converters.add(stringConverter);
    }
}
