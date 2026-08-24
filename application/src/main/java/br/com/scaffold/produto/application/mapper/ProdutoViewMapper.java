package br.com.scaffold.produto.application.mapper;

import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.domain.model.Produto;
import br.com.scaffold.shared.mapper.Mapper;
import org.springframework.stereotype.Component;

@Component
public class ProdutoViewMapper implements Mapper<Produto, ProdutoViewDTO> {
    @Override
    public ProdutoViewDTO map(Produto input) {
        return new ProdutoViewDTO(
                input.id(),
                input.nome(),
                input.custo(),
                input.margemPercentual(),
                input.valorVenda());
    }
}
