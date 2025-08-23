package su.kdt.minigame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP를 사용하는 웹소켓 메시지 처리를 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트(앱)가 웹소켓 연결을 맺을 때 사용할 엔드포인트를 지정합니다.
        // 예: ws://localhost:8090/ws
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // "/topic"으로 시작하는 주소를 구독하는 클라이언트에게 메시지를 브로드캐스트합니다.
        // 이것이 서버가 클라이언트에게 메시지를 보내는 채널입니다.
        registry.enableSimpleBroker("/topic");

        // "/app"으로 시작하는 주소는 @MessageMapping이 붙은 메소드로 라우팅됩니다.
        // 이것이 클라이언트가 서버에게 메시지를 보내는 채널입니다.
        registry.setApplicationDestinationPrefixes("/app");
    }
}