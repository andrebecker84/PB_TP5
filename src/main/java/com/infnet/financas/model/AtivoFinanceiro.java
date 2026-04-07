package com.infnet.financas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Entidade de domínio que representa um ativo financeiro no portfólio do
 * usuário.
 * Aplica Fail-Fast via Bean Validation e encapsula a regra de preço médio.
 */
@Entity
@Table(name = "ativo_financeiro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtivoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do ativo é obrigatório")
    private String nome;

    @NotBlank(message = "O código/ticker é obrigatório")
    @Column
    private String ticker;

    @NotNull(message = "O tipo de ativo é obrigatório")
    @Enumerated(EnumType.STRING)
    private TipoAtivo tipo;

    @NotNull(message = "A categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    private CategoriaAtivo categoria;

    @NotNull(message = "O valor investido é obrigatório")
    @PositiveOrZero(message = "O valor deve ser zero ou positivo")
    private BigDecimal valorInvestido;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser positiva")
    private Double quantidade;

    @NotNull(message = "A data da última operação é obrigatória")
    @PastOrPresent(message = "A data não pode ser no futuro")
    private LocalDate dataUltimaOperacao;

    /**
     * Atualiza os campos editáveis a partir de outro ativo, encapsulando a
     * lógica de atualização no próprio modelo (SRP) e substituindo o bloco de
     * setters no service por uma única chamada semântica (CQS: comando sem
     * retorno, sem efeitos colaterais externos).
     */
    public void updateFrom(AtivoFinanceiro detalhes) {
        this.nome = detalhes.getNome();
        this.ticker = detalhes.getTicker();
        this.tipo = detalhes.getTipo();
        this.categoria = detalhes.getCategoria();
        this.valorInvestido = detalhes.getValorInvestido();
        this.quantidade = detalhes.getQuantidade();
        this.dataUltimaOperacao = detalhes.getDataUltimaOperacao();
    }

    /**
     * Retorna o preço médio por unidade do ativo.
     * Fail-fast: quantidade nula ou zero retorna ZERO sem lançar exceção,
     * pois o estado pode ser provisório durante a construção via formulário.
     */
    public BigDecimal getPrecoMedio() {
        if (quantidade == null || quantidade <= 0.0)
            return BigDecimal.ZERO;
        return valorInvestido.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    public enum TipoAtivo {
        ACAO, FII, CRIPTOMOEDA, RENDA_FIXA, ETF, IMOVEL, PRECATORIO, OURO, TERRENO_AGRO, EQUIPAMENTO_PESADO
    }

    public enum CategoriaAtivo {
        RENDA_VARIAVEL, RENDA_FIXA, ATIVO_REAL, CRIPTODIVISA
    }
}
