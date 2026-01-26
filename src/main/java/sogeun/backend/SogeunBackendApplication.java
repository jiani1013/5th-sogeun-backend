package sogeun.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SogeunBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SogeunBackendApplication.class, args);
	}

}
