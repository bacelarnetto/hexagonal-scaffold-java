package br.com.scaffold.produto.application.usecase;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Exemplo de rollback via @Transactional: cadastra vários produtos numa única transação,
 * reaproveitando CadastrarProdutoUseCase item a item -- que só funciona sem cair na armadilha de
 * self-invocation (ver CLAUDE.md) porque agora são dois beans Spring diferentes, não dois métodos
 * da mesma classe: a chamada abaixo passa pelo proxy de CadastrarProdutoUseCaseImpl normalmente.
 *
 * ProdutoPrecoLogic (domain, sem Spring) só sabe lançar RegraDeNegocioException quando a margem de
 * um item é inválida -- não sabe nada sobre transação. É o @Transactional aqui, em application/,
 * que traduz essa exceção unchecked em rollback: se o item N falhar, os itens 1..N-1 já enviados a
 * port.save() (dentro da chamada a CadastrarProdutoUseCase) também são desfeitos, porque ainda
 * estão na mesma transação JDBC quando ela é revertida. O domínio não precisa saber que isso
 * existe -- ele só lança a exceção de sempre.
 */
@Service
public class CadastrarProdutoEmLoteUseCaseImpl implements CadastrarProdutoEmLoteUseCase {

    private final CadastrarProdutoUseCase cadastrarProduto;

    public CadastrarProdutoEmLoteUseCaseImpl(CadastrarProdutoUseCase cadastrarProduto) {
        this.cadastrarProduto = cadastrarProduto;
    }

    @Override
    @Transactional
    public List<ProdutoViewDTO> executar(List<ProdutoInsertFormDTO> dtos) {
        return dtos.stream().map(cadastrarProduto::executar).toList();
    }
}
