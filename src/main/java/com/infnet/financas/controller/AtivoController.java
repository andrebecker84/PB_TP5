package com.infnet.financas.controller;

import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.model.AtivoFinanceiroForm;
import com.infnet.financas.service.AtivoFinanceiroService;
import com.infnet.financas.service.AtivoFinanceiroService.DashboardMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller MVC responsável pelo fluxo de ativos financeiros.
 * Delega toda lógica de negócio ao {@link AtivoFinanceiroService},
 * mantendo-se restrito ao roteamento e ao binding de model/view.
 */
@Controller
@RequestMapping("/ativos")
@RequiredArgsConstructor
public class AtivoController {

    private final AtivoFinanceiroService service;

    private static final String FORM_VIEW        = "ativo-form";
    private static final String SUCCESS_MSG       = SUCCESS_MSG;
    private static final String REDIRECT_ATIVOS   = REDIRECT_ATIVOS;

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardMetrics metrics = service.buildDashboardMetrics();
        model.addAttribute("ativos", metrics.ativos());
        model.addAttribute("totalInvestido", metrics.totalInvestido());
        model.addAttribute("patrimonioTotal", metrics.patrimonioTotal());
        model.addAttribute("saldoDinheiro", metrics.saldoDinheiro());
        model.addAttribute("despesasMensais", metrics.despesasMensais());
        model.addAttribute("alocacaoPorTipo", metrics.alocacaoPorTipo());
        model.addAttribute("quantidadeAtivos", metrics.quantidadeAtivos());
        model.addAttribute("tipoMaiorAlocacao", metrics.tipoMaiorAlocacao());
        return "dashboard";
    }

    @GetMapping
    public String listarAtivos(Model model) {
        model.addAttribute("ativos", service.findAll());
        return "ativo-lista";
    }

    @GetMapping("/novo")
    public String exibirFormularioCriacao(Model model) {
        model.addAttribute("ativo", new AtivoFinanceiroForm());
        popularModelFormulario(model);
        return FORM_VIEW;
    }

    @PostMapping
    public String salvarAtivo(@Valid @ModelAttribute("ativo") AtivoFinanceiroForm form,
            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            popularModelFormulario(model);
            return FORM_VIEW;
        }
        try {
            service.save(form.toEntity());
            ra.addFlashAttribute(SUCCESS_MSG, "Ativo adicionado ao portfólio!");
            return REDIRECT_ATIVOS;
        } catch (RecursoDuplicadoException e) {
            model.addAttribute("errorMessage", e.getMessage());
            popularModelFormulario(model);
            return FORM_VIEW;
        }
    }

    @GetMapping("/editar/{id}")
    public String exibirFormularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("ativo", AtivoFinanceiroForm.from(service.findById(id)));
        popularModelFormulario(model);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String atualizarAtivo(@PathVariable Long id,
            @Valid @ModelAttribute("ativo") AtivoFinanceiroForm form,
            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            popularModelFormulario(model);
            return FORM_VIEW;
        }
        try {
            service.update(id, form.toEntity());
            ra.addFlashAttribute(SUCCESS_MSG, "Ativo atualizado!");
            return REDIRECT_ATIVOS;
        } catch (RecursoDuplicadoException e) {
            model.addAttribute("errorMessage", e.getMessage());
            popularModelFormulario(model);
            return FORM_VIEW;
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluirAtivo(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute(SUCCESS_MSG, "Ativo removido!");
        return REDIRECT_ATIVOS;
    }

    /**
     * Centraliza o carregamento dos enums no model do formulário — elimina
     * repetição (DRY).
     */
    private void popularModelFormulario(Model model) {
        model.addAttribute("tipos", AtivoFinanceiro.TipoAtivo.values());
        model.addAttribute("categorias", AtivoFinanceiro.CategoriaAtivo.values());
    }
}
