package com.saipraveen.login_registration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@org.springframework.cache.annotation.EnableCaching
public class LoginRegistrationApplication {

	@PostConstruct
	public void init() {
		// Set JVM default timezone to Indian Standard Time (IST / Asia/Kolkata)
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(LoginRegistrationApplication.class, args);
	}

}
