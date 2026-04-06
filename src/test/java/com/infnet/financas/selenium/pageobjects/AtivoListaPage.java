package com.infnet.financas.selenium.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class AtivoListaPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By newAssetButton = By.partialLinkText("Adicionar Ativo");
    private final By assetRows = By.cssSelector("table tbody tr");
    private final By successAlert = By.id("successToast");

    public AtivoListaPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public AtivoFormPage clickNewAsset() {
        wait.until(ExpectedConditions.elementToBeClickable(newAssetButton)).click();
        return new AtivoFormPage(driver);
    }

    public boolean isAssetInList(String ticker) {
        List<WebElement> rows = driver.findElements(assetRows);
        return rows.stream().anyMatch(row -> row.getText().contains(ticker));
    }

    /**
     * Aguarda o redirect para /ativos ser completado e o toast de sucesso ser
     * renderizado.
     * Estratégia: aguarda presença do #successToast no DOM (indica que o flash
     * attribute
     * chegou e o Thymeleaf renderizou th:if="${successMessage}"), depois aguarda
     * visibilidade.
     */
    public String getSuccessMessage() {
        // Passo 1: aguarda a URL ser /ativos (pode ser satisfeita prematuramente no
        // POST)
        wait.until(ExpectedConditions.urlContains("/ativos"));
        // Passo 2: aguarda o #successToast aparecer NO DOM (th:if="${successMessage}" =
        // true)
        // Isso garante que estamos na página GET pos-redirect, não ainda no POST
        wait.until(ExpectedConditions.presenceOfElementLocated(successAlert));
        // Passo 3: aguarda o elemento ser visível (classe 'show' garante isso
        // imediatamente)
        return wait.until(ExpectedConditions.visibilityOfElementLocated(successAlert)).getText();
    }

}
