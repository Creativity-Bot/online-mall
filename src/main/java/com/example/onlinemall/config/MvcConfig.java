package com.example.onlinemall.config;

import com.example.onlinemall.utils.LoginInterceptor;
import com.example.onlinemall.utils.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/sendcode",
                        "/user/login"
                ).order(1);
        // token刷新的拦截器
        registry.addInterceptor(new TokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
