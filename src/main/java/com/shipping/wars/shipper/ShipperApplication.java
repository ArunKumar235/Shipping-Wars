package com.shipping.wars.shipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class ShipperApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShipperApplication.class, args);
	}

}
