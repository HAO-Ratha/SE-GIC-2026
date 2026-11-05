package edu.itc.se.shop.common;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}