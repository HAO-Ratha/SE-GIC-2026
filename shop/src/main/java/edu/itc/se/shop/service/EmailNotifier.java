package edu.itc.se.shop.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("!dev")   // added after you observe the two-@Primary failure (step 5)
public class EmailNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public void send(String to, String text) {
        log.info("EMAIL to={} text={}", to, text);
    }

    @PostConstruct
    void start() { log.info("EmailNotifier started"); }

    @PreDestroy
    void stop() { log.info("EmailNotifier stopping"); }
}