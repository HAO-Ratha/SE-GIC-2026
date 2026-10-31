package edu.itc.shop.service;

import org.springframework.stereotype.Service;

@Service
public class SmsNotification implements Notification {
    @Override
    public void send(String msg) {
        System.out.println("SmsNotification sending: " + msg);
    }
}