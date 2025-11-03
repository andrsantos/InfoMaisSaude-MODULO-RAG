package com.RagArchitecture.InfoMaisSaude.controllers; 

import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medico")
public class SintomasController {

    @Autowired
    private RAGQueryService ragQueryService;


    public record SintomasRequest(String sintomas) {}

 
    @PostMapping("/recomendar")
    public String recomendar(@RequestBody SintomasRequest request) {
        
        if (request.sintomas() == null || request.sintomas().isBlank()) {
            return "Por favor, forneça uma descrição dos seus sintomas.";
        }

        return ragQueryService.obterRecomendacao(request.sintomas());
    }
}