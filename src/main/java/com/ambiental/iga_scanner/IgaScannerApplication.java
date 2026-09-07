package com.ambiental.iga_scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IgaScannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IgaScannerApplication.class, args);
	}

}
