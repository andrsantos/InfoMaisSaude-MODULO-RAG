package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.dtos.integration.ClinicaDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.LoginRequestDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.LoginResponseDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.MedicoDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.SlotDisponivelDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
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


    public List<MedicoDTO> buscarMedicos(String especialidade) {
        System.out.println("Buscando médicos para a especialidade: " + especialidade);
        String url = BASE_URL + "/api/medicos/por-especialidade?especialidade=" + especialidade;

        try {
            ResponseEntity<List<MedicoDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<MedicoDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar médicos: " + e.getMessage());
            if (e instanceof HttpClientErrorException.Forbidden) {
                this.jwtToken = null; 
            }
            return Collections.emptyList();
        }
    }

    public List<ClinicaDTO> buscarClinicas() {
        System.out.println("Buscando clínicas");
        String url = BASE_URL + "/api/clinicas/listar-resumo";

        try {
            ResponseEntity<List<ClinicaDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<ClinicaDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar clinicas: " + e.getMessage());
            if (e instanceof HttpClientErrorException.Forbidden) {
                this.jwtToken = null; 
            }
            return Collections.emptyList();
        }
    }

    public List<String> buscarEspecialidadesClinica(Long clinicaId){

        System.out.println("Buscando especialidades da clínica");
        String url = BASE_URL + "/api/clinicas/listar-especialidades/" + clinicaId;

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar especialidades da clinica: " + e.getMessage());
            if (e instanceof HttpClientErrorException.Forbidden) {
                this.jwtToken = null; 
            }
            return Collections.emptyList();
        }

    }


    public List<String> buscarHorarios(Long medicoId, String data) {
        String url = BASE_URL + "/api/agendamentos/disponibilidade?medicoId=" + medicoId + "&data=" + data;

        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar horários: " + e.getMessage());
            return Collections.emptyList();
        }
    }

public List<SlotDisponivelDTO> buscarDisponibilidadeCombo(String especialidade, Long clinicaId) {
        
        String url = BASE_URL + "/api/agendamentos/disponibilidade-combo?especialidade=" + especialidade + "&clinicaId=" + clinicaId;
        
        try {
            ResponseEntity<List<SlotDisponivelDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<SlotDisponivelDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar horários: " + e.getMessage());
            return Collections.emptyList();
        }
    }


    public boolean agendarConsulta(Long clinicaId, Long medicoId, LocalDate data, LocalTime horario, 
                                   String nome, String telefone, String idade, String sexo, String cpf, String resumo) {
        
        String url = BASE_URL + "/api/agendamentos/agendar";

        AgendamentoPayload payload = new AgendamentoPayload(
            medicoId, data, horario, nome, telefone, idade, sexo, resumo
        );

        try {
            HttpEntity<AgendamentoPayload> request = new HttpEntity<>(payload, criarHeaders());
            
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
            
            return response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException.Conflict e) {
            System.err.println("Conflito: Horário já ocupado.");
            return false;
        } catch (Exception e) {
            System.err.println("Erro crítico ao agendar: " + e.getMessage());
            return false;
        }
    }


    public String testarConexao() {
        if (this.jwtToken == null) autenticar();
        
        String url = BASE_URL + "/api/agendamentos/disponibilidade?medicoId=1&data=2025-12-20"; 

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(criarHeaders()), List.class);
            return "✅ Sucesso! Conexão estabelecida. Resposta: " + response.getBody();
        } catch (Exception e) {
            return "❌ Erro: " + e.getMessage();
        }
    }



    

    private record AgendamentoPayload(
        Long medicoId,
        LocalDate data,
        LocalTime horario,
        String nomePaciente,
        String telefonePaciente,
        String idade,
        String sexo,
        String resumoClinico
    ) {}
}