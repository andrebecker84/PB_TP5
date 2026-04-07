package com.infnet.financas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
                .httpBasic(basic -> basic.disable());
        return http.build();
    }
}
