package com.waimai.api.config;

import com.waimai.api.interceptor.LoginInterceptor;
import com.waimai.api.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDirPath;

    public WebMvcConfig(LoginInterceptor loginInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files from the configured upload directory (absolute path)
        Path uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (Exception ignored) {
                // Will be handled when upload is attempted
            }
        }
        String uploadPath = uploadDir.toString().replace("\\", "/");
        if (!uploadPath.endsWith("/")) uploadPath += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/ws/**", "/error", "/swagger-ui/**", "/v3/api-docs/**");

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/user/login",
                        "/api/merchant/nearby",
                        "/api/merchant/apply",
                        "/api/merchant/apply/status",
                        "/api/review/merchant/**",
                        "/api/review/rider/**",
                        "/ws/**",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/uploads/**"
                );
    }
}
