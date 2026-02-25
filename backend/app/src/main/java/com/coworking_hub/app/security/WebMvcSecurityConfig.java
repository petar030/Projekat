package com.coworking_hub.app.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcSecurityConfig(
            JwtAuthInterceptor jwtAuthInterceptor,
            CurrentUserArgumentResolver currentUserArgumentResolver
    ) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/member/**", "/api/manager/**", "/api/admin/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
