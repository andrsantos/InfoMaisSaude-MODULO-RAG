package com.RagArchitecture.InfoMaisSaude.dtos.integration;

public record NotificacaoPosConsultaDTO(
    String telefone,
    String nomePaciente,
    String nomeMedico,
    String prescricao
) {}