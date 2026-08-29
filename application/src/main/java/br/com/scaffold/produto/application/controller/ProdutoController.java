package br.com.scaffold.produto.application.controller;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.usecase.BuscarProdutoViewPorIdUseCase;
import br.com.scaffold.produto.application.usecase.CadastrarProdutoEmLoteUseCase;
import br.com.scaffold.produto.application.usecase.CadastrarProdutoUseCase;
import br.com.scaffold.produto.application.usecase.ExcluirProdutoUseCase;
import br.com.scaffold.produto.application.usecase.ListarProdutosUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/produto")
public class ProdutoController {

    private final ListarProdutosUseCase listarProdutos;
    private final BuscarProdutoViewPorIdUseCase buscarProdutoViewPorId;
    private final CadastrarProdutoUseCase cadastrarProduto;
    private final CadastrarProdutoEmLoteUseCase cadastrarProdutoEmLote;
    private final ExcluirProdutoUseCase excluirProduto;

    public ProdutoController(
            ListarProdutosUseCase listarProdutos,
            BuscarProdutoViewPorIdUseCase buscarProdutoViewPorId,
            CadastrarProdutoUseCase cadastrarProduto,
            CadastrarProdutoEmLoteUseCase cadastrarProdutoEmLote,
            ExcluirProdutoUseCase excluirProduto) {
        this.listarProdutos = listarProdutos;
        this.buscarProdutoViewPorId = buscarProdutoViewPorId;
        this.cadastrarProduto = cadastrarProduto;
        this.cadastrarProdutoEmLote = cadastrarProdutoEmLote;
        this.excluirProduto = excluirProduto;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoViewDTO>> listar() {
        return ResponseEntity.ok(listarProdutos.executar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoViewDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(buscarProdutoViewPorId.executar(id));
    }

    @PostMapping
    public ResponseEntity<ProdutoViewDTO> cadastrar(@Valid @RequestBody ProdutoInsertFormDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastrarProduto.executar(dto));
    }

    // Exemplo de rollback via @Transactional -- ver comentario em CadastrarProdutoEmLoteUseCaseImpl
    @PostMapping("/lote")
    public ResponseEntity<List<ProdutoViewDTO>> cadastrarEmLote(@Valid @RequestBody List<@Valid ProdutoInsertFormDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastrarProdutoEmLote.executar(dtos));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        excluirProduto.executar(id);
    }
}
