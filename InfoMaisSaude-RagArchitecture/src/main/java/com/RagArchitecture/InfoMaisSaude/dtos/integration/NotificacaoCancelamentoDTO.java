package com.RagArchitecture.InfoMaisSaude.dtos.integration;

public record NotificacaoCancelamentoDTO(
    String telefone,
    String nomePaciente,
    String nomeMedico,
    String dataHorario, 
    String motivo
) {}