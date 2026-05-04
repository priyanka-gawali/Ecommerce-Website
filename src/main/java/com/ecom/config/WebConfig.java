package com.ecom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/category_img/**")
                .addResourceLocations("file:C:/ecom_upload/category_img/");
        
        registry.addResourceHandler("/img/product_img/**")
        .addResourceLocations("file:C:/ecom_upload/product_img/");
        
        registry.addResourceHandler("/img/profile_img/**")
        .addResourceLocations("file:C:/ecom_upload/profile_img/");
        
       
    }
}

