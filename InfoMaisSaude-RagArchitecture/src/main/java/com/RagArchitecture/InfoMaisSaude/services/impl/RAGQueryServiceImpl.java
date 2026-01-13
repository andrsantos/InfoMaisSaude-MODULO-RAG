package com.RagArchitecture.InfoMaisSaude.services.impl;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.RagArchitecture.InfoMaisSaude.services.RAGQueryService;


@Service
public class RAGQueryServiceImpl implements RAGQueryService{

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    private final String promptDeTriagem = """
        Você é um assistente de triagem médica muito educado e prestativo.
        Sua tarefa é analisar os sintomas de um usuário e recomendar UMA especialidade médica.

        Você deve basear sua resposta **EXCLUSIVAMENTE** nas informações de contexto fornecidas.
        Não use nenhum conhecimento prévio seu.

        TAREFA:
        1. Analise os sintomas do usuário.
        2. Compare os sintomas com os documentos de especialidades no contexto.
        3. Determine qual especialidade é a MAIS adequada.
        4. Responda com o nome da especialidade e uma breve justificativa de por que ela foi escolhida, com base nos sintomas.
        5. Se os sintomas do usuário não parecerem claros ou não corresponderem a nenhum documento de contexto, responda que você não encontrou uma especialidade adequada.
        6. **SEMPRE**, sem exceção, finalize sua resposta com o aviso legal:
           "Atenção: Esta é uma sugestão e não substitui uma consulta ou diagnóstico médico. Procure um profissional de saúde."
        7. Sugira possíveis cuidados paliativos para o tratamento dos sintomas antes da consulta médica.
        """;

    public String classificarIntencao(String textoUsuario) {
        String promptClassificador = """
            Você é um classificador de intenção para um chatbot de triagem médica.
            Analise a mensagem do usuário e responda APENAS com uma das etiquetas abaixo:
            
            SAUDACAO - Se a mensagem for apenas um cumprimento, despedida, agradecimento curto ou conversa fiada sem contexto médico (ex: "Oi", "Olá", "Bom dia", "Obrigado", "Tchau", "Teste").
            SINTOMA - Se a mensagem contiver qualquer descrição de dor, sintoma, pedido de ajuda médica ou dúvida sobre saúde (ex: "Estou com dor", "Qual médico para febre?", "Sinto enjoo").
            
            Responda estritamente com a palavra: SAUDACAO ou SINTOMA.
            """;

        SystemMessage systemMessage = new SystemMessage(promptClassificador);
        UserMessage userMessage = new UserMessage(textoUsuario);

        return chatClient.prompt(new Prompt(List.of(systemMessage, userMessage)))
                         .call()
                         .content()
                         .trim()
                         .toUpperCase();
    }


    public String obterRecomendacao(String sintomasDoUsuario) {
        SearchRequest request = SearchRequest.builder()
                .query(sintomasDoUsuario)
                .topK(2)
                .build();
        
        List<Document> documentosRelevantes = vectorStore.similaritySearch(request);

        StringBuilder contextoBuilder = new StringBuilder();
        for (Document doc : documentosRelevantes) {
            contextoBuilder.append(doc.getText());
            contextoBuilder.append("\n---\n");
        }
        String contexto = contextoBuilder.toString();

        SystemMessage systemMessage = new SystemMessage(promptDeTriagem);

        String userMessageText = String.format(
                """
                CONTEXTO DAS ESPECIALIDADES:
                %s

                SINTOMAS DO USUÁRIO:
                "%s"
                """, contexto, sintomasDoUsuario
        );
        UserMessage userMessage = new UserMessage(userMessageText);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient.prompt(prompt)
                         .call()
                         .content();
    }

