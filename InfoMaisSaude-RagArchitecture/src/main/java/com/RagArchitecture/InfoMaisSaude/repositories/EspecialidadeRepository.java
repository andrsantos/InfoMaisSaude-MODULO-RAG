package com.RagArchitecture.InfoMaisSaude.repositories; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecialidadeRepository extends JpaRepository<com.RagArchitecture.InfoMaisSaude.entities.Especialidade, Long> {

}