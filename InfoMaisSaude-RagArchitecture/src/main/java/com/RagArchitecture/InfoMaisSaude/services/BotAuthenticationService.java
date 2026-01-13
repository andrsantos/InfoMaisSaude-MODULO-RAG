package com.RagArchitecture.InfoMaisSaude.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BotAuthenticationService {
    
    @Value("${bot.username}") 
    private String botUsername;

    @Value("${bot.password}") 
    private String botPassword;

    @Value("${api.url.login}") 
    private String loginUrl;

    private String tokenAtual;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getTokenValido() {
        if (tokenAtual == null || tokenAtual.isEmpty()) {
            renovarToken();
        }
        return tokenAtual;
    }

    public void renovarToken() {

        try {
            System.out.println("🔄 Bot: Renovando token de acesso...");
            
            Map<String, String> credenciais = new HashMap<>();
            credenciais.put("login", botUsername);
            credenciais.put("senha", botPassword);

            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, credenciais, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("token")) {
                this.tokenAtual = (String) response.getBody().get("token");
                System.out.println("✅ Bot: Novo token gerado com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("❌ Erro fatal: Bot não conseguiu se autenticar: " + e.getMessage());
        }
    }
    
}