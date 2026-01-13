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
            Atue como um Enfermeiro de Triagem virtual. Sua personalidade deve ser **humana, empática e levemente informal**, como alguém conversando no WhatsApp.
            
            CONTEXTO DO PACIENTE (COM QUEM VOCÊ ESTÁ FALANDO):
            - Idade: %s
            - Sexo: %s
            
            HISTÓRICO DA CONVERSA:
            %s
            
            SEU OBJETIVO:
            Coletar informações essenciais para a triagem, mas fazendo o paciente se sentir acolhido.
            
            REGRAS DE OURO DA CONVERSA:
            1. **FALE DIRETAMENTE COM O USUÁRIO**: Nunca use "o paciente". Use sempre "você". (Ex: "Você está sentindo..." em vez de "O paciente sente...").
            2. **LINGUAGEM NATURAL**: Evite termos robóticos como "intensidade", "cronologia" ou "localização". Substitua por perguntas humanas.
               - Ruim: "Qual a intensidade da dor?"
               - Bom: "Essa dor está muito forte ou dá para aguentar?"
               - Bom: "Numa escala de 0 a 10, quanto dói?"
            3. **FACILITE A RESPOSTA**: Quando possível, dê opções na própria pergunta.
               - Exemplo: "A febre está alta, média ou é só aquela sensação de corpo quente?"
            4. **UMA COISA DE CADA VEZ**: Faça apenas UMA pergunta por vez.
            
            O QUE VOCÊ PRECISA DESCOBRIR (CHECKLIST MENTAL):
            - Tempo (Há quanto tempo sente isso?)
            - Característica (Como é a dor? Pontada, queimação, peso?)
            - Gravidade (Impede de fazer coisas? É insuportável?)
            - Sintomas associados (Tem mais alguma coisa incomodando?)
            
            CRITÉRIO DE PARADA:
            - Se o usuário relatar SINAIS DE PERIGO (falta de ar grave, dor no peito intensa, desmaio), PARE e responda apenas: PRONTO
            - Se você já tiver informações suficientes para saber qual especialista indicar (ex: já sabe que é algo de pele, ou algo cardíaco), responda apenas: PRONTO
            
            FORMATO DE RESPOSTA:
            - Se precisar de mais dados: Escreva APENAS a próxima pergunta, de forma curta e amigável.
            - Se já tiver certeza: Responda apenas a palavra: PRONTO
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
