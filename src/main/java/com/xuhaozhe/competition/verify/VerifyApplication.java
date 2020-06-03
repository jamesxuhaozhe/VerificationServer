package com.xuhaozhe.competition.verify;

import com.xuhaozhe.competition.verify.service.LogicHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VerifyApplication {

	public static void main(String[] args) {
		LogicHandler.start();
		SpringApplication.run(VerifyApplication.class, args);
	}

}
