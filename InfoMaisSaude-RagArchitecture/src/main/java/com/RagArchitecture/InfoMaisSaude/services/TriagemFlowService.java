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
                
                String contextoPaciente = String.format("PACIENTE: %s, %s anos, %s.\nSINTOMAS: %s", 
                        sessao.getNome(), sessao.getIdade(), sessao.getSexo(), textoUsuario);
                
                String respostaIA = ragQueryService.obterRecomendacao(contextoPaciente);
                
                
                return respostaIA + "\n\n(Se tiver mais dúvidas, pode perguntar, ou digite 'sair' para encerrar)";

            default:
                return "Erro no fluxo. Digite 'reset' para reiniciar.";
        }
    }
}