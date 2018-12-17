package com.pikaqiu.elastic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-11-22 23:22
 **/
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    /**
     * 静态资源加载配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

}
