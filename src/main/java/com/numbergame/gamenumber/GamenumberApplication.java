package com.numbergame.gamenumber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
@EnableAsync
public class GamenumberApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamenumberApplication.class, args);
	}

}
