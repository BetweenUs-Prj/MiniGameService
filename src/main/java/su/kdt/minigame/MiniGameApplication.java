package su.kdt.minigame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MiniGameApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniGameApplication.class, args);
	}
}

// A simple test controller to prove the server is working
@RestController
class TestController {

	@GetMapping("/test")
	public String hello() {
		return "Success! The server is running.";
	}
}