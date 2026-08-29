package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.mapper.ProdutoFormMapper;
import br.com.scaffold.produto.application.mapper.ProdutoViewMapper;
import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import br.com.scaffold.shared.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contraste deliberado com o teste quebrado do projeto genetic (br.com.genetic.service.DnaServiceTest):
 * aqui SEMPRE chamamos o metodo real do use case primeiro, e so DEPOIS verificamos o mock e o
 * valor retornado. Nunca invocamos o mock diretamente no corpo do teste como se fosse o codigo em teste.
 */
@ExtendWith(MockitoExtension.class)
class CadastrarProdutoUseCaseImplTest {

    @Mock
    private ProdutoRepositoryPort port;

    private final ProdutoFormMapper formMapper = new ProdutoFormMapper();
    private final ProdutoViewMapper viewMapper = new ProdutoViewMapper();

    private CadastrarProdutoUseCase useCase;

    @BeforeEach
    void setup() {
        useCase = new CadastrarProdutoUseCaseImpl(port, formMapper, viewMapper);
    }

    // PS1: cadastrar calcula valorVenda via ProdutoPrecoLogic e persiste o produto ja com o preco calculado
    @Test
    void ps1_executarCalculaValorVendaAntesDeSalvar() {
        ProdutoInsertFormDTO dto = new ProdutoInsertFormDTO("Bolo de cenoura", new BigDecimal("40.00"), new BigDecimal("25"));
        when(port.save(any())).thenAnswer(invocation -> invocation.<Produto>getArgument(0).comId(1L));

        ProdutoViewDTO view = useCase.executar(dto);

        ArgumentCaptor<Produto> savedCaptor = ArgumentCaptor.forClass(Produto.class);
        verify(port).save(savedCaptor.capture());
        assertEquals(0, new BigDecimal("53.33").compareTo(savedCaptor.getValue().valorVenda()));
        assertEquals(0, new BigDecimal("53.33").compareTo(view.valorVenda()));
        assertEquals(1L, view.id());
    }

    // PS2: margem invalida (>=100) propaga RegraDeNegocioException e NUNCA chama port.save
    @Test
    void ps2_executarComMargemInvalidaNaoPersisteNada() {
        ProdutoInsertFormDTO dto = new ProdutoInsertFormDTO("Torta", new BigDecimal("10.00"), new BigDecimal("100"));

        assertThrows(RegraDeNegocioException.class, () -> useCase.executar(dto));

        verify(port, never()).save(any());
    }
}
