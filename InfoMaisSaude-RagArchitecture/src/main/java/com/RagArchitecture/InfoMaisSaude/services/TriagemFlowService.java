package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.dtos.BotResponseDTO;

public interface TriagemFlowService {

   BotResponseDTO processarMensagem(String telefone, String textoUsuario);
   
}