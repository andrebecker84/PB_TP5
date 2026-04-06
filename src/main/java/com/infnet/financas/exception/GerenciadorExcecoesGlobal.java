package com.infnet.financas.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GerenciadorExcecoesGlobal {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String handleNotFound(RecursoNaoEncontradoException ex, Model model) {
        log.error("Recurso não encontrado: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public String handleDuplicate(RecursoDuplicadoException ex, Model model) {
        log.warn("Tentativa de duplicidade: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound(NoResourceFoundException ex) {
        // Silently ignore browser-specific resource errors (like .well-known)
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Erro inesperado no sistema: ", ex);
        model.addAttribute("errorMessage", "Ocorreu um erro interno. Por favor, tente novamente mais tarde.");
        return "error";
    }
}
