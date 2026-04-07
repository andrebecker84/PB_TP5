package com.infnet.financas.unit;

import com.infnet.financas.controller.AtivoController;
import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.service.AtivoFinanceiroService;
import com.infnet.financas.service.AtivoFinanceiroService.DashboardMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AtivoController.class)
class AtivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AtivoFinanceiroService ativoService;

    /** Cria um DashboardMetrics vazio para uso em mocks de teste. */
    private DashboardMetrics buildEmptyMetrics() {
        return new DashboardMetrics(
                Collections.emptyList(),
                BigDecimal.ZERO,
                new BigDecimal("12450.00"),
                new BigDecimal("12450.00"),
                new BigDecimal("4820.00"),
                Map.of(),
                0,
                "NENHUM");
    }

    @Test
    void dashboardShouldReturnViewWithData() throws Exception {
        when(ativoService.buildDashboardMetrics()).thenReturn(buildEmptyMetrics());

        mockMvc.perform(get("/ativos/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("patrimonioTotal"))
                .andExpect(model().attributeExists("alocacaoPorTipo"));
    }

    @Test
    void showEditFormShouldReturnForm() throws Exception {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder().id(1L).ticker("TEST").build();
        when(ativoService.findById(1L)).thenReturn(ativo);

        mockMvc.perform(get("/ativos/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"))
                .andExpect(model().attributeExists("ativo"));
    }

    @Test
    void saveAssetShouldReturnFormOnError() throws Exception {
        mockMvc.perform(post("/ativos")
                .with(csrf())
                .param("ticker", "")
                .param("nome", "Test"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"));
    }

    @Test
    void saveAssetShouldReturnFormOnDuplicateTicker() throws Exception {
        doThrow(new RecursoDuplicadoException("Already exists")).when(ativoService).save(any());

        mockMvc.perform(post("/ativos")
                .with(csrf())
                .param("ticker", "TEST")
                .param("nome", "Test Asset")
                .param("tipo", "ACAO")
                .param("categoria", "RENDA_VARIAVEL")
                .param("valorInvestido", "100.00")
                .param("quantidade", "1")
                .param("dataUltimaOperacao", "2024-02-18"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void updateAssetShouldRedirectOnSuccess() throws Exception {
        mockMvc.perform(post("/ativos/1")
                .with(csrf())
                .param("ticker", "TEST")
                .param("nome", "Test Asset")
                .param("tipo", "ACAO")
                .param("categoria", "RENDA_VARIAVEL")
                .param("valorInvestido", "100.00")
                .param("quantidade", "1")
                .param("dataUltimaOperacao", "2024-02-18"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ativos"));
    }

    @Test
    void updateAssetShouldReturnFormOnDuplicateTicker() throws Exception {
        doThrow(new RecursoDuplicadoException("Already exists")).when(ativoService).update(eq(1L), any());

        mockMvc.perform(post("/ativos/1")
                .with(csrf())
                .param("ticker", "TEST")
                .param("nome", "Test Asset")
                .param("tipo", "ACAO")
                .param("categoria", "RENDA_VARIAVEL")
                .param("valorInvestido", "100.00")
                .param("quantidade", "1")
                .param("dataUltimaOperacao", "2024-02-18"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void deleteAssetShouldRedirectOnSuccess() throws Exception {
        mockMvc.perform(get("/ativos/excluir/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ativos"));
    }

    @Test
    void listAtivosPageShouldReturnListView() throws Exception {
        when(ativoService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/ativos"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-lista"))
                .andExpect(model().attributeExists("ativos"));
    }

    @Test
    void showCreateFormShouldReturnFormWithEmptyAtivo() throws Exception {
        mockMvc.perform(get("/ativos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"))
                .andExpect(model().attributeExists("ativo"))
                .andExpect(model().attributeExists("tipos"))
                .andExpect(model().attributeExists("categorias"));
    }

    @Test
    void updateAssetShouldReturnFormOnValidationError() throws Exception {
        mockMvc.perform(post("/ativos/1")
                .with(csrf())
                .param("ticker", "")
                .param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"));
    }

    /**
     * Testes parametrizados: múltiplas combinações de campos inválidos no POST.
     * Cada linha do @CsvSource representa um cenário de entrada malformada ou ausente.
     */
    @ParameterizedTest(name = "[{index}] ticker=''{0}'' nome=''{1}'' → formulário retornado")
    @CsvSource({
        "'', 'Ativo Valido'",
        "'   ', 'Ativo Valido'",
        "'TICK1', ''",
        "'TICK2', '   '"
    })
    void saveAssetShouldReturnFormForAnyInvalidFieldCombination(String ticker, String nome) throws Exception {
        mockMvc.perform(post("/ativos")
                .with(csrf())
                .param("ticker", ticker)
                .param("nome", nome))
                .andExpect(status().isOk())
                .andExpect(view().name("ativo-form"));
    }

    /**
     * Testes parametrizados: múltiplos IDs inválidos para edição.
     * O controller deve delegar ao service, que lança RecursoNaoEncontradoException.
     * O handler global intercepta e retorna a view de erro.
     */
    @ParameterizedTest(name = "[{index}] GET /ativos/editar/{0} com ativo inexistente → error view")
    @CsvSource({"999", "0", "99999"})
    void editFormShouldDelegateNotFoundToGlobalHandler(Long id) throws Exception {
        when(ativoService.findById(id))
                .thenThrow(new com.infnet.financas.exception.RecursoNaoEncontradoException(
                        "Ativo não encontrado com ID: " + id));

        mockMvc.perform(get("/ativos/editar/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}
