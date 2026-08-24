package br.com.scaffold.produto.application.controller;

import br.com.scaffold.produto.application.dto.ProdutoInsertFormDTO;
import br.com.scaffold.produto.application.dto.ProdutoViewDTO;
import br.com.scaffold.produto.application.service.ProdutoService;
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

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoViewDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoViewDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarViewPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProdutoViewDTO> cadastrar(@Valid @RequestBody ProdutoInsertFormDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(dto));
    }

    // Exemplo de rollback via @Transactional -- ver comentario em ProdutoService.cadastrarEmLote()
    @PostMapping("/lote")
    public ResponseEntity<List<ProdutoViewDTO>> cadastrarEmLote(@Valid @RequestBody List<@Valid ProdutoInsertFormDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrarEmLote(dtos));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
