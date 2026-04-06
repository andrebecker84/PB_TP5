package com.infnet.financas.unit;

import com.infnet.financas.model.AtivoFinanceiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AtivoFinanceiroTest {

    @Test
    void shouldCalculateAveragePriceCorrectly() {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .ticker("BTC")
                .valorInvestido(new BigDecimal("50000.00"))
                .quantidade(0.5)
                .build();

        assertEquals(new BigDecimal("100000.00"), ativo.getPrecoMedio());
    }

    @Test
    void shouldReturnZeroAveragePriceWhenQuantityIsZero() {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .ticker("CASH")
                .valorInvestido(new BigDecimal("1000.00"))
                .quantidade(0.0)
                .build();

        assertEquals(BigDecimal.ZERO, ativo.getPrecoMedio());
    }

    @Test
    void shouldReturnZeroAveragePriceWhenQuantityIsNull() {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .ticker("NULL_QTY")
                .valorInvestido(new BigDecimal("1000.00"))
                .quantidade(null)
                .build();

        assertEquals(BigDecimal.ZERO, ativo.getPrecoMedio());
    }

    @Test
    void shouldCreateWithNoArgsConstructorAndSetters() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        AtivoFinanceiro ativo = new AtivoFinanceiro();
        ativo.setId(10L);
        ativo.setNome("Petrobras");
        ativo.setTicker("PETR4");
        ativo.setTipo(AtivoFinanceiro.TipoAtivo.ACAO);
        ativo.setCategoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL);
        ativo.setValorInvestido(new BigDecimal("15000.00"));
        ativo.setQuantidade(500.0);
        ativo.setDataUltimaOperacao(date);

        assertEquals(10L, ativo.getId());
        assertEquals("Petrobras", ativo.getNome());
        assertEquals("PETR4", ativo.getTicker());
        assertEquals(AtivoFinanceiro.TipoAtivo.ACAO, ativo.getTipo());
        assertEquals(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL, ativo.getCategoria());
        assertEquals(new BigDecimal("15000.00"), ativo.getValorInvestido());
        assertEquals(500.0, ativo.getQuantidade());
        assertEquals(date, ativo.getDataUltimaOperacao());
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        AtivoFinanceiro ativo = new AtivoFinanceiro(
                2L, "Bitcoin", "BTC",
                AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA,
                AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA,
                new BigDecimal("50000.00"), 0.5, date);

        assertEquals(2L, ativo.getId());
        assertEquals("Bitcoin", ativo.getNome());
        assertEquals("BTC", ativo.getTicker());
        assertEquals(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA, ativo.getTipo());
        assertEquals(new BigDecimal("50000.00"), ativo.getValorInvestido());
        assertEquals(date, ativo.getDataUltimaOperacao());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        AtivoFinanceiro a1 = AtivoFinanceiro.builder()
                .id(1L).nome("Bitcoin").ticker("BTC")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("50000.00")).quantidade(0.5)
                .dataUltimaOperacao(LocalDate.of(2024, 1, 1))
                .build();
        AtivoFinanceiro a2 = AtivoFinanceiro.builder()
                .id(1L).nome("Bitcoin").ticker("BTC")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("50000.00")).quantidade(0.5)
                .dataUltimaOperacao(LocalDate.of(2024, 1, 1))
                .build();
        AtivoFinanceiro a3 = AtivoFinanceiro.builder()
                .id(2L).nome("Ethereum").ticker("ETH")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("10000.00")).quantidade(1.0)
                .dataUltimaOperacao(LocalDate.of(2024, 1, 1))
                .build();

        assertEquals(a1, a2);
        assertNotEquals(a1, a3);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1.hashCode(), a3.hashCode());
        assertEquals(a1, a1);
        assertNotEquals(null, a1);
        assertNotEquals("not an AtivoFinanceiro", a1);
    }

    @Test
    void shouldImplementToString() {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .id(1L).nome("Bitcoin").ticker("BTC")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("50000.00")).quantidade(0.5)
                .build();

        String str = ativo.toString();
        assertNotNull(str);
        assertTrue(str.contains("BTC"));
        assertTrue(str.contains("Bitcoin"));
        assertTrue(str.contains("CRIPTOMOEDA"));
    }
}
