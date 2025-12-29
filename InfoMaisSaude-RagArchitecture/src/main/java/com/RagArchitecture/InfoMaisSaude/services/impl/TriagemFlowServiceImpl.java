package com.RagArchitecture.InfoMaisSaude.services.impl;

import com.RagArchitecture.InfoMaisSaude.dtos.integration.MedicoDTO;
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
import java.time.format.DateTimeParseException;
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
    public String processarMensagem(String telefone, String textoUsuario) {
        UserSession sessao = sessionService.getOrCreateSession(telefone);

        if (textoUsuario.equalsIgnoreCase("reset") || textoUsuario.equalsIgnoreCase("sair")) {
            sessionService.clearSession(telefone);
            return "Atendimento encerrado/reiniciado. Se precisar, mande um 'Oi' para começar de novo.";
        }

        switch (sessao.getEstagio()) {
            case INICIO:
                sessao.setEstagio(TriagemStage.AGUARDANDO_NOME);
                return "Olá! Sou o assistente virtual do *Informa + Saúde*. \n\nPara começarmos sua triagem, por favor, digite seu **Nome Completo**.";

            case AGUARDANDO_NOME:
                sessao.setNome(textoUsuario);
                sessao.setEstagio(TriagemStage.AGUARDANDO_IDADE);
                return "Prazer, " + textoUsuario + "! \nAgora, por favor, me diga sua **Idade** (apenas números).";

            case AGUARDANDO_IDADE:
                if (!textoUsuario.matches("\\d+")) {
                    return "Por favor, digite apenas números para a idade.";
                }
                sessao.setIdade(textoUsuario);
                sessao.setEstagio(TriagemStage.AGUARDANDO_SEXO);
                return "Certo. Qual seu **Sexo Biológico**? (Responda Masculino ou Feminino)";

            case AGUARDANDO_SEXO:
                sessao.setSexo(textoUsuario);
                sessao.setEstagio(TriagemStage.TRIAGEM_IA);
                return "Cadastro concluído! ✅\n\nAgora me conte com detalhes: **O que você está sentindo?**";

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
                        return respostaInvestigativa;
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
                    return recomendacaoTexto + "\n\n" +
                           "-----------------------------------\n" +
                           "🔎 Identifiquei que um *" + especialidade + "* pode te ajudar.\n" +
                           "Encontrei " + medicos.size() + " especialistas disponíveis na nossa rede.\n" +
                           "**Gostaria de marcar uma consulta agora?** (Responda Sim ou Não)";
                } else {
                    sessionService.clearSession(telefone);
                    return recomendacaoTexto + "\n\n(No momento não temos médicos dessa especialidade disponíveis para agendamento online. Atendimento finalizado.)";
                }


            case OFERECER_AGENDAMENTO:
                if (textoUsuario.toLowerCase().contains("sim")) {
                    StringBuilder lista = new StringBuilder("Ótimo! Escolha um profissional digitando o número correspondente:\n\n");
                    List<MedicoDTO> listaMedicos = sessao.getMedicosEncontrados();
                    
                    for (int i = 0; i < listaMedicos.size(); i++) {
                        lista.append((i + 1)).append(". ").append(listaMedicos.get(i).getNome()).append("\n");
                    }
                    
                    sessao.setEstagio(TriagemStage.ESCOLHER_MEDICO);
                    return lista.toString();
                } else {
                    sessionService.clearSession(telefone);
                    return "Tudo bem! Espero que melhore. Se precisar, estou por aqui.";
                }

            case ESCOLHER_MEDICO:
                try {
                    int index = Integer.parseInt(textoUsuario.trim()) - 1;
                    if (index >= 0 && index < sessao.getMedicosEncontrados().size()) {
                        MedicoDTO medico = sessao.getMedicosEncontrados().get(index);
                        sessao.setMedicoSelecionado(medico);
                        
                        sessao.setEstagio(TriagemStage.DEFINIR_DATA);
                        return "Você escolheu: *" + medico.getNome() + "*.\n" +
                               "Para qual dia você deseja ver a agenda? (Digite no formato **DD/MM/AAAA**, ex: " + 
                               LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                    } else {
                        return "Número inválido. Tente novamente.";
                    }
                } catch (NumberFormatException e) {
                    return "Por favor, digite apenas o número da opção.";
                }

            case DEFINIR_DATA:
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate data = LocalDate.parse(textoUsuario.trim(), formatter);
                    
                    if (data.isBefore(LocalDate.now())) {
                        return "Essa data já passou. Por favor, escolha uma data futura (DD/MM/AAAA):";
                    }

                    sessao.setDataDesejada(data);

                    List<String> horarios = adminService.buscarHorarios(sessao.getMedicoSelecionado().getId(), data.toString());

                    if (horarios.isEmpty()) {
                        return "O Dr(a). " + sessao.getMedicoSelecionado().getNome() + " não tem horários livres em " + textoUsuario + ".\n" +
                               "Por favor, digite outra data (DD/MM/AAAA):";
                    }

                    sessao.setEstagio(TriagemStage.ESCOLHER_HORARIO);
                    return "Horários disponíveis para " + textoUsuario + ":\n\n" + 
                           String.join("  |  ", horarios) + 
                           "\n\nDigite o horário desejado (ex: 09:30):";

                } catch (DateTimeParseException e) {
                    return "Data inválida. Certifique-se de usar o formato DD/MM/AAAA (ex: 25/12/2025).";
                }

            case ESCOLHER_HORARIO:
                try {
                    LocalTime horario = LocalTime.parse(textoUsuario.trim());
                    sessao.setHorarioSelecionado(horario);

                    String dadosPaciente = sessao.getNome() + ", " + sessao.getIdade() + " anos, " + sessao.getSexo();
                    String resumo = ragQueryService.gerarResumoClinicoEstruturado(sessao.getHistoricoClinico().toString(), dadosPaciente);
                    sessao.setResumoClinicoGerado(resumo);

                    sessao.setEstagio(TriagemStage.CONFIRMAR_AGENDAMENTO);
                    return "📝 *Confirme seu Agendamento*\n\n" +
                           "👤 Paciente: " + sessao.getNome() + "\n" +
                           "👨‍⚕️ Médico: " + sessao.getMedicoSelecionado().getNome() + "\n" +
                           "📅 Data: " + sessao.getDataDesejada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                           "⏰ Horário: " + horario + "\n" +
                           "🏥 Especialidade: " + sessao.getEspecialidadeDetectada() + "\n\n" +
                           "Digite **SIM** para confirmar.";

                } catch (DateTimeParseException e) {
                    return "Formato de horário inválido. Digite exatamente como apareceu na lista (ex: 09:30).";
                }

            case CONFIRMAR_AGENDAMENTO:
                if (textoUsuario.equalsIgnoreCase("sim") || textoUsuario.toLowerCase().contains("confirm")) {
                    
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
                        return "✅ *Agendamento Confirmado com Sucesso!*\n\n" +
                               "O médico já recebeu seu histórico clínico.\n" +
                               "Obrigado por usar o Info + Saúde!";
                    } else {
                        return "❌ Ops! Tivemos um problema.\n" +
                               "Parece que esse horário foi ocupado agora mesmo.\n" +
                               "Por favor, reinicie o atendimento mandando um 'Oi' para escolher outro horário.";
                    }
                } else {
                    return "Agendamento pendente. Digite SIM para confirmar ou RESET para cancelar.";
                }

            default:
                return "Erro no fluxo. Digite 'reset' para reiniciar.";
        }
    }
}