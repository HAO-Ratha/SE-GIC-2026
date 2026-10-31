package edu.itc.se.shop.config;

import edu.itc.se.shop.service.ReportService;
import edu.itc.se.shop.service.WelcomeService;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupRunner {

    @Bean
    CommandLineRunner beanReport(ApplicationContext ctx,
                                 WelcomeService welcome,
                                 ReportService reports) {
        return args -> {
            System.out.println("Bean count: " + ctx.getBeanDefinitionCount());
            Arrays.stream(ctx.getBeanDefinitionNames())
                  .filter(n -> n.toLowerCase().contains("notifier"))
                  .forEach(n -> System.out.println("Notifier bean: " + n));

            System.out.println(welcome.welcome("Sok", false));  // default notifier
            System.out.println(welcome.welcome("Dara", true));  // sms notifier
            reports.demo();                                     // prototype proof
        };
    }
}