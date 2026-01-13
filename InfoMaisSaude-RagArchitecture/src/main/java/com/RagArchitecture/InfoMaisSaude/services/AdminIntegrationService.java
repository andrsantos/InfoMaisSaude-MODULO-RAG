package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.dtos.integration.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AdminIntegrationService {

    @Value("${admin.api.url}")
    private String BASE_URL;

    @Autowired
    private BotAuthenticationService botAuthService; 

    private final RestTemplate restTemplate = new RestTemplate();


    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(botAuthService.getTokenValido());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    public List<MedicoDTO> buscarMedicos(String especialidade) {
        String url = BASE_URL + "/api/medicos/por-especialidade?especialidade=" + especialidade;
        return executarComRetry(() -> {
            ResponseEntity<List<MedicoDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<MedicoDTO>>() {}
            );
            return response.getBody();
        }, Collections.emptyList());
    }

    public List<ClinicaDTO> buscarClinicas() {
        String url = BASE_URL + "/api/clinicas/listar-resumo";
        return executarComRetry(() -> {
            ResponseEntity<List<ClinicaDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<ClinicaDTO>>() {}
            );
            return response.getBody();
        }, Collections.emptyList());
    }

    public List<String> buscarEspecialidadesClinica(Long clinicaId) {
        String url = BASE_URL + "/api/clinicas/listar-especialidades/" + clinicaId;
        return executarComRetry(() -> {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        }, Collections.emptyList());
    }

    public List<SlotDisponivelDTO> buscarDisponibilidadeCombo(String especialidade, Long clinicaId) {
        String url = BASE_URL + "/api/agendamentos/disponibilidade-combo?especialidade=" + especialidade + "&clinicaId=" + clinicaId;
        return executarComRetry(() -> {
            ResponseEntity<List<SlotDisponivelDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<SlotDisponivelDTO>>() {}
            );
            return response.getBody();
        }, Collections.emptyList());
    }

    public List<ConsultaAgendadaDTO> buscarConsultasAtivas(String telefone, Long clinicaId) {
        String url = BASE_URL + "/api/agendamentos/paciente/" + telefone + "/clinica/" + clinicaId + "/ativas";
        return executarComRetry(() -> {
            ResponseEntity<List<ConsultaAgendadaDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(criarHeaders()),
                new ParameterizedTypeReference<List<ConsultaAgendadaDTO>>() {}
            );
            return response.getBody();
        }, new ArrayList<>());
    }

    public boolean agendarConsulta(Long clinicaId, Long medicoId, LocalDate data, LocalTime horario, 
                                   String nome, String telefone, String idade, String sexo, String cpf, String resumo) {
        
        String url = BASE_URL + "/api/agendamentos/agendar";
        AgendamentoPayload payload = new AgendamentoPayload(clinicaId, medicoId, data, horario, nome, telefone, idade, sexo, cpf, resumo);

        return executarComRetryBooleano(() -> {
            try {
                ResponseEntity<Void> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, criarHeaders()), Void.class);
                return response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK;
            } catch (HttpClientErrorException.Conflict e) {
                System.err.println("Conflito: Horário já ocupado.");
                return false; 
            }
        });
    }

    public boolean cancelarConsulta(Long consultaId, String telefonePaciente) {
        String url = BASE_URL + "/api/agendamentos/" + consultaId + "/cancelar-paciente?telefone=" + telefonePaciente;
        
        String motivoAutomatico = "Solicitado pelo paciente via WhatsApp";
        var payload = new CancelamentoRequestDTO(motivoAutomatico);

        return executarComRetryBooleano(() -> {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, criarHeaders()), Void.class);
            return true;
        });
    }


    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }

    @FunctionalInterface
    private interface BooleanApiCall {
        boolean execute();
    }

    private <T> T executarComRetry(ApiCall<T> call, T valorErro) {
        try {
            return call.execute(); 
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            System.out.println("⚠️ Token expirado. Renovando e tentando novamente...");
            botAuthService.renovarToken(); 
            try {
                return call.execute(); 
            } catch (Exception ex) {
                System.err.println("❌ Erro final após retry: " + ex.getMessage());
                return valorErro;
            }
        } catch (Exception e) {
            System.err.println("❌ Erro na requisição: " + e.getMessage());
            return valorErro;
        }
    }

    private boolean executarComRetryBooleano(BooleanApiCall call) {
        try {
            return call.execute();
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            System.out.println("⚠️ Token expirado. Renovando e tentando novamente...");
            botAuthService.renovarToken(); 
            try {
                return call.execute(); 
            } catch (Exception ex) {
                System.err.println("❌ Erro final após retry: " + ex.getMessage());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Erro na requisição: " + e.getMessage());
            return false;
        }
    }

    private record AgendamentoPayload(
        Long clinicaId, Long medicoId, LocalDate data, LocalTime horario,
        String nomePaciente, String telefonePaciente, String idade,
        String sexo, String cpf, String resumoClinico
    ) {}
    
    public String testarConexao() {
         try {
             buscarClinicas(); 
             return "✅ Conexão OK!";
         } catch (Exception e) {
             return "❌ Falha: " + e.getMessage();
         }
    }
}