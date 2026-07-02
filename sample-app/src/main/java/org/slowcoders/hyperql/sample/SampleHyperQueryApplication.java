package org.slowcoders.hyperql.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class SampleHyperQueryApplication {
	public static void main(String[] args) {
        if (System.getProperty("spring.profiles.active") == null) {
            System.setProperty("spring.profiles.active", "local");
        }
		SpringApplication.run(SampleHyperQueryApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void showStartup() {
		System.out.println("##############################################################");
		System.out.println("# Sample HyperQuery Application Started                      #");
		System.out.println("# Please, see the tutorial and API documentations            #");
		System.out.println("# -- Tutorial: http://localhost:7007                         #");
		System.out.println("# -- Rest API: http://localhost:7007/swagger-ui/index.html   #");
		System.out.println("##############################################################");

	}
}

