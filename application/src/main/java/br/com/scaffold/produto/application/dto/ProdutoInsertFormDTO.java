package br.com.scaffold.produto.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProdutoInsertFormDTO(
        @NotBlank
        String nome,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal custo,

        @NotNull
        BigDecimal margemPercentual
) {
}
