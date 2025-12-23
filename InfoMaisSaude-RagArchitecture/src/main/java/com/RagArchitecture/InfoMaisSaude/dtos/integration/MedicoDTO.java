package com.RagArchitecture.InfoMaisSaude.dtos.integration;

import java.util.List;

public class MedicoDTO {

    Long id;
    String nome;
    String especializacao;
    String telefone;
    List<AgendaMedicoDTO> agenda; 
    String login;
    String senha;


    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEspecializacao() {
        return this.especializacao;
    }

    public void setEspecializacao(String especializacao) {
        this.especializacao = especializacao;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public List<AgendaMedicoDTO> getAgenda() {
        return this.agenda;
    }

    public void setAgenda(List<AgendaMedicoDTO> agenda) {
        this.agenda = agenda;
    }

    public String getLogin() {
        return this.login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return this.senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    
}
