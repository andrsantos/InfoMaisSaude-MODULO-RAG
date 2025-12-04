package com.RagArchitecture.InfoMaisSaude.controllers;

import com.RagArchitecture.InfoMaisSaude.dtos.WhatsAppPayload;
import com.RagArchitecture.InfoMaisSaude.services.TriagemFlowService; 
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

    @Value("${meta.phone.id}")
    private String META_PHONE_ID;

    @Autowired
    private TriagemFlowService triagemFlowService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        System.out.println("Tentativa de verificação do Webhook...");

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            System.out.println("Webhook verificado com sucesso!");
            return ResponseEntity.ok(challenge);
        } else {
            System.err.println("Falha na verificação: Token inválido.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falha na verificação");
        }
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhookNotification(@RequestBody WhatsAppPayload payload) {
        try {
            Optional<String> userText = extractUserText(payload);
            Optional<String> userPhone = extractUserPhone(payload);

            if (userText.isPresent() && userPhone.isPresent()) {
                String texto = userText.get().trim();
                String numero = userPhone.get();
                
                System.out.println("Mensagem de " + numero + ": " + texto);

                String respostaParaEnviar = triagemFlowService.processarMensagem(numero, texto);
                
                enviarRespostaWhatsApp(numero, respostaParaEnviar);
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok().build();
    }

    private void enviarRespostaWhatsApp(String destinatario, String textoMensagem) {
        String url = "https://graph.facebook.com/v21.0/" + META_PHONE_ID + "/messages";

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", destinatario);
        
        Map<String, String> textObj = new HashMap<>();
        textObj.put("body", textoMensagem);
        body.put("text", textObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(META_API_TOKEN); 

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Resposta enviada com sucesso para: " + destinatario);
        } catch (Exception e) {
            System.err.println("FALHA ao enviar mensagem na Meta: " + e.getMessage());
        }
    }

    private Optional<String> extractUserText(WhatsAppPayload payload) {
        try {
            return Optional.of(payload.entry()[0].changes()[0].value().messages()[0].text().body());
        } catch (Exception e) { return Optional.empty(); }
    }

    private Optional<String> extractUserPhone(WhatsAppPayload payload) {
        try {
            return Optional.of(payload.entry()[0].changes()[0].value().messages()[0].from());
        } catch (Exception e) { return Optional.empty(); }
    }
}