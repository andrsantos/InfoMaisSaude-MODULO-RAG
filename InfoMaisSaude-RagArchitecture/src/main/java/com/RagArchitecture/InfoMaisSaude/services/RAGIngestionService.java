package com.RagArchitecture.InfoMaisSaude.services;
import org.springframework.boot.ApplicationArguments;

public interface RAGIngestionService  {
    void reindexarBaseDeConhecimento();
    void setupDatabase();
    void populateInitialData();
    void run(ApplicationArguments args) throws Exception;
}