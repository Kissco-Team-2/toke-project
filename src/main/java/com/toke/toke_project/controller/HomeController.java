package com.toke.toke_project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/join")
    public String joinForm() {
        return "joinForm"; 
    }
    
    
    @GetMapping("/login")
    public String loginForm() {
        return "loginForm"; 
    }
}