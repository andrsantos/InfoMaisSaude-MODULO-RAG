package com.RagArchitecture.InfoMaisSaude.services;


public interface RAGQueryService {

    String classificarIntencao(String textoUsuario);
    String obterRecomendacao(String sintomasDoUsuario);
    String analisarSintomas(String historico, String idade, String sexo);
    String extrairEspecialidade(String textoDaIA);
    String gerarResumoClinicoEstruturado(String historicoChat, String dadosPaciente);
   

}