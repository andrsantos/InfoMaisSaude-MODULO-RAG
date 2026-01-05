package com.RagArchitecture.InfoMaisSaude.controllers;

import com.RagArchitecture.InfoMaisSaude.dtos.BotResponseDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.WhatsAppPayloadDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.NotificacaoPosConsultaDTO;
import com.RagArchitecture.InfoMaisSaude.services.AdminIntegrationService;
import com.RagArchitecture.InfoMaisSaude.services.TriagemFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    @Autowired
    private AdminIntegrationService adminIntegrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/testar-integracao")
    public ResponseEntity<String> testarIntegracaoAdmin() {
        String resultado = adminIntegrationService.testarConexao();
        return ResponseEntity.ok(resultado);
    }

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
    public ResponseEntity<Void> handleWebhookNotification(@RequestBody WhatsAppPayloadDTO payload) {
        try {
            Optional<String> userText = extractUserText(payload);
            Optional<String> userPhone = extractUserPhone(payload);

            if (userText.isPresent() && userPhone.isPresent()) {
                String texto = userText.get().trim();
                String numero = userPhone.get();
                
                System.out.println("Mensagem de " + numero + ": " + texto);

                BotResponseDTO resposta = triagemFlowService.processarMensagem(numero, texto);

                if(resposta.temLista()){
                    enviarListaWhatsApp(numero, resposta.getTexto(), resposta.getTextoBotaoLista(), resposta.getItensLista());
                } else if(resposta.temBotoes()){
                    enviarBotoesWhatsApp(numero, resposta.getTexto(), resposta.getBotoes());
                } else {
                    enviarRespostaWhatsApp(numero, resposta.getTexto());
                }
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

     private void enviarBotoesWhatsApp(String destinatario, String textoMensagem, java.util.List<String> opcoes) {
        String url = "https://graph.facebook.com/v21.0/" + META_PHONE_ID + "/messages";

        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("messaging_product", "whatsapp");
            rootNode.put("recipient_type", "individual");
            rootNode.put("to", destinatario);
            rootNode.put("type", "interactive");

            ObjectNode interactiveNode = objectMapper.createObjectNode();
            interactiveNode.put("type", "button");

            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("text", textoMensagem);
            interactiveNode.set("body", bodyNode);

            ObjectNode actionNode = objectMapper.createObjectNode();
            ArrayNode buttonsArray = objectMapper.createArrayNode();

            for (int i = 0; i < opcoes.size(); i++) {
                String opcao = opcoes.get(i);
                
                ObjectNode buttonNode = objectMapper.createObjectNode();
                buttonNode.put("type", "reply");
                
                ObjectNode replyNode = objectMapper.createObjectNode();
                String idUnico = "btn_" + i + "_" + opcao.toLowerCase().replaceAll("\\s+", "_");
                
                replyNode.put("id", idUnico); 
                replyNode.put("title", opcao); 
                
                buttonNode.set("reply", replyNode);
                buttonsArray.add(buttonNode);
            }

            actionNode.set("buttons", buttonsArray);
            interactiveNode.set("action", actionNode);
            rootNode.set("interactive", interactiveNode);

            String jsonBody = objectMapper.writeValueAsString(rootNode);
            
            System.out.println("--- ENVIANDO JSON BOTOES ---");
            System.out.println(jsonBody);
            System.out.println("----------------------------");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(META_API_TOKEN);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Status Envio Botões: " + response.getStatusCode());

        } catch (Exception e) {
            System.err.println("❌ ERRO CRÍTICO AO ENVIAR BOTÕES: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enviarListaWhatsApp(String destinatario, String textoMensagem, String textoBotao, java.util.List<com.RagArchitecture.InfoMaisSaude.dtos.BotResponseDTO.ListItemDTO> itens) {
        String url = "https://graph.facebook.com/v21.0/" + META_PHONE_ID + "/messages";

        try {
            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
            root.put("messaging_product", "whatsapp");
            root.put("to", destinatario);
            root.put("type", "interactive");

            com.fasterxml.jackson.databind.node.ObjectNode interactive = objectMapper.createObjectNode();
            interactive.put("type", "list");

            com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.createObjectNode();
            body.put("text", textoMensagem);
            interactive.set("body", body);

            com.fasterxml.jackson.databind.node.ObjectNode action = objectMapper.createObjectNode();
            action.put("button", textoBotao); 

            com.fasterxml.jackson.databind.node.ArrayNode sections = objectMapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ObjectNode section = objectMapper.createObjectNode();
            section.put("title", "Disponibilidade"); 

            com.fasterxml.jackson.databind.node.ArrayNode rows = objectMapper.createArrayNode();
            
            int limite = Math.min(itens.size(), 10);
            
            for (int i = 0; i < limite; i++) {
                var item = itens.get(i);
                com.fasterxml.jackson.databind.node.ObjectNode row = objectMapper.createObjectNode();
                row.put("id", item.id()); 
                
                String tituloLimpo = item.titulo().length() > 24 ? item.titulo().substring(0, 23) + "…" : item.titulo();
                row.put("title", tituloLimpo);

                if (item.descricao() != null) {
                    String descLimpa = item.descricao().length() > 72 ? item.descricao().substring(0, 71) + "…" : item.descricao();
                    row.put("description", descLimpa);
                }
                rows.add(row);
            }

            section.set("rows", rows);
            sections.add(section);
            action.set("sections", sections);
            interactive.set("action", action);
            root.set("interactive", interactive);

            String jsonBody = objectMapper.writeValueAsString(root);
            System.out.println("JSON LISTA: " + jsonBody); 

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(META_API_TOKEN);

            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            new org.springframework.web.client.RestTemplate().postForEntity(url, request, String.class);

        } catch (Exception e) {
            System.err.println("Erro ao enviar lista: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private Optional<String> extractUserText(WhatsAppPayloadDTO payload) {
    try {
        
        if (payload == null || payload.entry() == null || payload.entry().length == 0) return Optional.empty();
        var changes = payload.entry()[0].changes();
        if (changes == null || changes.length == 0) return Optional.empty();
        var value = changes[0].value();
        if (value == null || value.messages() == null || value.messages().length == 0) return Optional.empty();

        var message = value.messages()[0];
        String type = message.type();

        if ("text".equals(type) && message.text() != null) {
            return Optional.of(message.text().body());
        }

        if ("interactive".equals(type) && message.interactive() != null) {
            var interactive = message.interactive();
            
            if (interactive.button_reply() != null) {
                return Optional.of(interactive.button_reply().title());
            }

            if (interactive.list_reply() != null) {
                return Optional.of(interactive.list_reply().id());
            }
        }

        return Optional.empty();
        
    } catch (Exception e) {
        System.err.println("Erro ao extrair texto: " + e.getMessage());
        e.printStackTrace(); 
        return Optional.empty();
    }
}

    private Optional<String> extractUserPhone(WhatsAppPayloadDTO payload) {
        try {
            return Optional.of(payload.entry()[0].changes()[0].value().messages()[0].from());
        } catch (Exception e) { return Optional.empty(); }
    }

    @PostMapping("/notificar-encerramento")
    public ResponseEntity<Void> notificarEncerramento(@RequestBody NotificacaoPosConsultaDTO dto) {
        try {
            System.out.println("Recebendo solicitação de notificação para: " + dto.nomePaciente());

            String mensagemTexto = String.format(
                "Olá, *%s*! 👋\n\n" +
                "Sua consulta com *%s* foi finalizada.\n\n" +
                "📝 *Recomendações e Prescrições:*\n" +
                "%s\n\n" +
                "Se tiver dúvidas, entre em contato.\n" +
                "_Info + Saúde - Cuidando de você._",
                dto.nomePaciente(),
                dto.nomeMedico(),
                dto.prescricao()
            );

            String telefoneDestino = dto.telefone(); 
            enviarRespostaWhatsApp(telefoneDestino, mensagemTexto);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Erro ao notificar paciente: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

   

}