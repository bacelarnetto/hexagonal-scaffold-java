package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class BuscarProdutoPorIdUseCaseImpl implements BuscarProdutoPorIdUseCase {

    private final ProdutoRepositoryPort port;

    public BuscarProdutoPorIdUseCaseImpl(ProdutoRepositoryPort port) {
        this.port = port;
    }

    @Override
    public Produto executar(Long id) {
        return port.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com o ID " + id));
    }
}
