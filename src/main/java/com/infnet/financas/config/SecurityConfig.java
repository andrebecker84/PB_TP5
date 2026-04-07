package com.infnet.financas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Configuração de segurança HTTP da aplicação.
 *
 * O sistema é público por design — não há autenticação de usuário.
 * A configuração centra-se em CSRF e desativa recursos desnecessários
 * para evitar auto-configuração com comportamento padrão indesejado
 * (formulário de login, basic auth).
 *
 * Proteção CSRF:
 *   O Spring Security gera um token por sessão e valida-o em toda
 *   requisição de escrita (POST). O Thymeleaf injeta automaticamente
 *   o campo oculto {@code _csrf} nos formulários via
 *   {@code CsrfRequestDataValueProcessor} — sem alterações nos templates.
 *   Isso elimina o alerta "Absence of Anti-CSRF Tokens" detectado pelo
 *   OWASP ZAP no scan DAST do pipeline CI.
 *
 *   Compatibilidade Spring Security 6 + Thymeleaf:
 *   O {@code CsrfTokenRequestAttributeHandler} padrão do Spring Security 6
 *   armazena um {@code Supplier<CsrfToken>} (deferred) no atributo de request.
 *   O {@code CsrfRequestDataValueProcessor} do Spring MVC lê o atributo e,
 *   em versões anteriores à 6.4, não resolve o Supplier automaticamente —
 *   o campo {@code _csrf} não é injetado no HTML e todo POST retorna 403.
 *   O {@code CsrfTokenResolvingFilter} resolve o Supplier imediatamente após
 *   o {@code CsrfFilter}, garantindo que o atributo contenha um
 *   {@code CsrfToken} concreto antes da renderização Thymeleaf.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterAfter(new CsrfTokenResolvingFilter(), CsrfFilter.class);
        return http.build();
    }

    /**
     * Força a resolução antecipada do token CSRF antes que o Thymeleaf
     * inicie o streaming da resposta.
     *
     * Problema: o {@code XorCsrfTokenRequestAttributeHandler} (padrão do
     * Spring Security 6) armazena um {@code SupplierCsrfToken} no atributo
     * de request. Esse objeto implementa {@code CsrfToken} e inicializa a
     * sessão HTTP de forma preguiçosa ao primeiro acesso. O Thymeleaf acessa
     * o token durante o processamento de {@code th:action} — mas a esta
     * altura o layout já enviou os fragments anteriores (sidebar, topbar,
     * ticker) e a resposta está parcialmente comprometida. A tentativa de
     * criar a sessão via {@code request.getSession()} lança:
     * "Cannot create a session after the response has been committed".
     *
     * Solução: chamar {@code token.getToken()} no filtro, antes de qualquer
     * byte de resposta ser gravado, força a inicialização da sessão e o
     * armazenamento do token. A partir daí o {@code SupplierCsrfToken}
     * mantém o delegate em cache — chamadas posteriores do Thymeleaf não
     * disparam nova sessão e o campo oculto {@code _csrf} é injetado
     * corretamente no formulário.
     */
    private static final class CsrfTokenResolvingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            Object attribute = request.getAttribute(CsrfToken.class.getName());
            if (attribute instanceof CsrfToken token) {
                // Dispara a inicialização antecipada: cria sessão e persiste o
                // token antes do streaming — o delegate fica em cache no
                // SupplierCsrfToken para uso posterior pelo Thymeleaf.
                token.getToken();
            } else if (attribute instanceof Supplier<?> supplier) {
                // Compatibilidade com implementações que expõem Supplier diretamente
                CsrfToken token = (CsrfToken) supplier.get();
                if (token != null) {
                    request.setAttribute(CsrfToken.class.getName(), token);
                    request.setAttribute(token.getParameterName(), token);
                }
            }
            chain.doFilter(request, response);
        }
    }
}
