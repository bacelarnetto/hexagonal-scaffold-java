package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoViewDTO;

@FunctionalInterface
public interface BuscarProdutoViewPorIdUseCase {
    ProdutoViewDTO executar(Long id);
}
