package edu.itc.shop.service;

import org.springframework.stereotype.Service;

@Service
public class EmailNotification implements Notification {
    @Override
    public void send(String msg) {
        System.out.println("EmailNotification sending: " + msg);
    }
}