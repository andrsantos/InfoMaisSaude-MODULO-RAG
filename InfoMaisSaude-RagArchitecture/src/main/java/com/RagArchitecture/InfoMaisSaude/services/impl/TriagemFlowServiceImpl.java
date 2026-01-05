package com.RagArchitecture.InfoMaisSaude.services.impl;

import com.RagArchitecture.InfoMaisSaude.dtos.BotResponseDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.ClinicaDTO;
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

                sessao.setEstagio(TriagemStage.AGUARDANDO_TERMOS);
                
                String mensagemPrivacidade = 
                    "Olá! Bem-vindo ao *Info + Saúde* 🏥\n\n" +
                    "🔐 Antes de prosseguirmos, precisamos do seu consentimento para tratar seus dados (Nome, Idade, Sintomas) com total segurança.\n\n" +
                    "Você pode ler nossa Política de Privacidade aqui:\n" +
                    "🔗 https://infomaissaude.com.br/politica-de-privacidade\n\n" +
                    "Ao continuar, você concorda com nossos termos.";
                
                return new BotResponseDTO(mensagemPrivacidade, List.of("Concordo e Continuar"));

            case AGUARDANDO_TERMOS:

                if (textoUsuario.toLowerCase().contains("concordo") || textoUsuario.toLowerCase().contains("continuar")) {

                    List<ClinicaDTO> clinicas = adminService.buscarClinicas();

                    if(clinicas.isEmpty()){
                        return new BotResponseDTO("Desculpe, não encontrei clínicas no nosso sistema");
                    }

                    List<BotResponseDTO.ListItemDTO> listaClinicas = new ArrayList<>();

                    for(ClinicaDTO c: clinicas){
                        listaClinicas.add(new BotResponseDTO.ListItemDTO(
                            "CLINICA_" + c.id(),
                            c.nome(),
                            "Toque para selecionar"
                        ));
                    }

                    sessao.setEstagio(TriagemStage.ESCOLHER_CLINICA);

                    return new BotResponseDTO("Perfeito! Para começar, por favor **selecione a clínica** onde deseja ser atendido:",
                    "Ver Clínicas",
                    listaClinicas);

                } else {
                    return new BotResponseDTO(
                        "Para continuarmos seu atendimento, preciso que você concorde com nossa política de dados.",
                        List.of("Concordo e Continuar")
                    );
                }

            case ESCOLHER_CLINICA:

                 Long clinicaId = null;

                 if(textoUsuario.startsWith("CLINICA_")){

                    try{
                        clinicaId = Long.parseLong(textoUsuario.split("_")[1]);
                    }
                    catch(Exception e){
                        System.out.println("Falha ao ler ID da clínica");
                    }

                 }  else {

                    return new BotResponseDTO("Por favor, selecione uma das opções da lista clicando no botão 'Ver Clínicas' ");
                 
                }

                if (clinicaId != null) {

                sessao.setClinicaIdSelecionada(clinicaId);
                sessao.setEstagio(TriagemStage.ESCOLHER_ACAO);
            
                return new BotResponseDTO(
                "Clínica selecionada com sucesso! 🏥\n\nO que você deseja fazer agora?",
                List.of("Marcar Consulta", "Cancelar Consulta")
                );

                } else {
                return new BotResponseDTO("Não entendi qual clínica você escolheu. Por favor, tente novamente pela lista.");
            }

            case ESCOLHER_ACAO:

                if (textoUsuario.toLowerCase().contains("marcar")) {

                    sessao.setEstagio(TriagemStage.AGUARDANDO_NOME);
                    return new BotResponseDTO("Ótimo! Vamos agendar.\n\nPara fazer seu cadastro, digite seu **Nome Completo**.");
                
                } 
                else if (textoUsuario.toLowerCase().contains("cancelar")) {

                    sessionService.clearSession(telefone);
                    return new BotResponseDTO("A funcionalidade de cancelamento estará disponível em breve.\nSessão encerrada.");
                
                } 
                else {

                    return new BotResponseDTO(
                        "Por favor, escolha uma das opções:",
                        List.of("Marcar Consulta", "Cancelar Consulta")
                    );

                }

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
                sessao.setEstagio(TriagemStage.AGUARDANDO_CPF);
                
                return new BotResponseDTO(
                    "Entendido.\n\n" +
                    "Para finalizarmos seu cadastro no sistema da clínica, digite seu **CPF** (apenas números):"
                );

            case AGUARDANDO_CPF:

                String cpfLimpo = textoUsuario.replaceAll("\\D", "");

                if (cpfLimpo.length() != 11) {
                    return new BotResponseDTO("O CPF deve conter 11 dígitos. Por favor, tente novamente (apenas números).");
                }

                sessao.setCpf(cpfLimpo);

                
                List<String> especialidades = adminService.buscarEspecialidadesClinica(sessao.getClinicaIdSelecionada());
                
                List<BotResponseDTO.ListItemDTO> listaOpcoes = new ArrayList<>();

                listaOpcoes.add(new BotResponseDTO.ListItemDTO(
                    "OPCAO_TRIAGEM", 
                    "🤖 Triagem Médica (IA)", 
                    "Não sei qual escolher / Descrever sintomas"
                ));

                for (String esp : especialidades) {
                    listaOpcoes.add(new BotResponseDTO.ListItemDTO(
                        "ESPECIALIDADE_" + esp, 
                        esp, 
                        "Agendar direto com " + esp
                    ));
                }

                sessao.setEstagio(TriagemStage.ESCOLHER_ESPECIALIDADE);

                return new BotResponseDTO(
                    "Cadastro realizado! ✅\n\n" +
                    "Agora, selecione a **Especialidade** que você procura, ou escolha a **Triagem Médica** para que nossa IA te ajude:",
                    "Ver Especialidades",
                    listaOpcoes
                );

            case ESCOLHER_ESPECIALIDADE:

                if (textoUsuario.equalsIgnoreCase("OPCAO_TRIAGEM") || textoUsuario.contains("Triagem")) {
                    sessao.setFluxoTriagemCompleta(true); 
                    sessao.setEstagio(TriagemStage.TRIAGEM_IA);
                    return new BotResponseDTO("Sem problemas! Vou te fazer algumas perguntas para entender melhor o caso.\n\nPara começar: **O que você está sentindo?**");
                } 
                
                else if (textoUsuario.startsWith("ESPECIALIDADE_")) {

                    String especialidadeEscolhida = textoUsuario.replace("ESPECIALIDADE_", "");
                    
                    sessao.setFluxoTriagemCompleta(false); 
                    sessao.setEspecialidadeDetectada(especialidadeEscolhida); 
                    sessao.setEstagio(TriagemStage.PERGUNTA_DESCRICAO_OPCIONAL);
                    
                    return new BotResponseDTO(
                        "Certo, vamos buscar horários para **" + especialidadeEscolhida + "**.\n\n" +
                        "Antes de eu te mostrar a agenda, **você gostaria de descrever brevemente o que está sentindo?**\n" +
                        "Isso ajuda o médico a se preparar para a consulta.",
                        List.of("Sim, quero descrever", "Não, pular essa etapa")
                    );
                } 
                
                else {
                    return new BotResponseDTO("Por favor, selecione uma das opções da lista acima.");
                }

            case PERGUNTA_DESCRICAO_OPCIONAL:
                if (textoUsuario.toLowerCase().contains("sim") || textoUsuario.toLowerCase().contains("descrever")) {
                    sessao.setEstagio(TriagemStage.TRIAGEM_IA);
                    return new BotResponseDTO("Entendido. Por favor, conte-me em poucas palavras: **Quais são seus sintomas ou o motivo da consulta?**");
                } 
                
                else {
                    sessao.setResumoClinicoGerado("Paciente optou por não descrever sintomas previamente.");
                    
                    String especialidade = sessao.getEspecialidadeDetectada();
                    List<SlotDisponivelDTO> slots = adminService.buscarDisponibilidadeCombo(especialidade, sessao.getClinicaIdSelecionada());

                    if (slots.isEmpty()) {
                        sessionService.clearSession(telefone);
                        return new BotResponseDTO("Poxa, verifiquei aqui e não encontrei horários livres para " + especialidade + " nesta clínica nos próximos dias.");
                    }

                    List<BotResponseDTO.ListItemDTO> itensMenu = new ArrayList<>();
                    for (SlotDisponivelDTO slot : slots) {
                        String titulo = formatarDataCurta(slot.data()) + " às " + slot.horario().toString().substring(0, 5);
                        String descricao = slot.nomeMedico();
                        String idUnico = "AGENDAR_" + slot.medicoId() + "_" + slot.data() + "_" + slot.horario();
                        itensMenu.add(new BotResponseDTO.ListItemDTO(idUnico, titulo, descricao));
                    }
                    
                    sessao.setEstagio(TriagemStage.CONFIRMAR_AGENDAMENTO);
                    
                    return new BotResponseDTO(
                        "Ok! Indo direto para a agenda.\n" +
                        "Aqui estão os horários disponíveis para **" + especialidade + "**:", 
                        "Ver Horários", 
                        itensMenu       
                    );
                }

            case TRIAGEM_IA:
                sessao.adicionarAoHistorico("Paciente: " + textoUsuario);

                if (!sessao.isFluxoTriagemCompleta()) {
                    String resumo = ragQueryService.gerarResumoClinicoEstruturado(
                        sessao.getHistoricoClinico().toString(), 
                        sessao.getNome()
                    );
                    sessao.setResumoClinicoGerado(resumo);
                    
                    return buscarHorariosEGerarResposta(sessao, telefone);
                }

                else {
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
                    String especialidadeIA = ragQueryService.extrairEspecialidade(recomendacaoTexto);
                    
                    sessao.setEspecialidadeDetectada(especialidadeIA);
                    
                    String resumo = ragQueryService.gerarResumoClinicoEstruturado(sessao.getHistoricoClinico().toString(), sessao.getNome());
                    sessao.setResumoClinicoGerado(resumo);

                    BotResponseDTO respostaHorarios = buscarHorariosEGerarResposta(sessao, telefone);
                    
                    String msgFinal = recomendacaoTexto + "\n\n" + 
                                      "-----------------------------------\n" +
                                      "🔎 Com base nisso, busquei especialistas em *" + especialidadeIA + "* para você.\n" +
                                      respostaHorarios.getTexto(); 
                    
                    return new BotResponseDTO(
                        msgFinal, 
                        respostaHorarios.getTextoBotaoLista(), 
                        respostaHorarios.getItensLista()
                    );
            }

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
                        
                        return new BotResponseDTO(
                            "📝 *Confirmar Agendamento*\n\n" +
                            "🏥 Clínica ID: " + sessao.getClinicaIdSelecionada() + "\n" + 
                            "📅 Data: " + formatarDataCurta(sessao.getDataDesejada()) + "\n" +
                            "⏰ Horário: " + sessao.getHorarioSelecionado() + "\n" +
                            "🩺 Especialidade: " + sessao.getEspecialidadeDetectada() + "\n\n" +
                            "Posso confirmar?",
                            List.of("Sim, confirmar", "Cancelar") 
                        );
                        
                    } catch (Exception e) {
                        return new BotResponseDTO("Ocorreu um erro ao processar sua escolha.");
                    }
                }
                
                if (textoUsuario.toLowerCase().contains("sim") || textoUsuario.toLowerCase().contains("confirmar")) {
                    
                    boolean sucesso = adminService.agendarConsulta(
                        sessao.getClinicaIdSelecionada(), 
                        sessao.getMedicoSelecionado().getId(),
                        sessao.getDataDesejada(),
                        sessao.getHorarioSelecionado(),
                        sessao.getNome(),
                        telefone,
                        sessao.getIdade(),
                        sessao.getSexo(),
                        sessao.getCpf(), 
                        sessao.getResumoClinicoGerado()
                    );
                    
                    sessionService.clearSession(telefone);
                    
                    if (sucesso) {
                        return new BotResponseDTO("✅ *Agendamento Confirmado!* \nSeu CPF foi registrado e o médico já recebeu seu histórico.");
                    } else {
                        return new BotResponseDTO("❌ Ops! Esse horário foi ocupado agora mesmo.");
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


    private BotResponseDTO buscarHorariosEGerarResposta(UserSession sessao, String telefone) {

        String especialidade = sessao.getEspecialidadeDetectada();
        Long clinicaId = sessao.getClinicaIdSelecionada(); 


        List<SlotDisponivelDTO> slots = adminService.buscarDisponibilidadeCombo(especialidade, clinicaId);

        if (slots.isEmpty()) {
            sessionService.clearSession(telefone);
            return new BotResponseDTO("Poxa, verifiquei aqui e não encontrei horários livres para " + especialidade + " nesta clínica nos próximos dias.");
        }

        List<BotResponseDTO.ListItemDTO> itensMenu = new ArrayList<>();
        
        for (SlotDisponivelDTO slot : slots) {
            String titulo = formatarDataCurta(slot.data()) + " às " + slot.horario().toString().substring(0, 5);
            
            String descricao = slot.nomeMedico();
            if (slot.diaDaSemana() != null) descricao += " • " + slot.diaDaSemana();
            
            String idUnico = "AGENDAR_" + slot.medicoId() + "_" + slot.data() + "_" + slot.horario();
            
            itensMenu.add(new BotResponseDTO.ListItemDTO(idUnico, titulo, descricao));
        }
        
        sessao.setEstagio(TriagemStage.CONFIRMAR_AGENDAMENTO);
        
        return new BotResponseDTO(
            "Encontrei estes horários disponíveis para **" + especialidade + "**.\n" +
            "Toque no botão abaixo para ver as opções:", 
            "Ver Horários", 
            itensMenu       
        );
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