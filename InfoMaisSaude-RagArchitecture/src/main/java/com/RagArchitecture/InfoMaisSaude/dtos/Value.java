package com.RagArchitecture.InfoMaisSaude.dtos;

public record Value(String messaging_product, Message[] messages, Metadata metadata) {}