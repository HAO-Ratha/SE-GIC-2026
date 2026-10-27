package edu.itc.shop.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {
    private final Notification emailNotification;
    private final Notification smsNotification;

    // Using @Qualifier to tell Spring WHICH bean to inject
    public WelcomeService(@Qualifier("emailNotification") Notification emailNotification,
                          @Qualifier("smsNotification") Notification smsNotification) {
        this.emailNotification = emailNotification;
        this.smsNotification = smsNotification;
        System.out.println("WelcomeService constructor called");
    }

    @PostConstruct
    public void init() {
        System.out.println("WelcomeService PostConstruct called");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("WelcomeService PreDestroy called");
    }

    public void processMessage(String msg, boolean useSms) {
        if (useSms) {
            smsNotification.send(msg);
        } else {
            emailNotification.send(msg);
        }
    }
}