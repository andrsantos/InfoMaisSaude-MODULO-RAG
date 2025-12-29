package com.RagArchitecture.InfoMaisSaude.dtos;

import java.util.List;

public class BotResponseDTO {
    private String texto;
    private List<String> botoes;

    public BotResponseDTO(String texto) {
        this.texto = texto;
        this.botoes = null;
    }

    public BotResponseDTO(String texto, List<String> botoes) {
        this.texto = texto;
        this.botoes = botoes; 
    }

    public String getTexto() {
        return texto;
    }

    public List<String> getBotoes() {
        return botoes;
    }

    public boolean temBotoes() {
        return botoes != null && !botoes.isEmpty();
    }
}