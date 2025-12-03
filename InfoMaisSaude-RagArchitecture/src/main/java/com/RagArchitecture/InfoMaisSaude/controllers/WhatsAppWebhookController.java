package com.RagArchitecture.InfoMaisSaude.controllers;

import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    @Value("${evolution.api.url}")
    private String EVOLUTION_URL;

    @Value("${evolution.api.key}")
    private String EVOLUTION_KEY;

    @Value("${evolution.instance.name}")
    private String INSTANCE_NAME;

    @Autowired
    private RAGQueryService ragQueryService;

    @PostMapping
    public ResponseEntity<Void> handleEvolutionWebhook(@RequestBody Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("event");
            if (!"messages.upsert".equals(eventType)) {
                return ResponseEntity.ok().build(); 
            }

            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> key = (Map<String, Object>) data.get("key");
            
            Boolean fromMe = (Boolean) key.get("fromMe");
            if (fromMe != null && fromMe) {
                return ResponseEntity.ok().build();
            }

            String remoteJid = (String) key.get("remoteJid"); 
            
            Map<String, Object> message = (Map<String, Object>) data.get("message");
            String userText = extractText(message);

            if (userText != null && !userText.isEmpty()) {
                System.out.println("Mensagem de " + remoteJid + ": " + userText);

                String respostaIA = ragQueryService.obterRecomendacao(userText);
                System.out.println("Resposta RAG: " + respostaIA);

                enviarRespostaEvolution(remoteJid, respostaIA);
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    private String extractText(Map<String, Object> message) {
        if (message == null) return null;
        
        if (message.containsKey("conversation")) {
            return (String) message.get("conversation");
        }
        if (message.containsKey("extendedTextMessage")) {
            Map<String, Object> extended = (Map<String, Object>) message.get("extendedTextMessage");
            return (String) extended.get("text");
        }
        return null; 
    }

    private void enviarRespostaEvolution(String remoteJid, String texto) {
        String url = EVOLUTION_URL + "/message/sendText/" + INSTANCE_NAME;

        String numeroLimpo = remoteJid.replace("@s.whatsapp.net", "");

        Map<String, Object> body = new HashMap<>();
        body.put("number", numeroLimpo);
        body.put("text", texto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_KEY); 

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Resposta enviada via Evolution para: " + numeroLimpo);
        } catch (Exception e) {
            System.err.println("Erro ao enviar pela Evolution: " + e.getMessage());
        }
    }
}