package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.entities.Especialidade;
import com.RagArchitecture.InfoMaisSaude.repositories.EspecialidadeRepository; 
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
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
        log.info("Iniciando a indexação da base de conhecimento (SQL/JPA) na inicialização...");
        reindexarBaseDeConhecimento();
    }

    public void reindexarBaseDeConhecimento() {
        try {
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

            log.warn("Limpando a tabela de vetores: {}", vectorTableName);
            int rowsDeleted = jdbcTemplate.update("DELETE FROM " + vectorTableName);
            log.info("Limpeza concluída. {} registros antigos deletados.", rowsDeleted);

            log.info("Buscando especialidades no banco de dados SQL (via JPA Repository)...");
            List<Especialidade> especialidades = especialidadeRepository.findAll();

            if (especialidades.isEmpty()) {
                log.warn("Nenhuma especialidade encontrada na tabela 'especialidades'. A base de conhecimento RAG ficará vazia.");
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