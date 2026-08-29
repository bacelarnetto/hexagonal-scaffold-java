package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.mapper.ProdutoFormMapper;
import br.com.scaffold.produto.application.mapper.ProdutoViewMapper;
import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.produto.domain.port.ProdutoRepositoryPort;
import br.com.scaffold.produto.domain.service.ProdutoPrecoLogic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @Service + @Transactional aqui -- e so aqui. Toda a regra de calculo de preco mora em
 * ProdutoPrecoLogic (domain/service), que nao sabe que Spring existe. Este use case so orquestra:
 * busca porta, chama a regra pura, persiste o resultado.
 */
@Service
public class CadastrarProdutoUseCaseImpl implements CadastrarProdutoUseCase {

    private final ProdutoRepositoryPort port;
    private final ProdutoFormMapper formMapper;
    private final ProdutoViewMapper viewMapper;
    private final ProdutoPrecoLogic precoLogic = new ProdutoPrecoLogic();

    public CadastrarProdutoUseCaseImpl(ProdutoRepositoryPort port, ProdutoFormMapper formMapper, ProdutoViewMapper viewMapper) {
        this.port = port;
        this.formMapper = formMapper;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional
    public ProdutoViewDTO executar(ProdutoInsertFormDTO dto) {
        Produto produto = formMapper.map(dto);
        BigDecimal valorVenda = precoLogic.calcularValorVenda(produto.custo(), produto.margemPercentual());
        Produto salvo = port.save(produto.comValorVenda(valorVenda));
        return viewMapper.map(salvo);
    }
}
