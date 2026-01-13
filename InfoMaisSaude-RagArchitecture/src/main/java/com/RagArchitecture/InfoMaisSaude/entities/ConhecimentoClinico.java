package com.RagArchitecture.InfoMaisSaude.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conhecimento_clinico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConhecimentoClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "especialidade_id", nullable = false)
    private Especialidade especialidade;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String textoConteudo;

    @Column(length = 50)
    private String tipoConhecimento; 
}