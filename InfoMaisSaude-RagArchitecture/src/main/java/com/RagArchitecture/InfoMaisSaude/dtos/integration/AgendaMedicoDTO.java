package com.RagArchitecture.InfoMaisSaude.dtos.integration;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AgendaMedicoDTO(
    
    Long id,
    Integer diaSemana,

    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioInicio,

    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioFim

){}
