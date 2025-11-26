package com.RagArchitecture.InfoMaisSaude.dtos;

public record Message(String from, String id, String timestamp, Text text, String type) {}