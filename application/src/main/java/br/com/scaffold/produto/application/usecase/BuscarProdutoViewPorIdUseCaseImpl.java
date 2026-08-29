package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.mapper.ProdutoViewMapper;
import org.springframework.stereotype.Service;

@Service
public class BuscarProdutoViewPorIdUseCaseImpl implements BuscarProdutoViewPorIdUseCase {

    private final BuscarProdutoPorIdUseCase buscarProdutoPorId;
    private final ProdutoViewMapper viewMapper;

    public BuscarProdutoViewPorIdUseCaseImpl(BuscarProdutoPorIdUseCase buscarProdutoPorId, ProdutoViewMapper viewMapper) {
        this.buscarProdutoPorId = buscarProdutoPorId;
        this.viewMapper = viewMapper;
    }

    @Override
    public ProdutoViewDTO executar(Long id) {
        return viewMapper.map(buscarProdutoPorId.executar(id));
    }
}
