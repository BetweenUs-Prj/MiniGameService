package su.kdt.minigame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
            // H2 콘솔을 사용하기 위해 iframe 관련 보안 설정을 비활성화합니다.
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
            
        return http.build();
    }
}