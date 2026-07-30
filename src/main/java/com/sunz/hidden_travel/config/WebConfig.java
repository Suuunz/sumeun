package com.sunz.hidden_travel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 업로드된 후기 사진을 정적 리소스로 노출한다.
 * /uploads/** → ./uploads/** (LocalImageStorage 가 저장하는 위치)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String urlPrefix;

    public WebConfig(@Value("${app.upload.dir:./uploads}") String uploadDir,
                     @Value("${app.upload.url-prefix:/uploads}") String urlPrefix) {
        this.uploadDir = uploadDir;
        this.urlPrefix = urlPrefix;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlPrefix + "/**").addResourceLocations(location);
    }
}
