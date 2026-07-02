package com.example.certificateportal.config;

import com.example.certificateportal.trace.TraceLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TraceLogInterceptor traceLogInterceptor;

    public WebConfig(TraceLogInterceptor traceLogInterceptor) {
        this.traceLogInterceptor = traceLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceLogInterceptor);
    }
}
