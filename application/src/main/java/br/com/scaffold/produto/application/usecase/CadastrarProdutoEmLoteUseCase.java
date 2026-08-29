package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;

import java.util.List;

@FunctionalInterface
public interface CadastrarProdutoEmLoteUseCase {
    List<ProdutoViewDTO> executar(List<ProdutoInsertFormDTO> dtos);
}
