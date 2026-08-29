package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.mapper.ProdutoViewMapper;
import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarProdutosUseCaseImpl implements ListarProdutosUseCase {

    private final ProdutoRepositoryPort port;
    private final ProdutoViewMapper viewMapper;

    public ListarProdutosUseCaseImpl(ProdutoRepositoryPort port, ProdutoViewMapper viewMapper) {
        this.port = port;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoViewDTO> executar() {
        return port.findAll().stream().map(viewMapper::map).toList();
    }
}
