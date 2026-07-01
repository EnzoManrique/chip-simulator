package com.manrique.chipsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChipSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChipSimulatorApplication.class, args);
	}

}
