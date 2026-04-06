package com.infnet.financas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redireciona o contexto raiz para o dashboard principal.
 * Garante que acessar http://localhost:8080/ leve ao ponto de entrada correto.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/ativos/dashboard";
    }
}
