package org.zim.gamsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GamsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamsApiApplication.class, args);
	}

}
