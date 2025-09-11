package su.kdt.minigame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    "http://localhost:*", 
                    "http://127.0.0.1:*",
                    "https://localhost:*", 
                    "https://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders(
                    "X-USER-UID", 
                    "uid", 
                    "Location", 
                    "X-Request-ID", 
                    "X-Response-Time",
                    "Access-Control-Allow-Origin",
                    "Access-Control-Allow-Credentials",
                    "x-round-phase"
                ) // 타임아웃 재시도 시 필요한 헤더들
                .allowCredentials(true)
                .maxAge(7200); // SecurityConfig와 동일하게 2시간
    }
}