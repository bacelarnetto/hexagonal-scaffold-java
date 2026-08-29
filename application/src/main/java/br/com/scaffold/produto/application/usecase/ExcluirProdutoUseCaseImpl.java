package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExcluirProdutoUseCaseImpl implements ExcluirProdutoUseCase {

    private final BuscarProdutoPorIdUseCase buscarProdutoPorId;
    private final ProdutoRepositoryPort port;

    public ExcluirProdutoUseCaseImpl(BuscarProdutoPorIdUseCase buscarProdutoPorId, ProdutoRepositoryPort port) {
        this.buscarProdutoPorId = buscarProdutoPorId;
        this.port = port;
    }

    @Override
    @Transactional
    public void executar(Long id) {
        buscarProdutoPorId.executar(id);
        port.delete(id);
    }
}
