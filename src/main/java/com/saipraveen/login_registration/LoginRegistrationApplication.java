package com.saipraveen.login_registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoginRegistrationApplication {

	public static void main(String[] args) {
		String neonUrl = "jdbc:postgresql://ep-silent-fog-azb5mawr-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
		String neonUser = "neondb_owner";
		String neonPass = "npg_jShnM6rUD0lA";

		System.setProperty("spring.datasource.url", neonUrl);
		System.setProperty("spring.datasource.username", neonUser);
		System.setProperty("spring.datasource.password", neonPass);
		System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

		SpringApplication.run(LoginRegistrationApplication.class, args);
	}

}

