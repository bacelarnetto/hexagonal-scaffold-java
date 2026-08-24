package br.com.scaffold.shared.application.dto;

public record ErroResponseDTO(
        int status,
        String erro,
        String mensagem,
        String caminho
) {
}
