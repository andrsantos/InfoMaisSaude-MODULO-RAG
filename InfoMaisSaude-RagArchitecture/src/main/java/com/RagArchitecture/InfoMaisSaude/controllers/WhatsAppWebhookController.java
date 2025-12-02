package com.RagArchitecture.InfoMaisSaude.controllers;

import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;
import com.RagArchitecture.InfoMaisSaude.dtos.WhatsAppPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    @Value("${meta.webhook.verify-token}")
    private String VERIFY_TOKEN;

    @Value("${meta.api.token}")
    private String META_API_TOKEN;

    @Value("${meta.api.phone-number-id}") 
    private String META_PHONE_ID;

    @Autowired
    private RAGQueryService ragQueryService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        System.out.println("GET /webhook - Verificação recebida.");

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falha na verificação");
        }
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhookNotification(@RequestBody Map<String, Object> rawPayload) {
        try {
            System.out.println("--- DEBUG PAYLOAD RECEBIDO ---");
            System.out.println(rawPayload.toString()); 
            
            if (rawPayload.toString().contains("statuses")) {
                System.out.println("⚠️ ALERTA: Recebemos um status de entrega (possível erro)!");
                return ResponseEntity.ok().build(); 
            }
            processarMensagem(rawPayload);

        } catch (Exception e) {
            System.err.println("Erro geral no webhook: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    private void processarMensagem(Map<String, Object> payload) {
        try {
            var entry = ((java.util.List<Map<String, Object>>) payload.get("entry")).get(0);
            var changes = ((java.util.List<Map<String, Object>>) entry.get("changes")).get(0);
            var value = (Map<String, Object>) changes.get("value");
            
            if (!value.containsKey("messages")) return; // Não é mensagem

            var messages = (java.util.List<Map<String, Object>>) value.get("messages");
            var contacts = (java.util.List<Map<String, Object>>) value.get("contacts");
            
            String texto = (String) ((Map<String, Object>) messages.get(0).get("text")).get("body");
            
            String waId = (String) contacts.get(0).get("wa_id");

            System.out.println("Mensagem: " + texto);
            System.out.println("ID Oficial para Resposta (wa_id): " + waId);

            String respostaIA = ragQueryService.obterRecomendacao(texto);
            enviarRespostaWhatsApp(waId, respostaIA); 

        } catch (Exception e) {
            System.out.println("Não foi possível processar como mensagem de texto simples.");
        }
    }

   private void enviarRespostaWhatsApp(String destinatario, String textoMensagem) {
        String url = "https://graph.facebook.com/v19.0/" + META_PHONE_ID + "/messages";

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", destinatario);
        
        body.put("type", "template");
        
        Map<String, Object> templateObj = new HashMap<>();
        templateObj.put("name", "hello_world");
        
        Map<String, String> languageObj = new HashMap<>();
        languageObj.put("code", "en_US"); 
        
        templateObj.put("language", languageObj);
        body.put("template", templateObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(META_API_TOKEN);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Template enviado! Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("FALHA ao enviar mensagem no WhatsApp: " + e.getMessage());
        }
    }

    private Optional<String> extractUserText(WhatsAppPayload payload) {
        try {
            return Optional.of(payload.entry()[0].changes()[0].value().messages()[0].text().body());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractUserPhone(WhatsAppPayload payload) {
        try {
            return Optional.of(payload.entry()[0].changes()[0].value().messages()[0].from());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}