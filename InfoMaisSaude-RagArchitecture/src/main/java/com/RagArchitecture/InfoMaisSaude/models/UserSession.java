package com.RagArchitecture.InfoMaisSaude.models;

import com.RagArchitecture.InfoMaisSaude.dtos.integration.ClinicaDTO;
import com.RagArchitecture.InfoMaisSaude.dtos.integration.MedicoDTO;
import com.RagArchitecture.InfoMaisSaude.enums.TriagemStage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class UserSession {

    private String telefone;
    private TriagemStage estagio = TriagemStage.INICIO;
    private String nome;
    private String idade;
    private String sexo;
    private String cpf;
    private Long clinicaIdSelecionada;
    private boolean fluxoTriagemCompleta;
    private String especialidadeManual;
    private StringBuilder historicoClinico = new StringBuilder();
    private int perguntasFeitas = 0;
    private String especialidadeDetectada;
    private List<MedicoDTO> medicosEncontrados = new ArrayList<>();
    private MedicoDTO medicoSelecionado;
    private LocalDate dataDesejada;
    private LocalTime horarioSelecionado;
    private String resumoClinicoGerado; 
    private Long consultaIdParaCancelar;
    private String nomeClinicaSelecionada;
    private List<ClinicaDTO> clinicasCache = new ArrayList<>();

    public UserSession(String telefone) {
        this.telefone = telefone;
    }
    
    public void adicionarAoHistorico(String texto) {
        historicoClinico.append(texto).append("\n");
    }
    
    public void incrementarPerguntas() {
        this.perguntasFeitas++;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public TriagemStage getEstagio() {
        return this.estagio;
    }

    public void setEstagio(TriagemStage estagio) {
        this.estagio = estagio;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIdade() {
        return this.idade;
    }

    public void setIdade(String idade) {
        this.idade = idade;
    }

    public String getSexo() {
        return this.sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public StringBuilder getHistoricoClinico() {
        return this.historicoClinico;
    }

    public void setHistoricoClinico(StringBuilder historicoClinico) {
        this.historicoClinico = historicoClinico;
    }

    public int getPerguntasFeitas() {
        return this.perguntasFeitas;
    }

    public void setPerguntasFeitas(int perguntasFeitas) {
        this.perguntasFeitas = perguntasFeitas;
    }

    public String getEspecialidadeDetectada() {
        return this.especialidadeDetectada;
    }

    public void setEspecialidadeDetectada(String especialidadeDetectada) {
        this.especialidadeDetectada = especialidadeDetectada;
    }

    public List<MedicoDTO> getMedicosEncontrados() {
        return this.medicosEncontrados;
    }

    public void setMedicosEncontrados(List<MedicoDTO> medicosEncontrados) {
        this.medicosEncontrados = medicosEncontrados;
    }

    public MedicoDTO getMedicoSelecionado() {
        return this.medicoSelecionado;
    }

    public void setMedicoSelecionado(MedicoDTO medicoSelecionado) {
        this.medicoSelecionado = medicoSelecionado;
    }

    public LocalDate getDataDesejada() {
        return this.dataDesejada;
    }

    public void setDataDesejada(LocalDate dataDesejada) {
        this.dataDesejada = dataDesejada;
    }

    public LocalTime getHorarioSelecionado() {
        return this.horarioSelecionado;
    }

    public void setHorarioSelecionado(LocalTime horarioSelecionado) {
        this.horarioSelecionado = horarioSelecionado;
    }

    public String getResumoClinicoGerado() {
        return this.resumoClinicoGerado;
    }

    public void setResumoClinicoGerado(String resumoClinicoGerado) {
        this.resumoClinicoGerado = resumoClinicoGerado;
    }


    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Long getClinicaIdSelecionada() {
        return this.clinicaIdSelecionada;
    }

    public void setClinicaIdSelecionada(Long clinicaIdSelecionada) {
        this.clinicaIdSelecionada = clinicaIdSelecionada;
    }

    public boolean isFluxoTriagemCompleta() {
        return this.fluxoTriagemCompleta;
    }

    public boolean getFluxoTriagemCompleta() {
        return this.fluxoTriagemCompleta;
    }

    public void setFluxoTriagemCompleta(boolean fluxoTriagemCompleta) {
        this.fluxoTriagemCompleta = fluxoTriagemCompleta;
    }

    public String getEspecialidadeManual() {
        return this.especialidadeManual;
    }

    public void setEspecialidadeManual(String especialidadeManual) {
        this.especialidadeManual = especialidadeManual;
    }


    public String getNomeClinicaSelecionada() {
        return this.nomeClinicaSelecionada;
    }

    public void setNomeClinicaSelecionada(String nomeClinicaSelecionada) {
        this.nomeClinicaSelecionada = nomeClinicaSelecionada;
    }

    public List<ClinicaDTO> getClinicasCache() {
        return this.clinicasCache;
    }

    public void setClinicasCache(List<ClinicaDTO> clinicasCache) {
        this.clinicasCache = clinicasCache;
    }


    public Long getConsultaIdParaCancelar() { return consultaIdParaCancelar; }
    
    public void setConsultaIdParaCancelar(Long consultaIdParaCancelar) { this.consultaIdParaCancelar = consultaIdParaCancelar; }
    

}