package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.shared.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mocka CadastrarProdutoUseCase (o use case vizinho, nao a porta) -- prova que a orquestracao do
 * lote (parar no primeiro item invalido) funciona isolada da regra de preco. O rollback do que ja
 * foi "salvo" antes do item invalido e responsabilidade do @Transactional (Spring), nao testavel
 * com mock -- ver PCI3 no teste de integracao.
 */
@ExtendWith(MockitoExtension.class)
class CadastrarProdutoEmLoteUseCaseImplTest {

    @Mock
    private CadastrarProdutoUseCase cadastrarProduto;

    // PS4: cadastrarEmLote para no primeiro item invalido -- nao chama cadastrarProduto para os
    // itens seguintes ao que falhou
    @Test
    void ps4_executarInterrompeNoPrimeiroItemInvalido() {
        ProdutoInsertFormDTO dtoValido1 = new ProdutoInsertFormDTO("Bolo", new BigDecimal("40.00"), new BigDecimal("25"));
        ProdutoInsertFormDTO dtoInvalido = new ProdutoInsertFormDTO("Torta", new BigDecimal("10.00"), new BigDecimal("100"));
        ProdutoInsertFormDTO dtoValido2 = new ProdutoInsertFormDTO("Brigadeiro", new BigDecimal("2.00"), new BigDecimal("50"));
        when(cadastrarProduto.executar(dtoValido1))
                .thenReturn(new ProdutoViewDTO(1L, "Bolo", dtoValido1.custo(), dtoValido1.margemPercentual(), new BigDecimal("53.33")));
        when(cadastrarProduto.executar(dtoInvalido))
                .thenThrow(new RegraDeNegocioException("Margem percentual deve ser menor que 100%"));
        CadastrarProdutoEmLoteUseCase useCase = new CadastrarProdutoEmLoteUseCaseImpl(cadastrarProduto);

        assertThrows(RegraDeNegocioException.class, () -> useCase.executar(List.of(dtoValido1, dtoInvalido, dtoValido2)));

        verify(cadastrarProduto, times(1)).executar(dtoValido1);
        verify(cadastrarProduto, never()).executar(dtoValido2);
    }
}
