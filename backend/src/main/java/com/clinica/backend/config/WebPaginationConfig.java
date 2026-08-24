package com.clinica.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebPaginationConfig implements WebMvcConfigurer {

    private final PaginationValidationInterceptor paginationValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(paginationValidationInterceptor)
                .addPathPatterns("/api/**");
    }
}
