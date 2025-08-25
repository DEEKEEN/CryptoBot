package org.example;

import java.time.LocalDateTime;

import static com.codeborne.selenide.Selenide.$x;

public class BuyPage {
    public void buyUSDT(double amountUSD) {

        while (true) {
            int startP2Pvalue = 1;
            if ($x("//tr[contains(@data-row-key, 'Promoted Ad')]").isDisplayed()) startP2Pvalue = 2;
            String price = $x(String.format("(//div[contains(@class,'headline5 text-primaryText')])[%s]", startP2Pvalue)).getText();
            if (Double.valueOf(price) < amountUSD) {
                if (!$x(String.format("(//button[contains(@id,'C2CofferList_btn_buy')])[%s]", startP2Pvalue)).getText().contains("Restricted")) {
                    System.out.println("Datetime " + LocalDateTime.now());
                    System.out.println("Price: " + price);
                    $x(String.format("(//button[contains(@id,'C2CofferList_btn_buy')])[%s]", startP2Pvalue)).click();
                    String value = $x("//input[contains(@id,'C2CofferBuy_amount_input')]").getAttribute("placeholder");

                    String[] parts = value.split("-");

                    String lastPart = parts[parts.length - 1].trim().replace(",", "");

                    double lastPrice = Double.parseDouble(lastPart);

                    System.out.println("Last price: " + lastPrice);
                    $x("//input[contains(@id,'C2CofferBuy_amount_input')]").setValue(String.valueOf(lastPrice));
                    //select payment if we see more than 1 payment
                    //Privat Bank (Universal Card)
                    if ($x("//div[text()='Set my payment method']").isDisplayed()) {
                        $x("//div[text()='Set my payment method']").click();
                        //select PrivatBank
                        $x("//div[@role='button' and @aria-label='Privat Bank (Universal Card)'] | //div[@role='button' and @aria-label='Monobank (Card)']").click();
                    }
                    $x("//tr[contains(@class,'-web-table-expanded')]//button[text()='Buy USDT']").click();
                    int a = 0;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}