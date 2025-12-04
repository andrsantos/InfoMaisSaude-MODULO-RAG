package com.RagArchitecture.InfoMaisSaude.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RAGQueryService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

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
        """;

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
}