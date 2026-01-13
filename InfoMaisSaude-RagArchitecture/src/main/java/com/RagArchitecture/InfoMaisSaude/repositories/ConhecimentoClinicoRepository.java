package com.RagArchitecture.InfoMaisSaude.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.RagArchitecture.InfoMaisSaude.entities.ConhecimentoClinico;

public interface ConhecimentoClinicoRepository extends JpaRepository<ConhecimentoClinico, Long> {
    
    @Query("SELECT c FROM ConhecimentoClinico c JOIN FETCH c.especialidade")
    List<ConhecimentoClinico> findAllComEspecialidade();
}