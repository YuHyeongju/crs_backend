package com.hyeongju.crs.crs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // toURI()는 OS에 따라 file:/C:/... (윈도우) 또는 file:/var/... (리눅스) 형태를 알맞게 만들어준다.
        // 디렉터리가 실제로 존재해야 끝에 슬래시가 붙으므로 위에서 미리 생성한다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(dir.toURI().toString());
    }
}