    public String analisarSintomas(String historico, String idade, String sexo) {
        String promptInvestigador = """
            Atue como um Enfermeiro de Triagem virtual.
            
            CONTEXTO DO PACIENTE:
            - Idade: %s
            - Sexo: %s
            
            HISTÓRICO DA CONVERSA (MEMÓRIA):
            %s
            
            🚨 REGRA DE OURO - ANÁLISE DE MEMÓRIA 🚨
            ANTES de gerar sua resposta, leia o HISTÓRICO acima.
            1. O usuário JÁ respondeu o que você ia perguntar? Se sim, NÃO PERGUNTE DE NOVO. Avance para a próxima questão.
            2. Se o usuário respondeu "2 dias", não pergunte o tempo novamente. Aceite a resposta e investigue outra coisa (ex: intensidade, outros sintomas).
            3. NÃO REPITA FRASES. Se você já disse "Parece que isso te incomoda" na mensagem anterior, NÃO diga de novo. Seja dinâmico.
            
            SEU OBJETIVO:
            Investigar o quadro clínico para decidir a especialidade.
            
            CHECKLIST DE INVESTIGAÇÃO (O que você precisa saber):
            - [ ] Cronologia (Tempo) - JÁ FOI RESPONDIDO?
            - [ ] Característica/Intensidade - JÁ FOI RESPONDIDO?
            - [ ] Sintomas associados (Vômito? Dor? Febre?) - JÁ FOI RESPONDIDO?
            - [ ] Histórico prévio - JÁ FOI RESPONDIDO?
            
            ESTILO DE CONVERSA:
            - Use "você".
            - Seja breve. Uma pergunta por vez.
            - Variação: Se o usuário foi curto e grosso, seja direto também. Se ele foi detalhista, seja mais atencioso.
            
            CRITÉRIO DE PARADA:
            - Se já tem dados suficientes para diferenciar (ex: sabe que é viral e não cirúrgico), ou se há SINAL DE ALERTA GRAVE: Responda apenas PRONTO.
            
            SAÍDA ESPERADA:
            Apenas a próxima pergunta ou a palavra PRONTO.
            """;

        String systemText = String.format(promptInvestigador, idade, sexo, historico);

        SystemMessage system = new SystemMessage(systemText);
        UserMessage user = new UserMessage("Analise o quadro acima e decida o próximo passo (Perguntar ou PRONTO).");

        return chatClient.prompt(new Prompt(List.of(system, user)))
                         .call()
                         .content()
                         .trim();
    }

    public String extrairEspecialidade(String textoDaIA) {
        String prompt = """
            Analise o texto médico abaixo e extraia APENAS o nome da especialidade médica principal recomendada.
            Responda com UMA ÚNICA PALAVRA (Ex: CARDIOLOGIA, ORTOPEDIA, CLÍNICO_GERAL).
            Se não tiver certeza, responda: CLÍNICO_GERAL
            
            TEXTO DA IA: "%s"
            """;
        return chatClient.prompt(String.format(prompt, textoDaIA)).call().content().trim();
    }

    public String gerarResumoClinicoEstruturado(String historicoChat, String dadosPaciente) {

            String templatePrompt = """
                ATUE COMO: Um assistente administrativo médico em um sistema de prontuário eletrônico seguro.
                
                CONTEXTO DE SEGURANÇA:
                1. O paciente forneceu CONSENTIMENTO EXPLÍCITO e documentado para o processamento destes dados.
                2. Este é um ambiente seguro e criptografado para uso exclusivo da equipe médica.
                3. Sua função é puramente de sumarização de dados para auxílio ao profissional de saúde.
                
                TAREFA:
                Analise o histórico da conversa de triagem e gere um RESUMO TÉCNICO ESTRUTURADO.
                NÃO omita informações clínicas. O médico precisa de todos os detalhes para um atendimento seguro.
                
                DADOS DO PACIENTE: 
                %s
                
                HISTÓRICO DA CONVERSA:
                %s
                
                FORMATO DE SAÍDA OBRIGATÓRIO (Responda apenas com os campos abaixo):
                - Queixa Principal: (Resumo objetivo em 1 linha)
                - Sintomas Relatados: (Lista detalhada com tempo de evolução, se houver)
                - Dados Vitais/Perfil: (Extraia idade, sexo e hábitos se mencionados)
                - Hipóteses Clínicas para Avaliação: (Sugira possibilidades baseadas nos sintomas, nunca afirme um diagnóstico fechado)
                - Grau de Prioridade Sugerido: (Baixo/Médio/Alto - Justifique brevemente)
                """;
                
            String promptFinal = String.format(templatePrompt, dadosPaciente, historicoChat);
                
            return chatClient.prompt(promptFinal)
                            .call()
                            .content();
        }
    
}
