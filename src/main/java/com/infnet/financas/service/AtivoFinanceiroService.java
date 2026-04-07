package com.infnet.financas.service;

import com.infnet.financas.exception.RecursoNaoEncontradoException;
import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.repository.AtivoFinanceiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Camada de serviço responsável pela lógica de negócio dos ativos financeiros.
 * Aplica Fail-Fast, logging para rastreabilidade e encapsula métricas do
 * dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtivoFinanceiroService {

    private final AtivoFinanceiroRepository repository;

    @Transactional(readOnly = true)
    public List<AtivoFinanceiro> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public AtivoFinanceiro findById(Long id) {
        log.debug("Buscando ativo por ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ativo não encontrado com ID: " + id));
    }

    @Transactional
    public AtivoFinanceiro save(AtivoFinanceiro ativo) {
        log.info("Registrando nova aquisição — ticker: {}", ativo.getTicker());
        // Cada registro representa uma aquisição (lote de compra) independente.
        // O mesmo ticker pode aparecer múltiplas vezes no portfólio com datas distintas.
        AtivoFinanceiro saved = repository.save(ativo);
        log.info("Aquisição registrada com sucesso. ID={}, ticker={}", saved.getId(), saved.getTicker());
        return saved;
    }

    @Transactional
    public AtivoFinanceiro update(Long id, AtivoFinanceiro detalhesAtivo) {
        log.info("Tentativa de atualizar ativo ID={} para ticker={}", id, detalhesAtivo.getTicker());
        // Justificativa: chama o repositório diretamente (não via this.findById) para que o
        // proxy Spring AOP do @Transactional desta própria chamada seja preservado.
        AtivoFinanceiro ativo = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ativo não encontrado com ID: " + id));

        // Delega a atualização dos campos ao próprio modelo (SRP + CQS).
        ativo.updateFrom(detalhesAtivo);

        AtivoFinanceiro updated = repository.save(ativo);
        log.info("Ativo ID={} atualizado com sucesso.", id);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        log.info("Tentativa de excluir ativo ID={}", id);
        if (!repository.existsById(id)) {
            log.warn("Ativo ID={} não encontrado para exclusão.", id);
            throw new RecursoNaoEncontradoException("Não é possível excluir: Ativo não encontrado com ID: " + id);
        }
        repository.deleteById(id);
        log.info("Ativo ID={} excluído com sucesso.", id);
    }

    @Transactional
    public void deleteAll() {
        log.info("Removendo todos os ativos do portfólio.");
        repository.deleteAll();
    }

    /**
     * Computa as métricas agregadas necessárias para o dashboard.
     * Mantém o controller coeso e sem lógica de negócio.
     */
    @Transactional(readOnly = true)
    public DashboardMetrics buildDashboardMetrics() {
        List<AtivoFinanceiro> ativos = repository.findAll();
        ativos.sort(Comparator.comparing(AtivoFinanceiro::getId).reversed());

        BigDecimal totalInvestido = ativos.stream()
                .map(AtivoFinanceiro::getValorInvestido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<AtivoFinanceiro.TipoAtivo, BigDecimal> alocacaoPorTipo = ativos.stream()
                .collect(Collectors.groupingBy(
                        AtivoFinanceiro::getTipo,
                        Collectors.reducing(BigDecimal.ZERO, AtivoFinanceiro::getValorInvestido, BigDecimal::add)));

        String tipoMaiorAlocacao = alocacaoPorTipo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().toString())
                .orElse("NENHUM");

        BigDecimal saldoDinheiro = new BigDecimal("12450.00");
        BigDecimal despesasMensais = new BigDecimal("4820.00");

        return new DashboardMetrics(
                ativos, totalInvestido, totalInvestido.add(saldoDinheiro),
                saldoDinheiro, despesasMensais, alocacaoPorTipo,
                ativos.size(), tipoMaiorAlocacao);
    }

    /**
     * Objeto de valor imutável que agrega as métricas do dashboard em um único
     * retorno.
     * Evita múltiplas consultas ao banco e mantém o controller isento de lógica.
     */
    public record DashboardMetrics(
            List<AtivoFinanceiro> ativos,
            BigDecimal totalInvestido,
            BigDecimal patrimonioTotal,
            BigDecimal saldoDinheiro,
            BigDecimal despesasMensais,
            Map<AtivoFinanceiro.TipoAtivo, BigDecimal> alocacaoPorTipo,
            int quantidadeAtivos,
            String tipoMaiorAlocacao) {
    }
}
