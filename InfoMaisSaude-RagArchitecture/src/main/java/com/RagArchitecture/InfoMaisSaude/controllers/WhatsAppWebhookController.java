
package com.RagArchitecture.InfoMaisSaude.controllers;
import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.RagArchitecture.InfoMaisSaude.dtos.*; 
import java.util.Optional;

@RestController
@RequestMapping("/webhook") 
public class WhatsAppWebhookController {

    @Value("${meta.webhook.verify-token}") 
    private String VERIFY_TOKEN;

    @Autowired
    private RAGQueryService ragQueryService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        System.out.println("GET /webhook - Verificação recebida.");

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            System.out.println("Verificação bem-sucedida. Respondendo com challenge.");
            return ResponseEntity.ok(challenge);
        } else {
            System.out.println("Falha na verificação. Tokens não batem.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falha na verificação");
        }
    }


    @PostMapping
    public ResponseEntity<Void> handleWebhookNotification(@RequestBody WhatsAppPayload payload) {

        System.out.println("POST /webhook - Mensagem recebida: " + payload);

        try {
            Optional<String> userText = extractUserText(payload);
            Optional<String> userPhone = extractUserPhone(payload);

            if (userText.isPresent() && userPhone.isPresent()) {
                String texto = userText.get();
                String numero = userPhone.get();
                System.out.println("Mensagem de " + numero + ": " + texto);
                String recomendacao = ragQueryService.obterRecomendacao(texto);
                System.out.println("Resposta RAG: " + recomendacao);
                System.out.println("TODO: Enviar esta resposta para " + numero + " via Graph API");

            }
        } catch (Exception e) {
            System.out.println("Erro ao processar webhook: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
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