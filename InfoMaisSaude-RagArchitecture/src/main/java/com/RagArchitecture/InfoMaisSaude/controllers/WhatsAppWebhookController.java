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
    public ResponseEntity<Void> handleWebhookNotification(@RequestBody WhatsAppPayload payload) {
        System.out.println("POST /webhook - Payload recebido.");

        try {
            Optional<String> userText = extractUserText(payload);
            Optional<String> userPhone = extractUserPhone(payload);

            if (userText.isPresent() && userPhone.isPresent()) {
                String textoRecebido = userText.get();
                String numeroUsuario = userPhone.get();

                System.out.println("Mensagem original de " + numeroUsuario + ": " + textoRecebido);

                if (numeroUsuario.startsWith("55") && numeroUsuario.length() == 12) {
                    String ddd = numeroUsuario.substring(0, 4); 
                    String numero = numeroUsuario.substring(4); 
                    
                    numeroUsuario = ddd + "9" + numero; 
                    System.out.println("Número corrigido para envio (com 9): " + numeroUsuario);
                }

                String respostaIA = ragQueryService.obterRecomendacao(textoRecebido);
                System.out.println("Resposta gerada pelo RAG: " + respostaIA);

                enviarRespostaWhatsApp(numeroUsuario, respostaIA);
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    private void enviarRespostaWhatsApp(String destinatario, String textoMensagem) {
        String url = "https://graph.facebook.com/v19.0/" + META_PHONE_ID + "/messages";

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
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Mensagem enviada com sucesso! Status: " + response.getStatusCode());
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