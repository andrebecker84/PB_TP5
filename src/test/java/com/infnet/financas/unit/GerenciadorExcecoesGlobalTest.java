package com.infnet.financas.unit;

import com.infnet.financas.exception.GerenciadorExcecoesGlobal;
import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GerenciadorExcecoesGlobalTest {

    private final GerenciadorExcecoesGlobal handler = new GerenciadorExcecoesGlobal();

    @Test
    void handleNotFoundShouldReturnErrorView() {
        Model model = new ConcurrentModel();
        String view = handler.handleNotFound(new RecursoNaoEncontradoException("Not found"), model);
        assertEquals("error", view);
        assertEquals("Not found", model.getAttribute("errorMessage"));
    }

    @Test
    void handleDuplicateShouldReturnErrorView() {
        Model model = new ConcurrentModel();
        String view = handler.handleDuplicate(new RecursoDuplicadoException("Ticker já existe"), model);
        assertEquals("error", view);
        assertEquals("Ticker já existe", model.getAttribute("errorMessage"));
    }

    @Test
    void handleGeneralShouldReturnErrorViewWithSafeMessage() {
        Model model = new ConcurrentModel();
        String view = handler.handleGeneral(new Exception("Internal details"), model);
        assertEquals("error", view);
        // Verifica que a mensagem ao usuário NÃO expõe detalhes internos
        // (fail-gracefully)
        assertEquals("Ocorreu um erro interno. Por favor, tente novamente mais tarde.",
                model.getAttribute("errorMessage"));
    }

    @Test
    void handleNoResourceFoundShouldCompleteWithoutException() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        assertDoesNotThrow(() -> handler.handleNoResourceFound(ex));
    }
}
