package com.infnet.financas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de segurança HTTP aplicado a todas as respostas da aplicação.
 *
 * Mitiga as seguintes categorias de vulnerabilidade detectadas pelo OWASP ZAP:
 *   - Clickjacking              → X-Frame-Options: DENY
 *   - MIME sniffing             → X-Content-Type-Options: nosniff
 *   - Injeção de conteúdo      → Content-Security-Policy
 *   - Abertura de janelas      → Cross-Origin-Opener-Policy
 *   - Acesso a recursos        → Cross-Origin-Resource-Policy
 *   - Permissões de browser    → Permissions-Policy
 *
 * A CSP permite os CDNs utilizados pelo layout (Bootstrap, Bootstrap Icons,
 * Chart.js, Google Fonts) e as APIs externas do ticker (CoinGecko, AwesomeAPI).
 * 'unsafe-inline' é necessário para os scripts inline presentes nos templates
 * Thymeleaf; a remoção exigiria migração para nonces ou arquivos externos.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
            "connect-src 'self' https://api.coingecko.com https://economia.awesomeapi.com.br; " +
            "img-src 'self' data:; " +
            "frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        chain.doFilter(request, response);
    }
}
