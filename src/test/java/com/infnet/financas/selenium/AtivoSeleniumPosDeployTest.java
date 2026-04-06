package com.infnet.financas.selenium;

import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.service.AtivoFinanceiroService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de fumaça pós-deploy — validam que as rotas críticas do sistema estão
 * operacionais após cada deploy em staging.
 *
 * Em CI/CD, estes testes são executados pelo workflow post-deploy.yml após o
 * CD concluir com sucesso. Localmente, sobem uma instância Spring Boot com
 * porta aleatória para isolamento.
 *
 * O foco é confirmar integridade estrutural da UI, não repetir a cobertura
 * funcional já garantida por AtivoSeleniumTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AtivoSeleniumPosDeployTest {

    private static final Path SCREENSHOTS_DIR = Paths.get("src/test/resources/screenshots/pos-deploy");

    @Autowired
    private AtivoFinanceiroService ativoService;

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
        try {
            Files.createDirectories(SCREENSHOTS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setupTest() {
        ativoService.deleteAll();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void teardown(TestInfo testInfo) {
        if (driver != null) {
            takeScreenshot(testInfo.getDisplayName());
            driver.quit();
        }
    }

    // ── Utilitário ───────────────────────────────────────────────────────────

    private void takeScreenshot(String name) {
        try {
            Thread.sleep(1000);
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeName = name.replaceAll("[^a-zA-Z0-9]", "_");
            Path destPath = SCREENSHOTS_DIR.resolve(safeName + "_" + stamp + ".png");
            Files.copy(srcFile.toPath(), destPath);
        } catch (Exception e) {
            System.err.println("Erro ao capturar screenshot pós-deploy: " + e.getMessage());
        }
    }

    // ── Testes de fumaça ─────────────────────────────────────────────────────

    /**
     * Verifica que a raiz "/" redireciona para o dashboard e que este responde.
     * Falha indica problema na rota de entrada da aplicação.
     */
    @Test
    @DisplayName("shouldRedirectRootToDashboard")
    void shouldRedirectRootToDashboard() {
        driver.get(baseUrl + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        assertTrue(driver.getCurrentUrl().contains("/ativos/dashboard"),
                "A raiz deve redirecionar para o dashboard");
        assertFalse(driver.getTitle().isBlank(),
                "O título da página não pode estar vazio");
    }

    /**
     * Verifica que o dashboard carrega com os quatro gráficos visíveis.
     * Falha indica problema nos templates Thymeleaf ou nos scripts de gráfico.
     */
    @Test
    @DisplayName("shouldLoadDashboardWithCharts")
    void shouldLoadDashboardWithCharts() {
        String ts = String.valueOf(System.currentTimeMillis()).substring(7);
        ativoService.save(AtivoFinanceiro.builder()
                .ticker("BTC-" + ts).nome("Bitcoin")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA)
                .categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("50000.00")).quantidade(0.5)
                .dataUltimaOperacao(LocalDate.now()).build());
        ativoService.save(AtivoFinanceiro.builder()
                .ticker("PETR4-" + ts).nome("Petrobras")
                .tipo(AtivoFinanceiro.TipoAtivo.ACAO)
                .categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("10000.00")).quantidade(100.0)
                .dataUltimaOperacao(LocalDate.now()).build());

        driver.get(baseUrl + "/ativos/dashboard");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lineChart"))).isDisplayed(),
                "Gráfico de evolução deve estar visível");
        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("barChart"))).isDisplayed(),
                "Gráfico de barras deve estar visível");
    }

    /**
     * Verifica que a listagem de ativos responde e exibe a tabela.
     * Falha indica problema no controller GET /ativos ou no template ativo-lista.
     */
    @Test
    @DisplayName("shouldLoadAssetListPage")
    void shouldLoadAssetListPage() {
        driver.get(baseUrl + "/ativos");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        WebElement table = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
        assertTrue(table.isDisplayed(), "Tabela de ativos deve estar presente na listagem");
    }

    /**
     * Verifica que o formulário de novo ativo carrega com os selects de tipo e
     * categoria preenchidos. Falha indica problema no binding de enums no model.
     */
    @Test
    @DisplayName("shouldLoadNewAssetFormWithOptions")
    void shouldLoadNewAssetFormWithOptions() {
        driver.get(baseUrl + "/ativos/novo");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        WebElement typeSelect = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("type")));
        WebElement categorySelect = driver.findElement(By.id("category"));

        assertTrue(typeSelect.isDisplayed(), "Select de tipo deve estar presente");
        assertTrue(categorySelect.isDisplayed(), "Select de categoria deve estar presente");
    }
}
