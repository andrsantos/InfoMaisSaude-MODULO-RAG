package com.RagArchitecture.InfoMaisSaude.services.impl;

import com.RagArchitecture.InfoMaisSaude.dtos.BotResponseDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.MedicoDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.SlotDisponivelDTO;
import com.RagArchitecture.InfoMaisSaude.enums.TriagemStage;
import com.RagArchitecture.InfoMaisSaude.models.UserSession;
import com.RagArchitecture.InfoMaisSaude.services.AdminIntegrationService;
import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;
import com.RagArchitecture.InfoMaisSaude.services.SessionService;
import com.RagArchitecture.InfoMaisSaude.services.TriagemFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TriagemFlowServiceImpl implements TriagemFlowService {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RAGQueryService ragQueryService;

    @Autowired
    private AdminIntegrationService adminService; 

 

    @Override
    public BotResponseDTO processarMensagem(String telefone, String textoUsuario) {
        UserSession sessao = sessionService.getOrCreateSession(telefone);

        if (textoUsuario.equalsIgnoreCase("reset") || textoUsuario.equalsIgnoreCase("sair")) {
            sessionService.clearSession(telefone);
            return new BotResponseDTO("Sua sessão foi reiniciada. Digite 'Oi' para começar uma nova triagem."); 
        }

        switch (sessao.getEstagio()) {
            case INICIO:
                sessao.setEstagio(TriagemStage.AGUARDANDO_NOME);
                return new BotResponseDTO("Olá! Bem-vindo ao Info + Saúde! 😊\n\nPara começarmos, qual é o seu **Nome Completo**?");

            case AGUARDANDO_NOME:
                sessao.setNome(textoUsuario);
                sessao.setEstagio(TriagemStage.AGUARDANDO_IDADE);
                return new BotResponseDTO("Prazer, " + textoUsuario + "! \nAgora, por favor, me diga sua **Idade** (apenas números).");

            case AGUARDANDO_IDADE:
                if (!textoUsuario.matches("\\d+")) {
                    return new BotResponseDTO("Por favor, digite apenas números para a idade.");
                }
                sessao.setIdade(textoUsuario);
                sessao.setEstagio(TriagemStage.AGUARDANDO_SEXO);
                return new BotResponseDTO("Certo. Qual seu **Sexo Biológico**?", List.of("Masculino", "Feminino"));
           
                case AGUARDANDO_SEXO:
                sessao.setSexo(textoUsuario);
                sessao.setEstagio(TriagemStage.TRIAGEM_IA);
                return new BotResponseDTO("Cadastro concluído! ✅\n\nAgora me conte com detalhes: **O que você está sentindo?**");

            case TRIAGEM_IA:
                sessao.adicionarAoHistorico("Paciente: " + textoUsuario);
                
                if (sessao.getPerguntasFeitas() < 5) {
                    
                    String respostaInvestigativa = ragQueryService.analisarSintomas(
                        sessao.getHistoricoClinico().toString(), 
                        sessao.getIdade(),
                        sessao.getSexo()
                    );

                    if (!respostaInvestigativa.toUpperCase().contains("PRONTO")) {
                        sessao.incrementarPerguntas();
                        sessao.adicionarAoHistorico("Bot: " + respostaInvestigativa);
                        return new BotResponseDTO(respostaInvestigativa);
                    }
                }
                
                
                String perfilCompleto = String.format(
                    "PACIENTE: %s, %s anos, %s.\nHISTÓRICO:\n%s", 
                    sessao.getNome(), sessao.getIdade(), sessao.getSexo(), sessao.getHistoricoClinico()
                );
                String recomendacaoTexto = ragQueryService.obterRecomendacao(perfilCompleto);

                String especialidade = ragQueryService.extrairEspecialidade(recomendacaoTexto);
                sessao.setEspecialidadeDetectada(especialidade);
                System.out.println("Especialidade detectada: " + especialidade);
                List<MedicoDTO> medicos = adminService.buscarMedicos(especialidade);
                sessao.setMedicosEncontrados(medicos);

                if (!medicos.isEmpty()) {
                    sessao.setEstagio(TriagemStage.OFERECER_AGENDAMENTO);
                    String msg = recomendacaoTexto + "\n\n" +
                           "-----------------------------------\n" +
                           "🔎 Identifiquei que um *" + especialidade + "* pode te ajudar.\n" +
                           "Encontrei " + medicos.size() + " especialistas.\n" +
                           "**Gostaria de marcar uma consulta agora?**";
                    return new BotResponseDTO(msg, List.of("Sim","Não"));
                } else {
                    sessionService.clearSession(telefone);
                    return new BotResponseDTO(recomendacaoTexto + "\n\n(No momento não temos médicos dessa especialidade disponíveis para agendamento online. Atendimento finalizado.)");
                }


            case OFERECER_AGENDAMENTO:
               if (textoUsuario.toLowerCase().contains("sim")) {
                    
                    String especialidadeDetectada = sessao.getEspecialidadeDetectada();
                    
                    List<SlotDisponivelDTO> slots = adminService.buscarDisponibilidadeCombo(especialidadeDetectada);
                    
                    if (slots.isEmpty()) {
                        sessionService.clearSession(telefone);
                        return new BotResponseDTO("Poxa, verifiquei aqui e não encontrei horários livres para " + especialidadeDetectada + " nos próximos dias. Tente novamente mais tarde.");
                    }

                    List<BotResponseDTO.ListItemDTO> itensMenu = new ArrayList<>();
                    
                    for (SlotDisponivelDTO slot : slots) {
                        String titulo = formatarDataCurta(slot.data()) + " às " + slot.horario().toString().substring(0, 5);
                        
                        String descricao = slot.nomeMedico();
                        if (slot.diaDaSemana() != null) {
                            descricao += " • " + slot.diaDaSemana();
                        }
                        
                        String idUnico = "AGENDAR_" + slot.medicoId() + "_" + slot.data() + "_" + slot.horario();
                        
                        itensMenu.add(new BotResponseDTO.ListItemDTO(idUnico, titulo, descricao));
                    }
                    
                    sessao.setEstagio(TriagemStage.CONFIRMAR_AGENDAMENTO);
                    
                    return new BotResponseDTO(
                        "Encontrei estes horários disponíveis para você. \nToque no botão abaixo para ver as opções:", 
                        "Ver Horários", 
                        itensMenu       
                    );

                } else {
                    sessionService.clearSession(telefone);
                    return new BotResponseDTO("Tudo bem! Espero que melhore. Se precisar, mande um 'Oi'.");
                }

            // case ESCOLHER_MEDICO:
            //     try {
            //         int index = Integer.parseInt(textoUsuario.trim()) - 1;
            //         if (index >= 0 && index < sessao.getMedicosEncontrados().size()) {
            //             MedicoDTO medico = sessao.getMedicosEncontrados().get(index);
            //             sessao.setMedicoSelecionado(medico);
                        
            //             sessao.setEstagio(TriagemStage.DEFINIR_DATA);
            //             return new BotResponseDTO("Você escolheu: *" + medico.getNome() + "*.\n" +
            //                    "Para qual dia você deseja ver a agenda? (Digite no formato **DD/MM/AAAA**, ex: " + 
            //                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
            //         } else {
            //             return new BotResponseDTO("Número inválido. Tente novamente.");
            //         }
            //     } catch (NumberFormatException e) {
            //         return new BotResponseDTO("Por favor, digite apenas o número da opção.");
            //     }

            // case DEFINIR_DATA:
            //     try {
            //         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            //         LocalDate data = LocalDate.parse(textoUsuario.trim(), formatter);
                    
            //         if (data.isBefore(LocalDate.now())) {
            //             return new BotResponseDTO("Essa data já passou. Por favor, escolha uma data futura (DD/MM/AAAA):");
            //         }

            //         sessao.setDataDesejada(data);

            //         List<String> horarios = adminService.buscarHorarios(sessao.getMedicoSelecionado().getId(), data.toString());

            //         if (horarios.isEmpty()) {
            //             return new BotResponseDTO("O Dr(a). " + sessao.getMedicoSelecionado().getNome() + " não tem horários livres em " + textoUsuario + ".\n" +
            //             "Por favor, digite outra data (DD/MM/AAAA):");
            //         }

            //         sessao.setEstagio(TriagemStage.ESCOLHER_HORARIO);
            //         return new BotResponseDTO("Horários disponíveis para " + textoUsuario + ":\n\n" + 
            //                String.join("  |  ", horarios) + 
            //                "\n\nDigite o horário desejado (ex: 09:30):" );

            //     } catch (DateTimeParseException e) {
            //         return new BotResponseDTO("Data inválida. Certifique-se de usar o formato DD/MM/AAAA (ex: 25/12/2025).");
            //     }

            // case ESCOLHER_HORARIO:
            //     try {
            //         LocalTime horario = LocalTime.parse(textoUsuario.trim());
            //         sessao.setHorarioSelecionado(horario);

            //         String dadosPaciente = sessao.getNome() + ", " + sessao.getIdade() + " anos, " + sessao.getSexo();
            //         String resumo = ragQueryService.gerarResumoClinicoEstruturado(sessao.getHistoricoClinico().toString(), dadosPaciente);
            //         sessao.setResumoClinicoGerado(resumo);

            //         sessao.setEstagio(TriagemStage.CONFIRMAR_AGENDAMENTO);
            //         return new BotResponseDTO("📝 *Confirme seu Agendamento*\n\n" +
            //                "👤 Paciente: " + sessao.getNome() + "\n" +
            //                "👨‍⚕️ Médico: " + sessao.getMedicoSelecionado().getNome() + "\n" +
            //                "📅 Data: " + sessao.getDataDesejada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
            //                "⏰ Horário: " + horario + "\n" +
            //                "🏥 Especialidade: " + sessao.getEspecialidadeDetectada() + "\n\n" +
            //                "Digite **SIM** para confirmar.          ");          

            //     } catch (DateTimeParseException e) {
            //         return new BotResponseDTO("Formato de horário inválido. Digite exatamente como apareceu na lista (ex: 09:30).");
            //     }

            case CONFIRMAR_AGENDAMENTO:
                if (textoUsuario.startsWith("AGENDAR_")) {
                    try {
                        String[] partes = textoUsuario.split("_");
                        Long medicoId = Long.parseLong(partes[1]);
                        String data = partes[2];
                        String hora = partes[3];
                        
                        MedicoDTO medicoFake = new MedicoDTO(medicoId, "Médico Selecionado", "", null, null, null);
                        sessao.setMedicoSelecionado(medicoFake);
                        sessao.setDataDesejada(LocalDate.parse(data));
                        sessao.setHorarioSelecionado(LocalTime.parse(hora));
                        
                        if (sessao.getResumoClinicoGerado() == null) {
                            String resumo = ragQueryService.gerarResumoClinicoEstruturado(sessao.getHistoricoClinico().toString(), sessao.getNome());
                            sessao.setResumoClinicoGerado(resumo);
                        }
                        
                        return new BotResponseDTO(
                            "📝 *Confirmar Agendamento*\n\n" +
                            "📅 Data: " + formatarDataCurta(sessao.getDataDesejada()) + "\n" +
                            "⏰ Horário: " + sessao.getHorarioSelecionado() + "\n" +
                            "🩺 Especialidade: " + sessao.getEspecialidadeDetectada() + "\n\n" +
                            "Posso confirmar?",
                            List.of("Sim, confirmar", "Cancelar") 
                        );
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new BotResponseDTO("Ocorreu um erro ao processar sua escolha. Por favor, tente novamente.");
                    }
                }
                
                if (textoUsuario.toLowerCase().contains("sim") || textoUsuario.toLowerCase().contains("confirmar")) {
                    
                    boolean sucesso = adminService.agendarConsulta(
                        sessao.getMedicoSelecionado().getId(),
                        sessao.getDataDesejada(),
                        sessao.getHorarioSelecionado(),
                        sessao.getNome(),
                        telefone,
                        sessao.getIdade(),
                        sessao.getSexo(),
                        sessao.getResumoClinicoGerado()
                    );
                    
                    sessionService.clearSession(telefone);
                    
                    if (sucesso) {
                        return new BotResponseDTO("✅ *Agendamento Confirmado!* \nO médico já recebeu seu histórico.");
                    } else {
                        return new BotResponseDTO("❌ Ops! Esse horário foi ocupado agora mesmo. Digite 'Oi' para tentar outro.");
                    }
                } 
                
                else {
                    sessionService.clearSession(telefone);
                    return new BotResponseDTO("Agendamento cancelado. Se precisar, mande um 'Oi'.");
                }

            default:
                return new BotResponseDTO("Erro no fluxo. Digite 'reset' para reiniciar.");
        }
    }

    private String formatarDataCurta(Object data) {
        try {
            LocalDate dt;
            if (data instanceof String) dt = LocalDate.parse((String) data);
            else dt = (LocalDate) data;
            
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            return dt.format(fmt);
        } catch (Exception e) {
            return data.toString();
        }
    }
}