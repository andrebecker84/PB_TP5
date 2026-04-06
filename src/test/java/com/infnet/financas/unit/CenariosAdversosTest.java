package com.infnet.financas.unit;

import com.infnet.financas.exception.RecursoNaoEncontradoException;
import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.repository.AtivoFinanceiroRepository;
import com.infnet.financas.service.AtivoFinanceiroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de robustez para cenários adversos: falhas de repositório,
 * latência simulada, entradas maliciosas e volume elevado de requisições.
 * Valida os princípios de fail-fast e fail-gracefully do serviço.
 */
@ExtendWith(MockitoExtension.class)
class CenariosAdversosTest {

    @Mock
    private AtivoFinanceiroRepository repository;

    @InjectMocks
    private AtivoFinanceiroService service;

    /**
     * Falha de repositório deve propagar a exceção sem suprimi-la.
     * O handler global (@ControllerAdvice) captura e retorna resposta segura ao usuário.
     */
    @Test
    void shouldPropagateRepositoryExceptionWithoutSuppressing() {
        when(repository.findAll()).thenThrow(new RuntimeException("Falha de conexão com o banco de dados"));

        RuntimeException ex = assertThrows(RuntimeException.class, service::findAll);
        assertTrue(ex.getMessage().contains("Falha de conexão"),
                "A mensagem original deve ser preservada para logging interno");
    }

    /**
     * Simula latência de rede no repositório (100ms).
     * Garante que a operação completa dentro de um limite razoável — sem bloqueio indefinido.
     * Em produção, o timeout real seria configurado no connection pool (HikariCP).
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldCompleteOperationWithinReasonableTimeUnderLatency() {
        when(repository.existsById(anyLong())).thenAnswer(inv -> {
            Thread.sleep(100); // Simula latência de rede de 100ms
            return false;
        });

        assertThrows(RecursoNaoEncontradoException.class, () -> service.delete(1L),
                "Deve lançar exceção de domínio, não congelar a thread");
    }

    /**
     * Tentativa de SQL injection no ticker.
     * JPA/Hibernate usa prepared statements — a entrada é tratada como string literal.
     * O serviço não deve alterar comportamento por conteúdo malicioso.
     */
    @Test
    void shouldHandleSqlInjectionAttemptInTickerSafely() {
        String maliciousTicker = "'; DROP TABLE ativo_financeiro; --";
        AtivoFinanceiro ativo = buildAtivo(maliciousTicker);

        when(repository.existsByTicker(anyString())).thenReturn(false);
        when(repository.save(any(AtivoFinanceiro.class))).thenAnswer(inv -> {
            AtivoFinanceiro a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AtivoFinanceiro saved = service.save(ativo);

        assertEquals(maliciousTicker, saved.getTicker(),
                "Ticker com tentativa de SQL injection deve ser persistido como string literal");
        verify(repository).existsByTicker(maliciousTicker);
        verify(repository, never()).deleteAll();
    }

    /**
     * Entrada com payload XSS no nome do ativo.
     * O Thymeleaf faz escaping automático na renderização (th:text).
     * O serviço deve aceitar e persistir a string sem alteração.
     */
    @Test
    void shouldHandleXssPayloadInNameWithoutAlteringValue() {
        String xssNome = "<script>alert('xss')</script>";
        AtivoFinanceiro ativo = buildAtivo("XSS01");
        ativo.setNome(xssNome);

        when(repository.existsByTicker("XSS01")).thenReturn(false);
        when(repository.save(any(AtivoFinanceiro.class))).thenAnswer(inv -> {
            AtivoFinanceiro a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AtivoFinanceiro saved = service.save(ativo);

        assertEquals(xssNome, saved.getNome(),
                "Payload XSS no nome deve ser salvo sem modificação — escaping ocorre na camada de view");
    }

    /**
     * Ticker com string extremamente longa (boundary testing).
     * O serviço não deve lançar exceção por comprimento — a constraint de banco rejeitaria via DDL.
     */
    @Test
    void shouldHandleExtremelyLongTickerInput() {
        String tickerLongo = "A".repeat(500);
        AtivoFinanceiro ativo = buildAtivo(tickerLongo);

        when(repository.existsByTicker(tickerLongo)).thenReturn(false);
        when(repository.save(any(AtivoFinanceiro.class))).thenAnswer(inv -> {
            AtivoFinanceiro a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        assertDoesNotThrow(() -> service.save(ativo),
                "O serviço não deve lançar exceção por comprimento — responsabilidade do banco");
    }

    /**
     * Simula volume elevado de requisições sequenciais.
     * Verifica que o serviço mantém comportamento consistente sem degradação de estado interno.
     */
    @Test
    void shouldMaintainConsistentBehaviorUnderHighRequestVolume() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        long falhasContabilizadas = IntStream.rangeClosed(1, 50)
                .mapToLong(i -> {
                    try {
                        service.findById((long) i);
                        return 0L;
                    } catch (RecursoNaoEncontradoException e) {
                        return 1L;
                    }
                })
                .sum();

        assertEquals(50L, falhasContabilizadas,
                "Todas as 50 chamadas devem lançar RecursoNaoEncontradoException de forma determinística");
        verify(repository, times(50)).findById(anyLong());
    }

    /**
     * Fail-fast: exclusão de recurso inexistente deve falhar antes de tentar o delete.
     * Garante que o banco não seja acessado desnecessariamente.
     */
    @Test
    void shouldFailFastOnDeleteOfNonExistentAsset() {
        when(repository.existsById(anyLong())).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> service.delete(99L));
        verify(repository, never()).deleteById(anyLong());
    }

    /**
     * Valor investido zero deve ser aceito pelo serviço.
     * Bean Validation permite @PositiveOrZero — o estado zero é válido para ativos sem custo de entrada.
     */
    @Test
    void shouldAcceptZeroInvestedValueAsValidInput() {
        AtivoFinanceiro ativo = buildAtivo("OURO0");
        ativo.setValorInvestido(BigDecimal.ZERO);

        when(repository.existsByTicker("OURO0")).thenReturn(false);
        when(repository.save(any(AtivoFinanceiro.class))).thenAnswer(inv -> {
            AtivoFinanceiro a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        assertDoesNotThrow(() -> service.save(ativo),
                "Valor investido zero deve ser aceito (@PositiveOrZero)");
    }

    private AtivoFinanceiro buildAtivo(String ticker) {
        return AtivoFinanceiro.builder()
                .ticker(ticker)
                .nome("Ativo Teste")
                .tipo(AtivoFinanceiro.TipoAtivo.ACAO)
                .categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("100.00"))
                .quantidade(1.0)
                .dataUltimaOperacao(LocalDate.now())
                .build();
    }
}
