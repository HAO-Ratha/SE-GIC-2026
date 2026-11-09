package edu.itc.se.shop_jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import edu.itc.se.shop_jpa.config.ShopProperties;

@SpringBootApplication
@EnableConfigurationProperties(ShopProperties.class)
public class ShopJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopJpaApplication.class, args);
	}

}
