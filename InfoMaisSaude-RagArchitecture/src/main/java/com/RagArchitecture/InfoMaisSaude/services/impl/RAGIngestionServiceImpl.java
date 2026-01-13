package com.RagArchitecture.InfoMaisSaude.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.RagArchitecture.InfoMaisSaude.entities.ConhecimentoClinico;
import com.RagArchitecture.InfoMaisSaude.repositories.ConhecimentoClinicoRepository;
import com.RagArchitecture.InfoMaisSaude.services.RAGIngestionService;

@Service
@DependsOn("entityManagerFactory") 
public class RAGIngestionServiceImpl implements RAGIngestionService, org.springframework.boot.ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(RAGIngestionService.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConhecimentoClinicoRepository conhecimentoRepository;

    @Value("${spring.ai.vectorstore.pgvector.table-name}")
    private String vectorTableName;

    @Value("${spring.ai.vectorstore.pgvector.dimension}")
    private int vectorDimension;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 Iniciando setup da base de dados RAG...");
        setupDatabase(); 
        
        log.info("📚 Iniciando reindexação granular...");
        reindexarBaseDeConhecimento();
    }

    public void setupDatabase() {
        try {
            
            log.warn("⚠️ LIMPANDO TABELAS ANTIGAS PARA RECRIAR ESTRUTURA...");
            jdbcTemplate.execute("DROP TABLE IF EXISTS conhecimento_clinico CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS especialidades CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + vectorTableName + " CASCADE");


            log.info("Configurando extensão vector...");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");

            String createVectorTable = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id UUID PRIMARY KEY,
                    content TEXT,
                    metadata JSONB,
                    embedding vector(%d)
                );
                """, vectorTableName, vectorDimension);
            jdbcTemplate.execute(createVectorTable);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS especialidades (
                    id BIGSERIAL PRIMARY KEY,
                    nome VARCHAR(255) NOT NULL UNIQUE,
                    descricao TEXT,
                    sintomas_comuns TEXT -- Mantemos para compatibilidade, mas o foco agora é a filha
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conhecimento_clinico (
                    id BIGSERIAL PRIMARY KEY,
                    especialidade_id BIGINT REFERENCES especialidades(id) ON DELETE CASCADE,
                    texto_conteudo TEXT NOT NULL,
                    tipo_conhecimento VARCHAR(50)
                );
            """);

            log.info("✅ Tabelas estruturadas criadas com sucesso.");

            populateInitialData();

        } catch (Exception e) {
            log.error("❌ Erro fatal durante o setup do banco.", e);
            throw new RuntimeException(e);
        }
    }

    public void populateInitialData() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM especialidades", Integer.class);

        if (count != null && count > 0) {
            log.info("Base de dados já contém informações. Pulando população.");
            return;
        }

        log.info("Populando base com Conhecimento Granular...");

        Long idCardio = inserirEspecialidade("Cardiologista", "Especialista em coração e circulação.");
        inserirFragmento(idCardio, "Dor no peito tipo aperto, sensação de elefante sentado no peito.", "SINTOMA_LEIGO");
        inserirFragmento(idCardio, "Angina, infarto agudo do miocárdio, taquicardia.", "TERMO_TECNICO");
        inserirFragmento(idCardio, "Coração disparado, batedeira no peito, palpitações fortes.", "SINTOMA_LEIGO");
        inserirFragmento(idCardio, "Pressão alta, hipertensão, tontura ao levantar, dor na nuca.", "SINTOMA_GERAL");
        inserirFragmento(idCardio, "Dor no peito que irradia para o braço esquerdo ou pescoço.", "RED_FLAG");

        Long idDermato = inserirEspecialidade("Dermatologista", "Especialista em pele, cabelos e unhas.");
        inserirFragmento(idDermato, "Manchas vermelhas na pele que coçam muito.", "SINTOMA_LEIGO");
        inserirFragmento(idDermato, "Erupção cutânea, urticária, dermatite, acne severa.", "TERMO_TECNICO");
        inserirFragmento(idDermato, "Pintas estranhas que mudam de cor ou sangram.", "RED_FLAG");
        inserirFragmento(idDermato, "Queda de cabelo excessiva, buracos no cabelo, unhas fracas.", "SINTOMA_LEIGO");

        Long idOrtopedia = inserirEspecialidade("Ortopedista", "Especialista em ossos e músculos.");
        inserirFragmento(idOrtopedia, "Dor nas costas que trava a coluna, dor na lombar.", "SINTOMA_LEIGO");
        inserirFragmento(idOrtopedia, "Fratura exposta, osso quebrado, torção no pé.", "RED_FLAG");
        inserirFragmento(idOrtopedia, "Dor no joelho ao subir escadas, dor nas juntas.", "SINTOMA_LEIGO");
        inserirFragmento(idOrtopedia, "Tendinite, bursite, dor no ombro.", "TERMO_TECNICO");

        Long idNeuro = inserirEspecialidade("Neurologista", "Especialista em cérebro e nervos.");
        inserirFragmento(idNeuro, "Dor de cabeça muito forte que não passa com remédio comum.", "SINTOMA_LEIGO");
        inserirFragmento(idNeuro, "Enxaqueca com aura, cefaleia tensional, epilepsia.", "TERMO_TECNICO");
        inserirFragmento(idNeuro, "Formigamento no rosto ou perda de força em um lado do corpo.", "RED_FLAG");
        inserirFragmento(idNeuro, "Tontura que tudo gira, labirintite, esquecimentos frequentes.", "SINTOMA_LEIGO");

        Long idClinico = inserirEspecialidade("Clínico Geral", "Médico generalista para triagem.");
        inserirFragmento(idClinico, "Mal estar geral, corpo mole, febre baixa, cansaço sem motivo.", "SINTOMA_GERAL");
        inserirFragmento(idClinico, "Dor de barriga, diarreia, vômito, enjoo.", "SINTOMA_LEIGO");
        inserirFragmento(idClinico, "Preciso de um check-up, exames de rotina, renovar receita.", "BUROCRATICO");

        Long idEndocrino = inserirEspecialidade("Endocrinologista", "Especialista em hormônios, glândulas e metabolismo.");
        inserirFragmento(idEndocrino, "Tenho diabetes, minha glicose está alta, açúcar no sangue descontrolado.", "SINTOMA_LEIGO");
        inserirFragmento(idEndocrino, "Sede excessiva, boca seca, vontade de fazer xixi toda hora, visão turva.", "SINTOMA_LEIGO"); // Sintomas clássicos de hiperglicemia
        inserirFragmento(idEndocrino, "Tenho problema na tireoide, hipotireoidismo, hipertireoidismo.", "TERMO_TECNICO");
        inserirFragmento(idEndocrino, "Nódulo no pescoço, caroço na garganta, bócio.", "RED_FLAG");
        inserirFragmento(idEndocrino, "Engordei muito sem motivo aparente, metabolismo lento, cansaço extremo e frio.", "SINTOMA_LEIGO"); // Hipotireoidismo
        inserirFragmento(idEndocrino, "Emagreci muito rápido, agitação, coração acelerado, calor excessivo e olhos saltados.", "SINTOMA_LEIGO"); // Hipertireoidismo
        inserirFragmento(idEndocrino, "Dificuldade para emagrecer, obesidade, preciso regular meus hormônios.", "SINTOMA_GERAL");
        inserirFragmento(idEndocrino, "Colesterol alto, triglicerídeos altos (dislipidemia).", "TERMO_TECNICO");
        inserirFragmento(idEndocrino, "Excesso de pelos no rosto (mulheres), acne hormonal, ovário policístico (SOP).", "SINTOMA_LEIGO");
        
        Long idUro = inserirEspecialidade("Urologista", "Especialista no sistema urinário (homens e mulheres) e sistema reprodutor masculino.");
        inserirFragmento(idUro, "Dor insuportável nas costas que desce para a virilha, cólica renal.", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Pedra no rim, cálculo renal, litíase.", "TERMO_TECNICO");
        inserirFragmento(idUro, "Urina com sangue (hematúria), xixi avermelhado ou escuro.", "RED_FLAG");
        inserirFragmento(idUro, "Ardência para urinar, dor ao fazer xixi, disúria.", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Vontade de ir ao banheiro toda hora mas sai pouco xixi (polaciúria).", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Infecção urinária, cistite, dor na bexiga.", "TERMO_TECNICO");
        inserirFragmento(idUro, "Dificuldade para iniciar o xixi, jato urinário fraco, gotejamento.", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Acordar muitas vezes a noite para urinar (nictúria).", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Exame da próstata, PSA alto, toque retal, prevenção de câncer de próstata.", "BUROCRATICO");
        inserirFragmento(idUro, "Impotência sexual, dificuldade de ereção, disfunção erétil, ejaculação precoce.", "SINTOMA_LEIGO");
        inserirFragmento(idUro, "Dor nos testículos, caroço no testículo, varicocele.", "SINTOMA_LEIGO");


        log.info("✅ População granular concluída!");
    }


    private Long inserirEspecialidade(String nome, String desc) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO especialidades (nome, descricao) VALUES (?, ?) RETURNING id",
            Long.class, nome, desc
        );
    }

    private void inserirFragmento(Long idEspecialidade, String texto, String tipo) {
        jdbcTemplate.update(
            "INSERT INTO conhecimento_clinico (especialidade_id, texto_conteudo, tipo_conhecimento) VALUES (?, ?, ?)",
            idEspecialidade, texto, tipo
        );
    }


    public void reindexarBaseDeConhecimento() {
        try {
            log.warn("🧹 Limpando tabela de vetores pgvector...");
            jdbcTemplate.update("DELETE FROM " + vectorTableName);

            log.info("🔍 Buscando fragmentos de conhecimento no banco SQL...");
            
            List<ConhecimentoClinico> conhecimentos = conhecimentoRepository.findAllComEspecialidade();

            if (conhecimentos.isEmpty()) {
                log.warn("Nenhum conhecimento clínico encontrado para indexar.");
                return;
            }

            List<Document> documentos = conhecimentos.stream().map(fragmento -> {
                
                String content = fragmento.getTextoConteudo();
                
                Map<String, Object> metadata = Map.of(
                    "especialidade", fragmento.getEspecialidade().getNome(),
                    "tipo", fragmento.getTipoConhecimento(),
                    "descricao_especialidade", fragmento.getEspecialidade().getDescricao() 
                );
                
                return new Document(content, metadata);
            }).collect(Collectors.toList());

            log.info("🧠 Indexando {} fragmentos no PgVector...", documentos.size());
            vectorStore.add(documentos);
            log.info("✅ INDEXAÇÃO GRANULAR CONCLUÍDA!");

        } catch (Exception e) {
            log.error("❌ Erro durante a reindexação.", e);
        }
    }
}