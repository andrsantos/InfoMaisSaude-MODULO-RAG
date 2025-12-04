package com.RagArchitecture.InfoMaisSaude.models;

import com.RagArchitecture.InfoMaisSaude.enums.TriagemStage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSession {

    private String telefone;
    private TriagemStage estagio;
    private LocalDateTime ultimaInteracao;
    private String sexo;
    private List<String> historicoConversa;
    private String nome;
    private String idade;
    private int perguntasFeitas = 0;
    private StringBuilder historicoClinico = new StringBuilder();

    public int getPerguntasFeitas() { return perguntasFeitas; }
    
    public void incrementarPerguntas() { this.perguntasFeitas++; }
    
    public void adicionarAoHistorico(String texto) {
        this.historicoClinico.append(texto).append("\n");
    }
    
    public String getHistoricoClinico() {
        return historicoClinico.toString();
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public LocalDateTime getUltimaInteracao() {
        return this.ultimaInteracao;
    }

    public void setUltimaInteracao(LocalDateTime ultimaInteracao) {
        this.ultimaInteracao = ultimaInteracao;
    }

    public List<String> getHistoricoConversa() {
        return this.historicoConversa;
    }

    public void setHistoricoConversa(List<String> historicoConversa) {
        this.historicoConversa = historicoConversa;
    }

    public UserSession(String telefone) {
        this.telefone = telefone;
        this.estagio = TriagemStage.INICIO;
        this.ultimaInteracao = LocalDateTime.now();
        this.historicoConversa = new ArrayList<>();
    }

    public String getTelefone() { return telefone; }
    public TriagemStage getEstagio() { return estagio; }
    public void setEstagio(TriagemStage estagio) { this.estagio = estagio; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIdade() { return idade; }
    public void setIdade(String idade) { this.idade = idade; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    
    public void adicionarHistorico(String remetente, String mensagem) {
        this.historicoConversa.add(remetente + ": " + mensagem);
        this.ultimaInteracao = LocalDateTime.now();
    }
    
    public String getHistoricoFormatado() {
        return String.join("\n", historicoConversa);
    }
}