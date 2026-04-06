package com.infnet.financas.unit;

import com.infnet.financas.model.AtivoFinanceiro;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class AtivoFinanceiroPropriedadeTest {

    @Property
    void precoMedioDeveSerValorInvestidoDivididoPorQuantidade(
            @ForAll @DoubleRange(min = 1.0, max = 1000000.0) double valorInvestido,
            @ForAll @DoubleRange(min = 0.1, max = 1000.0) double quantidade) {
        BigDecimal val = BigDecimal.valueOf(valorInvestido).setScale(2, RoundingMode.HALF_UP);
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .valorInvestido(val)
                .quantidade(quantidade)
                .build();

        BigDecimal esperado = val.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        assertEquals(esperado, ativo.getPrecoMedio());
    }

    @Property
    void precoMedioSemprePositivoQuandoEntradasPositivas(
            @ForAll @Positive double valorInvestido,
            @ForAll @DoubleRange(min = 0.1) double quantidade) {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .valorInvestido(BigDecimal.valueOf(valorInvestido))
                .quantidade(quantidade)
                .build();

        assertTrue(ativo.getPrecoMedio().compareTo(BigDecimal.ZERO) >= 0);
    }

    /**
     * Propriedade: preço médio nunca negativo com BigDecimal arbitrário positivo.
     * Cobre a mesma invariante usando valores monetários reais (BigDecimal)
     * em vez de double, eliminando problemas de precisão de ponto flutuante.
     */
    @Property
    void precoMedioNaoNegativoComBigDecimalArbitrario(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal valorInvestido,
            @ForAll @DoubleRange(min = 0.01, max = 10000.0) double quantidade) {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .valorInvestido(valorInvestido)
                .quantidade(quantidade)
                .build();

        assertTrue(ativo.getPrecoMedio().compareTo(BigDecimal.ZERO) >= 0,
                "getPrecoMedio() não pode retornar valor negativo com entradas positivas");
    }

    /**
     * Propriedade: quantidade nula ou zero sempre retorna ZERO como preço médio.
     * Fail-safe contra estados provisórios durante o binding do formulário.
     */
    @Property
    void precoMedioDeveRetornarZeroQuandoQuantidadeEhZero(
            @ForAll @BigRange(min = "0.01", max = "999999.99") BigDecimal valorInvestido) {
        AtivoFinanceiro ativo = AtivoFinanceiro.builder()
                .valorInvestido(valorInvestido)
                .quantidade(0.0)
                .build();

        assertEquals(BigDecimal.ZERO, ativo.getPrecoMedio(),
                "Quantidade zero deve sempre retornar ZERO, independente do valorInvestido");
    }
}
