package br.com.scaffold.produto.infrastructure.adapter;

import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import br.com.scaffold.produto.infrastructure.entity.ProdutoEntity;
import br.com.scaffold.produto.infrastructure.repository.ProdutoJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mapeamento explicito entity <-> domain. De proposito NAO usa BeanUtils.copyProperties():
 * fica claro no diff quais campos existem dos dois lados, e sobrevive a divergencia de nomes
 * sem falhar silenciosamente em runtime.
 */
@Repository
public class ProdutoRepositoryAdapter implements ProdutoRepositoryPort {

    private final ProdutoJpaRepository jpaRepository;

    public ProdutoRepositoryAdapter(ProdutoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Produto> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Produto> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Produto save(Produto produto) {
        return toDomain(jpaRepository.save(toEntity(produto)));
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    private Produto toDomain(ProdutoEntity entity) {
        return new Produto(
                entity.getId(),
                entity.getNome(),
                entity.getCusto(),
                entity.getMargemPercentual(),
                entity.getValorVenda());
    }

    private ProdutoEntity toEntity(Produto produto) {
        return ProdutoEntity.builder()
                .id(produto.id())
                .nome(produto.nome())
                .custo(produto.custo())
                .margemPercentual(produto.margemPercentual())
                .valorVenda(produto.valorVenda())
                .build();
    }
}
