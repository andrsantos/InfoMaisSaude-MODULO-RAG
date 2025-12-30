package com.RagArchitecture.InfoMaisSaude.dtos;

import java.util.List;

public class BotResponseDTO {

    private String texto;
    private List<String> botoes;
    private List<ListItemDTO> itensLista;
    private String textoBotaoLista;

    public record ListItemDTO(String id, String titulo, String descricao) {}

    public BotResponseDTO(String texto) {
        this.texto = texto;
        this.botoes = null;
    }

    public BotResponseDTO(String texto, List<String> botoes) {
        this.texto = texto;
        this.botoes = botoes; 
    }

    public BotResponseDTO(String texto, String textoBotaoLista, List<ListItemDTO> itensLista) {
        this.texto = texto;
        this.textoBotaoLista = textoBotaoLista;
        this.itensLista = itensLista;
    }

    public boolean temBotoes() { return botoes != null && !botoes.isEmpty(); }
    public boolean temLista() { return itensLista != null && !itensLista.isEmpty(); }


    public List<ListItemDTO> getItensLista() { 
        return itensLista; 
    }

    public String getTexto() {
        return this.texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public List<String> getBotoes() {
        return this.botoes;
    }

    public void setBotoes(List<String> botoes) {
        this.botoes = botoes;
    }
    public void setItensLista(List<ListItemDTO> itensLista) {
        this.itensLista = itensLista;
    }

    public String getTextoBotaoLista() { 
        return textoBotaoLista; 
    }

    public void setTextoBotaoLista(String textoBotaoLista) {
        this.textoBotaoLista = textoBotaoLista;
    }


}