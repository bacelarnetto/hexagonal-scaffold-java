package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;

@FunctionalInterface
public interface CadastrarProdutoUseCase {
    ProdutoViewDTO executar(ProdutoInsertFormDTO dto);
}
