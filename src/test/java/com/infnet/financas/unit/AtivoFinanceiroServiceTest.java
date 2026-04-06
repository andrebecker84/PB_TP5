package com.infnet.financas.unit;

import com.infnet.financas.exception.RecursoNaoEncontradoException;
import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.repository.AtivoFinanceiroRepository;
import com.infnet.financas.service.AtivoFinanceiroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtivoFinanceiroServiceTest {

    @Mock
    private AtivoFinanceiroRepository repository;

    @InjectMocks
    private AtivoFinanceiroService service;

    private AtivoFinanceiro ativo;

    @BeforeEach
    void setUp() {
        ativo = AtivoFinanceiro.builder()
                .id(1L)
                .ticker("BTC")
                .nome("Bitcoin")
                .valorInvestido(new BigDecimal("50000.00"))
                .quantidade(0.5)
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .dataUltimaOperacao(LocalDate.now())
                .build();
    }

    @Test
    void shouldFindAssetById() {
        when(repository.findById(1L)).thenReturn(Optional.of(ativo));
        AtivoFinanceiro found = service.findById(1L);
        assertEquals("BTC", found.getTicker());
    }

    @Test
    void shouldThrowExceptionWhenAssetNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.findById(1L));
    }

    @Test
    void shouldSaveAssetSuccessfully() {
        ativo.setId(null);
        when(repository.existsByTicker("BTC")).thenReturn(false);
        when(repository.save(any(AtivoFinanceiro.class))).thenReturn(ativo);

        AtivoFinanceiro saved = service.save(ativo);
        assertNotNull(saved);
        verify(repository, times(1)).save(any(AtivoFinanceiro.class));
    }

    @Test
    void shouldUpdateAssetSuccessfully() {
        AtivoFinanceiro updatedDetails = AtivoFinanceiro.builder()
                .ticker("BTC")
                .nome("Bitcoin Updated")
                .valorInvestido(new BigDecimal("60000.00"))
                .quantidade(1.0)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(ativo));
        when(repository.save(any(AtivoFinanceiro.class))).thenReturn(ativo);

        AtivoFinanceiro result = service.update(1L, updatedDetails);
        assertEquals("Bitcoin Updated", result.getNome());
        verify(repository).save(ativo);
    }

    @Test
    void shouldThrowExceptionOnUpdateWhenNewTickerAlreadyExists() {
        AtivoFinanceiro updatedDetails = AtivoFinanceiro.builder()
                .ticker("ETH")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(ativo));
        when(repository.existsByTicker("ETH")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> service.update(1L, updatedDetails));
    }

    @Test
    void shouldThrowExceptionWhenTickerAlreadyExistsOnSave() {
        ativo.setId(null);
        when(repository.existsByTicker("BTC")).thenReturn(true);
        assertThrows(RecursoDuplicadoException.class, () -> service.save(ativo));
    }

    @Test
    void shouldDeleteAsset() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertDoesNotThrow(() -> service.delete(1L));
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionOnDeleteWhenNotFound() {
        when(repository.existsById(1L)).thenReturn(false);
        assertThrows(RecursoNaoEncontradoException.class, () -> service.delete(1L));
    }

    @Test
    void shouldBuildDashboardMetricsWithCorrectAggregates() {
        List<AtivoFinanceiro> ativos = new ArrayList<>(List.of(
                AtivoFinanceiro.builder()
                        .id(1L).ticker("BTC").nome("Bitcoin")
                        .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                        .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                        .valorInvestido(new BigDecimal("50000.00")).quantidade(0.5)
                        .dataUltimaOperacao(LocalDate.now()).build(),
                AtivoFinanceiro.builder()
                        .id(2L).ticker("PETR4").nome("Petrobras")
                        .tipo(AtivoFinanceiro.TipoAtivo.ACAO)
                        .categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                        .valorInvestido(new BigDecimal("10000.00")).quantidade(100.0)
                        .dataUltimaOperacao(LocalDate.now()).build()
        ));

        when(repository.findAll()).thenReturn(ativos);

        AtivoFinanceiroService.DashboardMetrics metrics = service.buildDashboardMetrics();

        assertEquals(new BigDecimal("60000.00"), metrics.totalInvestido(),
                "Total investido deve ser a soma dos valorInvestido de todos os ativos");
        assertEquals(2, metrics.quantidadeAtivos());
        assertEquals("CRIPTOMOEDA", metrics.tipoMaiorAlocacao(),
                "Criptomoeda tem maior alocação (R$ 50.000 vs R$ 10.000)");
        // patrimonioTotal = totalInvestido + saldoDinheiro fixo (R$ 12.450)
        assertEquals(new BigDecimal("72450.00"), metrics.patrimonioTotal());
    }

    @Test
    void shouldReturnDashboardMetricsWithEmptyPortfolio() {
        when(repository.findAll()).thenReturn(new ArrayList<>());

        AtivoFinanceiroService.DashboardMetrics metrics = service.buildDashboardMetrics();

        assertEquals(BigDecimal.ZERO, metrics.totalInvestido());
        assertEquals(0, metrics.quantidadeAtivos());
        assertEquals("NENHUM", metrics.tipoMaiorAlocacao());
    }

    @Test
    void shouldFindAllAssets() {
        when(repository.findAll()).thenReturn(List.of(ativo));

        List<AtivoFinanceiro> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("BTC", result.get(0).getTicker());
        verify(repository, times(1)).findAll();
    }

    @Test
    void shouldDeleteAllAssets() {
        doNothing().when(repository).deleteAll();

        assertDoesNotThrow(() -> service.deleteAll());
        verify(repository, times(1)).deleteAll();
    }
}
