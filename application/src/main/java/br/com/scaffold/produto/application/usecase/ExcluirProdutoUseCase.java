package br.com.scaffold.produto.application.usecase;

@FunctionalInterface
public interface ExcluirProdutoUseCase {
    void executar(Long id);
}
