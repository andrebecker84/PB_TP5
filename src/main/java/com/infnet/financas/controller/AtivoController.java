package com.infnet.financas.controller;

import com.infnet.financas.exception.RecursoDuplicadoException;
import com.infnet.financas.model.AtivoFinanceiro;
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
        model.addAttribute("ativo", new AtivoFinanceiro());
        popularModelFormulario(model);
        return "ativo-form";
    }

    @PostMapping
    public String salvarAtivo(@Valid @ModelAttribute("ativo") AtivoFinanceiro ativo,
            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            popularModelFormulario(model);
            return "ativo-form";
        }
        try {
            service.save(ativo);
            ra.addFlashAttribute("successMessage", "Ativo adicionado ao portfólio!");
            return "redirect:/ativos";
        } catch (RecursoDuplicadoException e) {
            model.addAttribute("errorMessage", e.getMessage());
            popularModelFormulario(model);
            return "ativo-form";
        }
    }

    @GetMapping("/editar/{id}")
    public String exibirFormularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("ativo", service.findById(id));
        popularModelFormulario(model);
        return "ativo-form";
    }

    @PostMapping("/{id}")
    public String atualizarAtivo(@PathVariable Long id,
            @Valid @ModelAttribute("ativo") AtivoFinanceiro ativo,
            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            popularModelFormulario(model);
            return "ativo-form";
        }
        try {
            service.update(id, ativo);
            ra.addFlashAttribute("successMessage", "Ativo atualizado!");
            return "redirect:/ativos";
        } catch (RecursoDuplicadoException e) {
            model.addAttribute("errorMessage", e.getMessage());
            popularModelFormulario(model);
            return "ativo-form";
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluirAtivo(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Ativo removido!");
        return "redirect:/ativos";
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
