package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoViewDTO;

import java.util.List;

@FunctionalInterface
public interface ListarProdutosUseCase {
    List<ProdutoViewDTO> executar();
}
