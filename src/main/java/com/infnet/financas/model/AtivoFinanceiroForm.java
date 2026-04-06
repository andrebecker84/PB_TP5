package com.infnet.financas.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de formulário para criação e edição de ativos financeiros.
 *
 * Justificativa: isola a entidade JPA {@link AtivoFinanceiro} do binding direto
 * com o formulário HTTP, eliminando o risco de mass assignment. O campo {@code id}
 * é incluído apenas para que o template Thymeleaf distinga criação de edição e
 * construa corretamente a action do formulário — nunca é usado para localizar ou
 * persistir dados (o controller usa o {@code @PathVariable} do endpoint para isso).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtivoFinanceiroForm {

    private Long id;

    @NotBlank(message = "O nome do ativo é obrigatório")
    private String nome;

    @NotBlank(message = "O código/ticker é obrigatório")
    private String ticker;

    @NotNull(message = "O tipo de ativo é obrigatório")
    private AtivoFinanceiro.TipoAtivo tipo;

    @NotNull(message = "A categoria é obrigatória")
    private AtivoFinanceiro.CategoriaAtivo categoria;

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
     * Converte o DTO em entidade JPA para persistência.
     * O {@code id} não é transferido — é responsabilidade do service/repositório.
     */
    public AtivoFinanceiro toEntity() {
        return AtivoFinanceiro.builder()
                .nome(nome)
                .ticker(ticker)
                .tipo(tipo)
                .categoria(categoria)
                .valorInvestido(valorInvestido)
                .quantidade(quantidade)
                .dataUltimaOperacao(dataUltimaOperacao)
                .build();
    }

    /**
     * Cria um DTO a partir de uma entidade existente (usado no formulário de edição).
     * O {@code id} é incluído somente para que o template Thymeleaf construa a
     * action correta do formulário — não é usado para localizar nem persistir dados.
     */
    public static AtivoFinanceiroForm from(AtivoFinanceiro ativo) {
        return new AtivoFinanceiroForm(
                ativo.getId(),
                ativo.getNome(),
                ativo.getTicker(),
                ativo.getTipo(),
                ativo.getCategoria(),
                ativo.getValorInvestido(),
                ativo.getQuantidade(),
                ativo.getDataUltimaOperacao()
        );
    }
}
