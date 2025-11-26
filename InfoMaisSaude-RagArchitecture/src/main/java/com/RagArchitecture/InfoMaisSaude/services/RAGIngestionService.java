package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.entities.Especialidade;
import com.RagArchitecture.InfoMaisSaude.repositories.EspecialidadeRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn; 
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@DependsOn("entityManagerFactory") 
public class RAGIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RAGIngestionService.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    @Value("${spring.ai.vectorstore.pgvector.table-name}")
    private String vectorTableName;

    @Value("${spring.ai.vectorstore.pgvector.dimension}")
    private int vectorDimension;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Iniciando setup da base de dados (PostgreSQL/pgvector)...");
        setupDatabase(); 

        log.info("Iniciando a indexação da base de conhecimento (SQL/JPA) na inicialização...");
        reindexarBaseDeConhecimento();
    }


    private void setupDatabase() {
        try {
            log.info("Garantindo que a extensão 'vector' exista...");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");

            log.info("Verificando e/ou criando a tabela de vetores: {}", vectorTableName);
            String createTableSql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id UUID PRIMARY KEY,
                    content TEXT,
                    metadata JSONB,
                    embedding vector(%d)
                );
                """, vectorTableName, vectorDimension);
            jdbcTemplate.execute(createTableSql);
            log.info("Tabela '{}' pronta.", vectorTableName);

            log.info("Verificando e/ou criando a tabela 'especialidades'...");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS especialidades (
                    id BIGSERIAL PRIMARY KEY,
                    nome VARCHAR(255) NOT NULL,
                    descricao TEXT,
                    sintomas_comuns TEXT
                );
            """);
            log.info("Tabela 'especialidades' pronta.");

            populateInitialData();

        } catch (Exception e) {
            log.error("Erro fatal durante o setup do banco de dados.", e);
            throw new RuntimeException(e);
        }
    }

 
    private void populateInitialData() {
        log.info("Verificando se há dados iniciais...");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM especialidades", Integer.class);

        if (count == null || count == 0) {
            log.warn("Tabela 'especialidades' está vazia. Populando com dados iniciais...");
            jdbcTemplate.execute("""
                INSERT INTO especialidades (nome, descricao, sintomas_comuns) VALUES
                (
                    'Clínico Geral',
                    'É o ponto de partida para a maioria dos problemas de saúde. Ideal para check-ups, prevenção e tratamento de sintomas comuns e abrangentes.',
                    'Febre, dor no corpo, mal-estar geral, tosse, dor de garganta, resfriado, gripe, dor de cabeça leve, náusea, vômito, diarreia, tontura leve, cansaço, dor abdominal geral.'
                ),
                (
                    'Neurologista',
                    'Médico especializado no diagnóstico e tratamento de doenças do sistema nervoso (cérebro, medula espinhal e nervos periféricos).',
                    'Dor de cabeça forte ou persistente (enxaqueca), tontura, vertigem, convulsões, perda de memória, confusão mental, formigamento, dormência, tremores, fraqueza muscular, dificuldade de fala, perda de visão súbita.'
                ),
                (
                    'Ortopedista',
                    'Médico que cuida da saúde do sistema locomotor, incluindo ossos, articulações, músculos, ligamentos e tendões.',
                    'Dor nas costas, dor na coluna, dor no pescoço, dor no joelho, dor no ombro, fraturas, torções, luxações, lesões esportivas, tendinite, bursite, dificuldade de locomoção, dor muscular intensa.'
                );
            """);
            log.info("Dados iniciais populados com sucesso!");
        } else {
            log.info("Tabela 'especialidades' já contém dados. Nenhuma ação necessária.");
        }
    }


    public void reindexarBaseDeConhecimento() {
        try {
            log.warn("Limpando a tabela de vetores: {}", vectorTableName);
            jdbcTemplate.update("DELETE FROM " + vectorTableName);

            log.info("Buscando especialidades no banco de dados SQL (via JPA Repository)...");
            List<Especialidade> especialidades = especialidadeRepository.findAll();

            if (especialidades.isEmpty()) {
                log.warn("Nenhuma especialidade encontrada. O RAG ficará vazio.");
                return;
            }

            List<Document> documentos = especialidades.stream().map(especialidade -> {
                String content = String.format(
                    "Especialidade: %s\nDescrição: %s\nSintomas Comuns: %s",
                    especialidade.getNome(),
                    especialidade.getDescricao(),
                    especialidade.getSintomasComuns()
                );
                Map<String, Object> metadata = Map.of("especialidade", especialidade.getNome());
                return new Document(content, metadata);
            }).collect(Collectors.toList());


            log.info("Iniciando indexação de {} especialidades no PgVector...", documentos.size());
            vectorStore.add(documentos);
            log.info("INDEXAÇÃO DE ESPECIALIDADES CONCLUÍDA!");

        } catch (Exception e) {
            log.error("Erro durante a reindexação da base de conhecimento.", e);
        }
    }
}