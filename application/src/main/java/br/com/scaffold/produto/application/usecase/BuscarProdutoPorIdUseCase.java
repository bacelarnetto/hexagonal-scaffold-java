package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.domain.model.Produto;

/**
 * Devolve o model de domain (nao o DTO) -- reusado por outros use cases que precisam confirmar
 * que o produto existe antes de agir (ver BuscarProdutoViewPorIdUseCase, ExcluirProdutoUseCase).
 */
@FunctionalInterface
public interface BuscarProdutoPorIdUseCase {
    Produto executar(Long id);
}
