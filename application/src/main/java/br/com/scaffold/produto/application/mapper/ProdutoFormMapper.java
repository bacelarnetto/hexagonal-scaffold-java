package br.com.scaffold.produto.application.mapper;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.shared.mapper.Mapper;
import org.springframework.stereotype.Component;

@Component
public class ProdutoFormMapper implements Mapper<ProdutoInsertFormDTO, Produto> {
    @Override
    public Produto map(ProdutoInsertFormDTO input) {
        return new Produto(input.nome(), input.custo(), input.margemPercentual());
    }
}
