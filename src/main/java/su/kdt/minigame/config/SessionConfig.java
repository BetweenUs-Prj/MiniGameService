package su.kdt.minigame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.session")
@Data
public class SessionConfig {
    private int maxPlayers = 10;
}