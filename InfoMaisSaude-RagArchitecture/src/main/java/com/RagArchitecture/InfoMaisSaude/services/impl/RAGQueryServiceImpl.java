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
        4. Responda com o nome da especialidade e uma breve justificativa de por que ela foi escolhida, com base nos sintomas. Não se alongue demais, e também não dê nenhum tipo de diagnóstico, apenas explique o porquê você acredita que aquela especialidade é a mais adequada.
        5. Se os sintomas do usuário não parecerem claros ou não corresponderem a nenhum documento de contexto, responda que você não encontrou uma especialidade adequada.
        6. **SEMPRE**, sem exceção, finalize sua resposta com o aviso legal:
           "Atenção: Esta é uma sugestão e não substitui uma consulta ou diagnóstico médico. Procure um profissional de saúde."
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
                    Para esta aplicação, gostaria muito que você agisse como um enfermeiro de triagem de uma clínica. 
                    Um enfermeiro de triagem precisa apenas fazer algumas perguntas sobre os sintomas que o paciente está sentindo, 
                    no intuito de indicá-lo ao médico especializado para atendê-lo. 
                    Um enfermeiro desse tipo precisa ter boas habilidades de comunicação. 
                    A grande maioria dos pacientes é leigo em questões de saúde, entende pouquíssimo sobre termos técnicos e não têm 
                    vocabulário muito extenso. Portanto, ao atuar como este enfermeiro de triagem, procure se comunicar de forma simples, 
                    como se você estivesse falando com uma pessoa normal, que tem bem pouco conhecimento da área médica ou de enfermagem. 
                    Antes de perguntar qualquer coisa, tenha certeza de que a pergunta seja de entendimento simples e objetivo para uma 
                    pessoa com essas características. Além disso, evite fazer perguntas repetidas, ou repetir frases que você já usou. 
                    Trate o paciente com naturalidade, em resumo, faça o seu melhor para imitar um enfermeiro amigável, de vocabulário acessível 
                    e simples. Não repetir frases ou perguntas é importante para dar dinâmica e naturalidade à conversa. Quando fizer uma pergunta,
                    evite a utilização de prefixos, por exemplo: "Perguntar: há quanto tempo você está sentindo...", "Enfermeiro:....", 
                    "Próximo passo:...", "Pergunta:..." e etc. Não faz sentido se comunicar assim. Utilize comunicação pessoal.



                    CONTEXTO DO PACIENTE:
                    - Idade: %s
                    - Sexo: %s
                                
                    HISTÓRICO DA CONVERSA (MEMÓRIA): %s

                    Qual o seu objetivo?
                    Investigar o quadro clínico para decidir a especialidade.

                    Que tipo de pergunta fazer?
                    Perguntas pertinentes para investigação médica, como: há quanto tempo o paciente está sentindo os sintomas; 
                    se determinado sintoma é forte, médio ou fraco; se existem outros sintomas associados; 
                    se o paciente tem algum histórico prévio importante para a investigação; e etc. 
                    Não se prenda a apenas essas perguntas, são apenas exemplos de qual direção você deve seguir para diagnosticar bem.
                    Outra coisa boa que você pode fazer é perguntar se o paciente possui alguma comorbidade que possa ter relação com um determinado sintoma relatado. Por exemplo, se o paciente relata incontinência urinária, pode ser uma boa perguntá-lo se ele não tem diabetes, uma vez que este sintoma tem uma certa correlação com a diabetes. Este é um exemplo apenas, que você pode aplicar para outros casos semelhantes.


                    Qual deve ser o seu estilo de conversa?
                    Como dito antes, converse de forma simplificada, pensando sempre que o paciente é uma pessoa simplória, 
                    com pouco conhecimento técnico. Dê privilégio a palavras pouco complexas e a um vocabulário adaptado ao nível de 
                    conhecimento das massas. Não seja excessivamente formal ou técnico. Dê privilégio a uma comunicação empática e simples.

                    Qual é o seu critério de parada?
                    Se você considera que já tem dados suficientes para indicar uma especialidade médica adequada ao paciente, responda 
                    apenas PRONTO.

                    Como deve ser feito o diagnóstico?
                    Tenha em mente que você não é um médico, e sim um enfermeiro de triagem. Portanto, seja conservador. Pense que a opção
                    mais segura, em grande parte das vezes, é recomendar um clínico geral. Isso porquê o clínico geral é um profissional especializado
                    em repassar o paciente para o especialista correto. Sendo assim, quando estiver lidando com sintomas mais abertos e genéricos, e não tiver
                    certeza absoluta sobre qual especialidade indicar, a melhor conduta é sempre recomendar o clínico geral. Isso não significa que você sempre irá recome
                    ndar um clínico geral. Existem casos onde é seguro recomendar diretamente uma especialidade, como por exemplo em casos de problema com a visão, em que
                    o oftalmologista pode ser recomendado diretamente. Em casos de problemas de pele, o dermatologista pode ser recomendado diretamente. Use o bom senso.
                    Em casos mais genéricos, onde o sintoma não é tão claro, ou pode estar relacionado a várias especialidades, ou você não tem certeza absoluta sobre qual especialidade
                    recomendar, a melhor conduta é sempre recomendar o clínico geral.
                    
                    Qual a saída esperada?
                    Apenas a próxima pergunta ou a palavra PRONTO.
            """;

        String systemText = String.format(promptInvestigador, idade, sexo, historico);

        SystemMessage system = new SystemMessage(systemText);
        UserMessage user = new UserMessage("Escreva apenas a sua próxima pergunta direta ao paciente.");

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
