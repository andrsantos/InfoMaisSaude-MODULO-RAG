package com.RagArchitecture.InfoMaisSaude.dtos;

public record MessageDTO(String from, String id, String timestamp, TextDTO text, String type) {}