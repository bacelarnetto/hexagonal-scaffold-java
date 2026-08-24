package br.com.scaffold.produto.domain.service;

import br.com.scaffold.shared.domain.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de negocio pura: sem @Service, sem @Transactional, sem log de infra.
 * Recebe tudo por parametro -- nada de Instant.now() ou I/O aqui dentro.
 * application/service/ instancia esta classe direto (new), nao e um bean Spring.
 */
public class ProdutoPrecoLogic {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public BigDecimal calcularValorVenda(BigDecimal custo, BigDecimal margemPercentual) {
        if (margemPercentual.compareTo(CEM) >= 0) {
            throw new RegraDeNegocioException(
                    "Margem percentual deve ser menor que 100%% (recebido: %s)".formatted(margemPercentual));
        }
        BigDecimal margemFracao = margemPercentual.divide(CEM);
        return custo.divide(BigDecimal.ONE.subtract(margemFracao), 2, RoundingMode.HALF_UP);
    }
}
