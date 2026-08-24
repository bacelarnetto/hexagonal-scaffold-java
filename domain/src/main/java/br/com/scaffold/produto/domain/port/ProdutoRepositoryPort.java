package br.com.scaffold.produto.domain.port;

import br.com.scaffold.produto.domain.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepositoryPort {
    List<Produto> findAll();

    Optional<Produto> findById(Long id);

    Produto save(Produto produto);

    void delete(Long id);
}
