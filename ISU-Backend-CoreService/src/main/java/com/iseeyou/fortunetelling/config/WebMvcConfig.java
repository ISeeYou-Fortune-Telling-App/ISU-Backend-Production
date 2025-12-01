package com.iseeyou.fortunetelling.config;

import com.iseeyou.fortunetelling.config.converter.StringToAvailableTimeSlotsConverter;
import com.iseeyou.fortunetelling.config.converter.StringToMultipartFileConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringToAvailableTimeSlotsConverter stringToAvailableTimeSlotsConverter;
    private final StringToMultipartFileConverter stringToMultipartFileConverter;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
        registry.addRedirectViewController("/docs", "/swagger-ui/index.html");
        registry.addRedirectViewController("/api-docs", "/swagger-ui/index.html");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToAvailableTimeSlotsConverter);
        registry.addConverter(stringToMultipartFileConverter);
    }
}
