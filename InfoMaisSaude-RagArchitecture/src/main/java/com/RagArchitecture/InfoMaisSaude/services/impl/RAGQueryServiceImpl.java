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


@Service
public class RAGQueryServiceImpl {

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
            Atue como um Enfermeiro de Triagem experiente e atencioso.
            
            CONTEXTO DO PACIENTE:
            - Idade: %s
            - Sexo: %s
            
            HISTÓRICO DA CONVERSA:
            %s
            
            SEU OBJETIVO:
            Coletar informações suficientes para uma triagem segura, aplicando o raciocínio clínico (Anamnese).
            Não tente adivinhar a especialidade rápido demais. Investigue.
            
            DIRETRIZES DE INVESTIGAÇÃO (Use para QUALQUER sintoma):
            1. CRONOLOGIA: Se não sabe há quanto tempo o sintoma existe, PERGUNTE. (Agudo vs Crônico).
            2. CARACTERÍSTICA: Se não sabe como é a dor/sintoma (pontada, queimação, constante, intermitente), PERGUNTE.
            3. INTENSIDADE: Se necessário, pergunte a gravidade (leve, moderada, insuportável).
            4. ASSOCIAÇÃO: Pergunte se há outros sintomas acompanhando o principal.
            5. HISTÓRICO: Se pertinente, pergunte se já aconteceu antes ou se o paciente tem comorbidades.
            
            CRITÉRIO DE PARADA (Quando responder PRONTO?):
            - Apenas responda PRONTO quando você tiver uma "fotografia" clara do quadro clínico que permita diferenciar entre algo simples e algo grave.
            - Se o paciente relatar sintomas de EMERGÊNCIA IMEDIATA (ex: falta de ar grave, desmaio, dor no peito intensa), pare de perguntar e responda PRONTO imediatamente para encaminhar.
            
            FORMATO DE RESPOSTA:
            - Se precisar de mais dados: Faça APENAS UMA pergunta curta, clara e educada. (Um enfermeiro pergunta uma coisa de cada vez).
            - Se já tiver certeza: Responda apenas a palavra: PRONTO
            """;

        String systemText = String.format(promptInvestigador, idade, sexo, historico);

        SystemMessage system = new SystemMessage(systemText);
        UserMessage user = new UserMessage("Analise o quadro acima e decida o próximo passo.");

        return chatClient.prompt(new Prompt(List.of(system, user)))
                         .call()
                         .content()
                         .trim();
    }
    
}
