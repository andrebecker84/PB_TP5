package com.infnet.financas.repository;

import com.infnet.financas.model.AtivoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtivoFinanceiroRepository extends JpaRepository<AtivoFinanceiro, Long> {
    Optional<AtivoFinanceiro> findByTicker(String ticker);

    boolean existsByTicker(String ticker);
}
