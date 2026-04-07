package com.infnet.financas.selenium;

import com.infnet.financas.model.AtivoFinanceiro;
import com.infnet.financas.selenium.pageobjects.AtivoFormPage;
import com.infnet.financas.selenium.pageobjects.AtivoListaPage;
import com.infnet.financas.service.AtivoFinanceiroService;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração Selenium — validação end-to-end dos principais fluxos da
 * UI.
 * Screenshots são salvas em src/test/resources/screenshots/ para
 * rastreabilidade e versionamento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AtivoSeleniumTest {

    /** Caminho dos screenshots rastreáveis pelo Git (fora de target/). */
    private static final Path SCREENSHOTS_DIR = Paths.get("src/test/resources/screenshots");

    @Autowired
    private AtivoFinanceiroService ativoService;

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeAll
    static void setupClass() {
        // Selenium Manager (embutido no Selenium 4.6+) detecta e baixa automaticamente
        // o ChromeDriver compatível com a versão do Chrome instalada na máquina.
        // WebDriverManager (bonigarcia) foi removido: não possui suporte imediato para
        // builds do Chrome Dev/Canary que ainda não têm binário publicado no endpoint
        // oficial, causando 404 e fallback para versão incompatível.
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
        baseUrl = "http://localhost:" + port + "/ativos";
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
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Aguarda o document estar pronto antes de capturar a screenshot
            wait.until(d -> js.executeScript("return document.readyState").equals("complete"));

            // Força renderização do gráfico se necessário e rola para o topo primeiro
            js.executeScript("window.scrollTo(0, 0)");

            long height = (Long) js.executeScript(
                    "return Math.max(document.body.scrollHeight, document.body.offsetHeight," +
                            " document.documentElement.clientHeight, document.documentElement.scrollHeight," +
                            " document.documentElement.offsetHeight)");
            long width = (Long) js.executeScript(
                    "return Math.max(document.body.scrollWidth, document.body.offsetWidth," +
                            " document.documentElement.clientWidth, document.documentElement.scrollWidth," +
                            " document.documentElement.offsetWidth)");

            driver.manage().window().setSize(new Dimension((int) width + 50, (int) height + 100));

            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeName = name.replaceAll("[^a-zA-Z0-9]", "_");
            Path destPath = SCREENSHOTS_DIR.resolve(safeName + "_" + stamp + ".png");
            Files.copy(srcFile.toPath(), destPath);
        } catch (Exception e) {
            System.err.println("Erro ao capturar screenshot: " + e.getMessage());
        }
    }

    // ── Testes ───────────────────────────────────────────────────────────────

    /**
     * Fluxo feliz: cadastro de um novo ativo e verificação na listagem.
     */
    @Test
    @DisplayName("shouldRegisterNewAsset")
    void shouldRegisterNewAsset() {
        driver.get(baseUrl);
        AtivoListaPage listPage = new AtivoListaPage(driver);
        AtivoFormPage formPage = listPage.clickNewAsset();

        String ticker = "ETH" + System.currentTimeMillis();
        formPage.fillForm(ticker, "CRIPTOMOEDA", "CRIPTODIVISA", "15000.50", "2.5", "2024-02-18");

        takeScreenshot("shouldRegisterNewAsset_FormFilled");

        listPage = formPage.submit();

        assertThat(listPage.getSuccessMessage(), containsString("Ativo adicionado ao portfólio"));
        takeScreenshot("shouldRegisterNewAsset_SuccessToast");
        assertTrue(listPage.isAssetInList(ticker));
    }

    /**
     * Fluxo de edição: cadastra, localiza na tabela, edita e valida mensagem de
     * sucesso.
     */
    @Test
    @DisplayName("shouldEditAsset")
    void shouldEditAsset() {
        driver.get(baseUrl + "/novo");
        AtivoFormPage formPage = new AtivoFormPage(driver);
        String ticker = "EDIT" + System.currentTimeMillis();
        formPage.fillForm(ticker, "ACAO", "RENDA_VARIAVEL", "100.00", "1", "2024-02-18");
        formPage.submit();

        wait.until(ExpectedConditions.urlContains("/ativos"));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[td/span[contains(text(), '" + ticker + "')]]")));

        WebElement row = driver.findElement(
                By.xpath("//tr[td/span[contains(text(), '" + ticker + "')]]"));
        WebElement editButton = row.findElement(By.cssSelector("a[href*='/editar/']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", editButton);
        editButton.click();

        formPage = new AtivoFormPage(driver);
        formPage.fillForm(ticker, "ACAO", "RENDA_VARIAVEL", "150.00", "1.5", "2024-02-18");

        takeScreenshot("shouldEditAsset_FormFilled");

        AtivoListaPage listPage = formPage.submit();
        assertThat(listPage.getSuccessMessage(), containsString("Ativo atualizado"));
        takeScreenshot("shouldEditAsset_SuccessToast");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[td/span[contains(text(), '" + ticker + "')]]"))).isDisplayed();
        assertTrue(listPage.isAssetInList(ticker), "Ticker deve permanecer na lista após edição");
    }

    /**
     * Fluxo de exclusão: cadastra, localiza, clica em excluir e valida toast de
     * remoção.
     */
    @Test
    @DisplayName("shouldDeleteAsset")
    void shouldDeleteAsset() {
        String ticker = "DEL" + System.currentTimeMillis();

        driver.get(baseUrl + "/novo");
        AtivoFormPage formPage = new AtivoFormPage(driver);
        formPage.fillForm(ticker, "ACAO", "RENDA_VARIAVEL", "10.00", "1", "2024-02-18");
        formPage.submit();

        wait.until(ExpectedConditions.urlContains("/ativos"));
        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[td/span[contains(text(), '" + ticker + "')]]")));

        WebElement deleteBtn = row.findElement(By.cssSelector("a[href*='/excluir/']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", deleteBtn);
        deleteBtn.click();

        try {
            driver.switchTo().alert().accept();
        } catch (Exception ignored) {
            // Nenhuma caixa de diálogo exibida — comportamento esperado em alguns navegadores
        }

        AtivoListaPage listPage = new AtivoListaPage(driver);
        assertThat(listPage.getSuccessMessage(), containsString("Ativo removido"));
        takeScreenshot("shouldDeleteAsset_SuccessToast");
        assertFalse(listPage.isAssetInList(ticker), "Ticker excluído não deve mais aparecer na listagem");
    }

    /**
     * Verifica que o dashboard carrega com os gráficos principais visíveis.
     */
    @Test
    @DisplayName("shouldVisitDashboardWithCharts")
    void shouldVisitDashboardWithCharts() {
        String ts = String.valueOf(System.currentTimeMillis()).substring(7);
        ativoService.save(AtivoFinanceiro.builder().ticker("BTC-" + ts).nome("Bitcoin")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA).categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("80000.00")).quantidade(0.2).dataUltimaOperacao(LocalDate.now())
                .build());
        ativoService.save(AtivoFinanceiro.builder().ticker("ETH-" + ts).nome("Ethereum")
                .tipo(AtivoFinanceiro.TipoAtivo.CRIPTOMOEDA).categoria(AtivoFinanceiro.CategoriaAtivo.CRIPTODIVISA)
                .valorInvestido(new BigDecimal("25000.00")).quantidade(1.5).dataUltimaOperacao(LocalDate.now())
                .build());
        ativoService.save(AtivoFinanceiro.builder().ticker("PETR4").nome("Petrobras")
                .tipo(AtivoFinanceiro.TipoAtivo.ACAO).categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("10000.00")).quantidade(100.0).dataUltimaOperacao(LocalDate.now())
                .build());
        ativoService.save(AtivoFinanceiro.builder().ticker("MXRF11").nome("Maxi Renda")
                .tipo(AtivoFinanceiro.TipoAtivo.FII).categoria(AtivoFinanceiro.CategoriaAtivo.RENDA_VARIAVEL)
                .valorInvestido(new BigDecimal("8000.00")).quantidade(500.0).dataUltimaOperacao(LocalDate.now())
                .build());

        driver.get("http://localhost:" + port + "/ativos/dashboard");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 500)");

        WebElement lineChart = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lineChart")));
        WebElement cryptoComparison = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("cryptoComparisonChart")));
        WebElement acaoComparison = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("acaoComparisonChart")));
        WebElement barChart = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("barChart")));

        assertTrue(lineChart.isDisplayed(), "Line chart deve estar visível");
        assertTrue(cryptoComparison.isDisplayed(), "Crypto comparison chart deve estar visível");
        assertTrue(acaoComparison.isDisplayed(), "Ação comparison chart deve estar visível");
        assertTrue(barChart.isDisplayed(), "Bar chart deve estar visível");
    }

    /**
     * Verifica a persistência de sessão via cookie JSESSIONID.
     * As mensagens flash (RedirectAttributes) dependem da sessão HTTP:
     * sem o JSESSIONID correto após o redirect, o toast de sucesso não seria
     * renderizado.
     */
    @Test
    @DisplayName("shouldMaintainSessionCookieAfterPostRedirectGet")
    void shouldMaintainSessionCookieAfterPostRedirectGet() {
        driver.get(baseUrl + "/novo");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ticker")));

        String ticker = "SESS" + System.currentTimeMillis();
        AtivoFormPage formPage = new AtivoFormPage(driver);
        formPage.fillForm(ticker, "ACAO", "RENDA_VARIAVEL", "100.00", "1", "2024-02-18");
        AtivoListaPage listPage = formPage.submit();

        // A mensagem de sucesso via flash attribute só é entregue se o JSESSIONID
        // for mantido entre o POST e o GET de redirect — comprova que a sessão persiste
        assertThat(listPage.getSuccessMessage(), containsString("Ativo adicionado ao portfólio"));
        assertTrue(listPage.isAssetInList(ticker));

        Cookie jsession = driver.manage().getCookieNamed("JSESSIONID");
        assertNotNull(jsession, "JSESSIONID deve existir após o ciclo POST → redirect → GET");
        assertFalse(jsession.getValue().isBlank(), "Valor do JSESSIONID não pode ser vazio");
    }

    /**
     * Modelo de aquisições: o mesmo ticker pode ser registrado múltiplas vezes,
     * cada compra é um lote independente com sua data e valor próprios.
     */
    @Test
    @DisplayName("shouldAllowMultipleAcquisitionsForSameTicker")
    void shouldAllowMultipleAcquisitionsForSameTicker() {
        String ticker = "SOL" + System.currentTimeMillis();

        driver.get(baseUrl + "/novo");
        AtivoFormPage formPage = new AtivoFormPage(driver);
        formPage.fillForm(ticker, "CRIPTOMOEDA", "CRIPTODIVISA", "1000.00", "10", "2024-02-18");
        AtivoListaPage listPage = formPage.submit();
        assertThat(listPage.getSuccessMessage(), containsString("Ativo adicionado ao portfólio"));

        driver.get(baseUrl + "/novo");
        formPage = new AtivoFormPage(driver);
        formPage.fillForm(ticker, "CRIPTOMOEDA", "CRIPTODIVISA", "2000.00", "20", "2024-03-10");
        takeScreenshot("shouldAllowMultipleAcquisitions_SecondFormFilled");
        AtivoListaPage listPage2 = formPage.submit();

        assertThat(listPage2.getSuccessMessage(), containsString("Ativo adicionado ao portfólio"));
        takeScreenshot("shouldAllowMultipleAcquisitions_SecondAcquisitionSuccess");
    }
}
