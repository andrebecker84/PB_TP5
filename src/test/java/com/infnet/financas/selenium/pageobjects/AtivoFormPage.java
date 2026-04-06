package com.infnet.financas.selenium.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AtivoFormPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By tickerField = By.id("ticker");
    private final By typeSelect = By.id("type");
    private final By categorySelect = By.id("category");
    private final By investedValueField = By.id("investedValue");
    private final By quantityField = By.id("quantity");
    private final By lastOperationDateField = By.id("lastOperationDate");
    private final By submitButton = By.cssSelector("button[type='submit']");
    private final By errorAlert = By.cssSelector("#errorToast .toast-body");

    public AtivoFormPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void fillForm(String ticker, String type, String category, String value, String quantity,
            String date) {
        new Select(wait.until(ExpectedConditions.elementToBeClickable(categorySelect))).selectByVisibleText(category);
        new Select(wait.until(ExpectedConditions.elementToBeClickable(typeSelect))).selectByVisibleText(type);

        WebElement tickerElem = wait.until(ExpectedConditions.elementToBeClickable(tickerField));
        tickerElem.clear();
        tickerElem.sendKeys(ticker);

        // nameElem não é mais preenchido pois virou readOnly (auto-fill por JS)

        WebElement valElem = driver.findElement(investedValueField);
        valElem.clear();
        valElem.sendKeys(value);

        WebElement qElem = driver.findElement(quantityField);
        qElem.clear();
        qElem.sendKeys(quantity);

        WebElement dElem = driver.findElement(lastOperationDateField);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; " +
                        "arguments[0].dispatchEvent(new Event('change')); " +
                        "arguments[0].dispatchEvent(new Event('input'));",
                dElem, date);
    }

    public AtivoListaPage submit() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
        return new AtivoListaPage(driver);
    }

    public String getErrorMessage() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(errorAlert)).getText();
        } catch (Exception e) {
            return "Validation Error";
        }
    }
}
