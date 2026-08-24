package br.com.scaffold.produto.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public record Produto(
        Long id,
        String nome,
        BigDecimal custo,
        BigDecimal margemPercentual,
        BigDecimal valorVenda
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public Produto(String nome, BigDecimal custo, BigDecimal margemPercentual) {
        this(null, nome, custo, margemPercentual, null);
    }

    public Produto comId(Long id) {
        return new Produto(id, nome, custo, margemPercentual, valorVenda);
    }

    public Produto comValorVenda(BigDecimal valorVenda) {
        return new Produto(id, nome, custo, margemPercentual, valorVenda);
    }
}
