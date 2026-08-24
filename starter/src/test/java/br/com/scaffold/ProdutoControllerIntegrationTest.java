package br.com.scaffold;

import br.com.scaffold.produto.infrastructure.repository.ProdutoJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova que os 4 modulos Maven (domain, infrastructure, application, starter) realmente
 * compilam e se conectam num Spring context de verdade -- H2 + Flyway, sem mocks.
 * O projeto genetic (referencia) nao pode ser validado assim aqui porque depende de libs
 * privadas que nao estao disponiveis fora da rede da empresa original.
 *
 * De proposito SEM @Transactional na classe de teste: PCI3 precisa que a transacao do
 * proprio cadastrarEmLote() seja a fronteira real (commit ou rollback de verdade), nao uma
 * transacao de teste que so reverte no final do metodo -- isso esconderia a diferenca entre
 * "linha commitada" e "linha revertida". A limpeza entre testes fica por conta do @BeforeEach
 * abaixo em vez de rollback automatico do Spring TestContext.
 */
@SpringBootTest(classes = ScaffoldApplication.class)
@AutoConfigureMockMvc
class ProdutoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProdutoJpaRepository produtoJpaRepository;

    @BeforeEach
    void limparBanco() {
        produtoJpaRepository.deleteAll();
    }

    // PCI1: POST calcula valorVenda via domain/service e persiste via H2+Flyway; GET devolve o mesmo produto
    @Test
    void pci1_cadastraProdutoEBuscaPorId() throws Exception {
        Map<String, String> body = Map.of("nome", "Bolo de chocolate", "custo", "40.00", "margemPercentual", "25");

        MvcResult result = mockMvc.perform(post("/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorVenda").value(53.33))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        mockMvc.perform(get("/produto/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bolo de chocolate"))
                .andExpect(jsonPath("$.valorVenda").value(53.33));
    }

    // PCI2: margem invalida -> 400 tratado pelo GlobalExceptionHandler, nao 500
    @Test
    void pci2_margemInvalidaRetorna400() throws Exception {
        Map<String, String> body = Map.of("nome", "Torta", "custo", "10.00", "margemPercentual", "100");

        mockMvc.perform(post("/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // PCI3: prova o rollback de verdade. cadastrarEmLote salva 2 produtos validos e so estoura
    // RegraDeNegocioException no 3o; como os 3 estao na mesma transacao (@Transactional em
    // ProdutoService.cadastrarEmLote), o rollback desfaz os 2 primeiros tambem -- por isso o
    // GET /produto depois do lote falho tem que devolver lista vazia, nao 2 produtos. Isso so
    // e visivel com banco real (H2 aqui); um teste com mock nao provaria nada sobre transacao.
    @Test
    void pci3_loteComItemInvalidoNoFinalNaoDeixaNenhumProdutoSalvo() throws Exception {
        List<Map<String, String>> lote = List.of(
                Map.of("nome", "Bolo", "custo", "40.00", "margemPercentual", "25"),
                Map.of("nome", "Brigadeiro", "custo", "2.00", "margemPercentual", "50"),
                Map.of("nome", "Torta", "custo", "10.00", "margemPercentual", "100"));

        mockMvc.perform(post("/produto/lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lote)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/produto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
