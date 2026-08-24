package br.com.scaffold.produto.application.service;

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
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @Service + @Transactional aqui -- e so aqui. Toda a regra de calculo de preco
 * mora em ProdutoPrecoLogic (domain/service), que nao sabe que Spring existe.
 * Este service so orquestra: busca porta, chama a regra pura, persiste o resultado.
 */
@Service
public class ProdutoService {

    private final ProdutoRepositoryPort port;
    private final ProdutoFormMapper formMapper;
    private final ProdutoViewMapper viewMapper;
    private final ProdutoPrecoLogic precoLogic = new ProdutoPrecoLogic();

    public ProdutoService(ProdutoRepositoryPort port, ProdutoFormMapper formMapper, ProdutoViewMapper viewMapper) {
        this.port = port;
        this.formMapper = formMapper;
        this.viewMapper = viewMapper;
    }

    @Transactional(readOnly = true)
    public List<ProdutoViewDTO> listar() {
        return port.findAll().stream().map(viewMapper::map).toList();
    }

    public Produto buscarPorId(Long id) {
        return port.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com o ID " + id));
    }

    public ProdutoViewDTO buscarViewPorId(Long id) {
        return viewMapper.map(buscarPorId(id));
    }

    @Transactional
    public ProdutoViewDTO cadastrar(ProdutoInsertFormDTO dto) {
        Produto produto = formMapper.map(dto);
        BigDecimal valorVenda = precoLogic.calcularValorVenda(produto.custo(), produto.margemPercentual());
        Produto salvo = port.save(produto.comValorVenda(valorVenda));
        return viewMapper.map(salvo);
    }

    @Transactional
    public void excluir(Long id) {
        buscarPorId(id);
        port.delete(id);
    }

    /**
     * Exemplo de rollback via @Transactional: cadastra varios produtos numa unica transacao.
     * ProdutoPrecoLogic (domain, sem Spring) so sabe lancar RegraDeNegocioException quando a
     * margem de um item e invalida -- nao sabe nada sobre transacao. E o @Transactional aqui,
     * em application/, que traduz essa excecao unchecked em rollback: se o item N falhar, os
     * itens 1..N-1 ja enviados a port.save() dentro desta mesma chamada tambem sao desfeitos,
     * porque ainda estao na mesma transacao JDBC quando ela e revertida. O dominio nao precisa
     * saber que isso existe -- ele so lanca a excecao de sempre.
     */
    @Transactional
    public List<ProdutoViewDTO> cadastrarEmLote(List<ProdutoInsertFormDTO> dtos) {
        return dtos.stream().map(this::cadastrarItemDoLote).toList();
    }

    private ProdutoViewDTO cadastrarItemDoLote(ProdutoInsertFormDTO dto) {
        Produto produto = formMapper.map(dto);
        BigDecimal valorVenda = precoLogic.calcularValorVenda(produto.custo(), produto.margemPercentual());
        Produto salvo = port.save(produto.comValorVenda(valorVenda));
        return viewMapper.map(salvo);
    }
}
