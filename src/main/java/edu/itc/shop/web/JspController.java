package edu.itc.shop.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class JspController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("profile", "default");
        model.addAttribute("message", "Hello from JSP!");
        return "index"; 
    }
}