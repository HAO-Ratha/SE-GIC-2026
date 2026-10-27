package edu.itc.se.shop.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("dev")
public class ConsoleNotifier implements Notifier {
    @Override
    public void send(String to, String text) {
        System.out.println("CONSOLE to=" + to + " text=" + text);
    }
}