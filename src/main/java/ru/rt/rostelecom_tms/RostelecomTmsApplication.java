package ru.rt.rostelecom_tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RostelecomTmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RostelecomTmsApplication.class, args);
	}

}
