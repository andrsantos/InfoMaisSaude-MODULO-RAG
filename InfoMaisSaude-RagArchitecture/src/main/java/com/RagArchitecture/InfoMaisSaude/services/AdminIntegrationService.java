package com.RagArchitecture.InfoMaisSaude.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.LoginRequestDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.LoginResponseDTO;
import java.util.List;

@Service
public class AdminIntegrationService {

    @Value("${admin.api.url}")
    private String BASE_URL;

    @Value("${admin.api.user}")
    private String BOT_USER;

    @Value("${admin.api.password}")
    private String BOT_PASS;

    private String jwtToken;
    private final RestTemplate restTemplate = new RestTemplate();

    private void autenticar() {
        String url = BASE_URL + "/login";
        LoginRequestDTO loginRequest = new LoginRequestDTO(BOT_USER, BOT_PASS);

        try {
            ResponseEntity<LoginResponseDTO> response = restTemplate.postForEntity(url, loginRequest, LoginResponseDTO.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.jwtToken = response.getBody().token();
                System.out.println("✅ Chatbot autenticado no Admin com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao autenticar Chatbot: " + e.getMessage());
            this.jwtToken = null;
        }
    }

    private HttpHeaders criarHeaders() {
        if (this.jwtToken == null) {
            autenticar();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public String testarConexao() {
        if (this.jwtToken == null) {
            autenticar();
            if (this.jwtToken == null) return "Falha na Autenticação (Verifique ngrok e credenciais)";
        }

        String url = BASE_URL + "/api/agendamentos/disponibilidade?medicoId=1&data=2025-12-15"; 

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(criarHeaders());
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, List.class);
            
            return "✅ Sucesso! Conexão estabelecida. Horários encontrados: " + response.getBody();

        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            System.out.println("Token expirado, reautenticando...");
            this.jwtToken = null;
            autenticar();
            return "Token expirou. Tente novamente.";
        } catch (Exception e) {
            return "❌ Erro ao conectar no endpoint de agendamentos: " + e.getMessage();
        }
    }
}