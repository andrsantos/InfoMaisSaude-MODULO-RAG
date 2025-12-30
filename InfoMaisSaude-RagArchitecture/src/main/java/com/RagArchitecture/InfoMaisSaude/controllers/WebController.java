package com.RagArchitecture.InfoMaisSaude.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/politica-de-privacidade")
    public String politica() {
        return "politica-de-privacidade.html"; 
    }
}