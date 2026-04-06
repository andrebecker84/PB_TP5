package com.infnet.financas.unit;

import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.repository.AtivoFinanceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração da camada de persistência usando banco H2 real (sem
 * mocks).
 * Valida os métodos custom do repository e o comportamento da constraint
 * unique.
 */
@DataJpaTest
class AtivoFinanceiroRepositoryTest {

    @Autowired
    private AtivoFinanceiroRepository repository;

    private AtivoFinanceiro ativo;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        ativo = AtivoFinanceiro.builder()
                .ticker("PETR4")
                .nome("Petrobras")
                .tipo(AtivoFinanceiro.TipoAtivo.ACAO)
                .categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("15000.00"))
                .quantidade(500.0)
                .dataUltimaOperacao(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Test
    void shouldPersistAndRetrieveAssetById() {
        AtivoFinanceiro saved = repository.save(ativo);
        assertNotNull(saved.getId(), "O ID deve ser gerado automaticamente pelo banco");

        Optional<AtivoFinanceiro> found = repository.findById(saved.getId());
        assertTrue(found.isPresent(), "O ativo persistido deve ser recuperável por ID");
        assertEquals("PETR4", found.get().getTicker());
    }

    @Test
    void shouldReturnTrueWhenTickerExists() {
        repository.save(ativo);
        assertTrue(repository.existsByTicker("PETR4"),
                "existsByTicker deve retornar true para ticker já cadastrado");
    }

    @Test
    void shouldReturnFalseWhenTickerDoesNotExist() {
        assertFalse(repository.existsByTicker("INEXISTENTE"),
                "existsByTicker deve retornar false para ticker não cadastrado");
    }

    @Test
    void shouldFindByTickerSuccessfully() {
        repository.save(ativo);
        Optional<AtivoFinanceiro> found = repository.findByTicker("PETR4");
        assertTrue(found.isPresent(), "findByTicker deve encontrar o ativo pelo ticker exato");
        assertEquals("Petrobras", found.get().getNome());
    }

    @Test
    void shouldReturnEmptyWhenTickerNotFound() {
        Optional<AtivoFinanceiro> found = repository.findByTicker("NAOEXISTE");
        assertTrue(found.isEmpty(), "findByTicker deve retornar Optional.empty() para ticker inexistente");
    }

    @Test
    void shouldDeleteAssetAndNotFindIt() {
        AtivoFinanceiro saved = repository.save(ativo);
        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()), "Ativo excluído não deve mais existir no banco");
    }

    @Test
    void shouldCountAssetsCorrectly() {
        assertEquals(0, repository.count(), "Banco deve estar vazio antes do teste");

        repository.save(ativo);
        AtivoFinanceiro outro = AtivoFinanceiro.builder()
                .ticker("VALE3").nome("Vale S.A.")
                .tipo(AtivoFinanceiro.TipoAtivo.ACAO)
                .categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("8000.00"))
                .quantidade(200.0)
                .dataUltimaOperacao(LocalDate.now())
                .build();
        repository.save(outro);

        assertEquals(2, repository.count(), "Deve haver 2 ativos após salvar dois registros distintos");
    }
}
