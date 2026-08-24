package br.com.scaffold.produto.application.dto;

import java.math.BigDecimal;

public record ProdutoViewDTO(
        Long id,
        String nome,
        BigDecimal custo,
        BigDecimal margemPercentual,
        BigDecimal valorVenda
) {
}
