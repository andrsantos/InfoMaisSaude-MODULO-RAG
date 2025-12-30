package com.RagArchitecture.InfoMaisSaude.dtos;

public record InteractiveDTO(
    String type, 
    ButtonReplyDTO button_reply,
    ListReplyDTO list_reply
) {}