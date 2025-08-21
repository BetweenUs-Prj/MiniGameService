package su.kdt.minigame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories // ◀◀◀ Add this annotation
@SpringBootApplication
public class MiniGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniGameApplication.class, args);
    }

}