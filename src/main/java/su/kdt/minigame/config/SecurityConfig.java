package su.kdt.minigame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 모든 HTTP 요청에 대해 인증 없이 접근을 허용합니다.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // CSRF 보호 기능을 비활성화합니다. (API 서버에서는 보통 비활성화합니다)
            .csrf(csrf -> csrf.disable())
            // CORS 설정을 활성화합니다.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // H2 콘솔을 사용하기 위해 iframe 관련 보안 설정을 비활성화합니다.
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
            
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 🚀 개발 환경에서 모든 localhost 포트 허용 (타임아웃 재시도 지원)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*", 
            "http://127.0.0.1:*",
            "https://localhost:*", 
            "https://127.0.0.1:*"
        ));
        
        // 🔥 더 광범위한 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // 💡 타임아웃 재시도 시 필요한 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type", 
            "X-USER-UID", 
            "uid", // 임시 호환성 헤더
            "Authorization", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cache-Control",
            "Connection", // 타임아웃 처리 시 필요
            "*"
        ));
        
        // ⚡ 클라이언트에서 접근 가능한 응답 헤더 확장
        configuration.setExposedHeaders(Arrays.asList(
            "Location", 
            "X-USER-UID", 
            "uid",
            "X-Request-ID", 
            "X-Response-Time",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        configuration.setAllowCredentials(true);
        
        // 🚀 preflight 캐시 시간 증가 (성능 향상)
        configuration.setMaxAge(7200L); // 2시간
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}