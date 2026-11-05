package edu.itc.se.shop.web;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController // response controller
public class HelloController {

    public record Greeting(String message, Instant at) {}

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "World") String name) {
     
        return new Greeting("Hello, " + name + "!", Instant.now());
    }
}
