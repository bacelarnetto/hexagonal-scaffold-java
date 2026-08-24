package br.com.scaffold.produto.infrastructure.repository;

import br.com.scaffold.produto.infrastructure.entity.ProdutoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoEntity, Long> {
}
