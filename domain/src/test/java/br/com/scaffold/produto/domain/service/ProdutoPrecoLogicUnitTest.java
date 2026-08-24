package br.com.scaffold.produto.domain.service;

import br.com.scaffold.shared.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Nenhum mock, nenhum Spring context. Isso e o que "domain testavel sem framework" significa na pratica.
 */
class ProdutoPrecoLogicUnitTest {

    private final ProdutoPrecoLogic logic = new ProdutoPrecoLogic();

    // PP1: margem de 20% sobre custo de 100 -> venda de 125.00
    @Test
    void pp1_calcularValorVendaComMargemDe20PorCento() {
        BigDecimal valorVenda = logic.calcularValorVenda(new BigDecimal("100.00"), new BigDecimal("20"));

        assertEquals(0, new BigDecimal("125.00").compareTo(valorVenda));
    }

    // PP2: margem zero -> venda igual ao custo
    @Test
    void pp2_calcularValorVendaComMargemZero() {
        BigDecimal valorVenda = logic.calcularValorVenda(new BigDecimal("50.00"), BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("50.00").compareTo(valorVenda));
    }

    // PP3: margem >= 100% e regra de negocio invalida -> lanca RegraDeNegocioException
    @Test
    void pp3_calcularValorVendaComMargemMaiorOuIgualA100LancaExcecao() {
        assertThrows(RegraDeNegocioException.class, () ->
                logic.calcularValorVenda(new BigDecimal("10.00"), new BigDecimal("100")));
    }
}
