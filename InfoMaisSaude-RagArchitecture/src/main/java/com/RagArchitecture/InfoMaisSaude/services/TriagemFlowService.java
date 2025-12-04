package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.enums.TriagemStage;
import com.RagArchitecture.InfoMaisSaude.models.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TriagemFlowService {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RAGQueryService ragQueryService; 

    public String processarMensagem(String telefone, String textoUsuario) {
        UserSession sessao = sessionService.getOrCreateSession(telefone);
        
        if (textoUsuario.equalsIgnoreCase("reset") || textoUsuario.equalsIgnoreCase("sair")) {
            sessionService.clearSession(telefone);
            return "Atendimento encerrado. Se precisar, mande um 'Oi' para começar de novo.";
        }

        switch (sessao.getEstagio()) {
            case INICIO:
                sessao.setEstagio(TriagemStage.AGUARDANDO_NOME);
                return "Olá! Sou o assistente virtual do *Informa + Saúde*. \n\nPara começarmos sua triagem, por favor, digite seu **Nome Completo**.";

            case AGUARDANDO_NOME:
                sessao.setNome(textoUsuario);
                sessao.setEstagio(TriagemStage.AGUARDANDO_IDADE);
                return "Prazer, " + textoUsuario + "! \nAgora, por favor, me diga sua **Idade**.";

            case AGUARDANDO_IDADE:
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
                        sessao.getHistoricoClinico(), 
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
                    "PACIENTE: %s, %s anos, %s.\n" +
                    "HISTÓRICO COMPLETO DA CONVERSA:\n%s\n\n" +
                    "TAREFA FINAL: Faça um resumo do quadro clínico e indique a especialidade médica adequada.", 
                    sessao.getNome(), sessao.getIdade(), sessao.getSexo(), sessao.getHistoricoClinico()
                );
                
                String recomendacaoFinal = ragQueryService.obterRecomendacao(perfilCompleto);
                
                sessionService.clearSession(telefone);
                
                return recomendacaoFinal + "\n\n(Atendimento finalizado. Se precisar de mais ajuda, mande um 'Oi')";

            default:
                return "Erro no fluxo. Digite 'reset' para reiniciar.";
        }
    }
}