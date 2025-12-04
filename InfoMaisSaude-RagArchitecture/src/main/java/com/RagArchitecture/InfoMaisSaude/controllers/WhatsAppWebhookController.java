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
            System.out.println("--- PAYLOAD RECEBIDO ---");

            String eventType = (String) payload.get("event");
            if (!"messages.upsert".equals(eventType)) return ResponseEntity.ok().build();

            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> key = (Map<String, Object>) data.get("key");
            
            if (Boolean.TRUE.equals(key.get("fromMe"))) return ResponseEntity.ok().build();

            String remoteJid = (String) key.get("remoteJid");
            String pushName = (String) data.get("pushName");
            
            Map<String, Object> message = (Map<String, Object>) data.get("message");
            String userText = extractText(message);

            if (userText != null && !userText.isEmpty()) {
                System.out.println("Mensagem de " + pushName + " (" + remoteJid + "): " + userText);

                String respostaIA = ragQueryService.obterRecomendacao(userText);
                System.out.println("RAG Respondeu. Enviando resposta citada...");

                enviarRespostaCitada(remoteJid, respostaIA, data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }

    private void enviarRespostaCitada(String remoteJid, String texto, Map<String, Object> messageData) {
        String url = EVOLUTION_URL + "/message/sendText/" + INSTANCE_NAME;

        Map<String, Object> body = new HashMap<>();
        body.put("number", remoteJid); 
        body.put("text", texto);
        
 
        body.put("quoted", messageData);
        // -------------------------------

        Map<String, Object> options = new HashMap<>();
        options.put("presence", "composing"); 
        options.put("linkPreview", false);
        body.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", EVOLUTION_KEY);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Resposta enviada com sucesso para: " + remoteJid);
        } catch (Exception e) {
            System.err.println("Erro ao enviar: " + e.getMessage());
        }
    }

    private String extractText(Map<String, Object> message) {
        if (message == null) return null;
        if (message.containsKey("conversation")) return (String) message.get("conversation");
        if (message.containsKey("extendedTextMessage")) {
            Map<String, Object> ext = (Map<String, Object>) message.get("extendedTextMessage");
            return (String) ext.get("text");
        }
        return null;
    }
}