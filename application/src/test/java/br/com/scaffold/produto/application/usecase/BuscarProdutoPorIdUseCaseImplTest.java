package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarProdutoPorIdUseCaseImplTest {

    @Mock
    private ProdutoRepositoryPort port;

    // PS3: buscarPorId inexistente lanca NoSuchElementException
    @Test
    void ps3_executarComIdInexistenteLancaNoSuchElementException() {
        when(port.findById(99L)).thenReturn(Optional.empty());
        BuscarProdutoPorIdUseCase useCase = new BuscarProdutoPorIdUseCaseImpl(port);

        assertThrows(NoSuchElementException.class, () -> useCase.executar(99L));
    }
}
