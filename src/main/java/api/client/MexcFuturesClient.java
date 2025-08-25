package api.client;

import api.model.Spread;
import api.model.mexc.MexcTickerResponse;
import api.model.mexc.MexcTickerSingleResponse;
import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import io.restassured.RestAssured;

import java.time.LocalDateTime;

import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;

public class MexcFuturesClient {

    private static final String BASE_URL = "https://futures.mexc.com";

    static {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // 1. Get current bid/ask for BTC_USDT
    //https://futures.mexc.com/api/v1/contract/ticker?symbol=APT_USDT
    public static MexcTickerResponse getBookTicker() {
        return RestAssured
                .given()
                .baseUri(BASE_URL)
                .redirects().follow(true)
                .when()
                .get("/api/v1/contract/ticker")
                .then()
                .extract().as(MexcTickerResponse.class);
    }

    public static MexcTickerSingleResponse getBookTicker(String symbol) {
        return RestAssured
                .given()
                .baseUri(BASE_URL)
                .redirects().follow(true)
                .queryParam("symbol", symbol)
                .when()
                .get("/api/v1/contract/ticker")
                .then()
                .extract().as(MexcTickerSingleResponse.class);
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        Configuration.pageLoadStrategy = "none"; // avoid full page wait
        open("https://www.mexc.com/futures/BTC_USDT?type=linear_swap");
    }


    public static void makeOder(Spread spread) {
//7 seconds
        System.out.println("Starting Mexc Order " + LocalDateTime.now());
        Selenide.open(String.format("https://www.mexc.com/futures/%s?type=linear_swap", toMexcSymbol(spread.symbol)));
        $x("//div[@id='mexc_contract_v_open_position']//input[@autocomplete='off']").setValue("10");
        if (spread.getShortExchange().equals("Mexc")) {
            $x("//button[@data-testid='contract-trade-open-short-btn']").click(ClickOptions.usingJavaScript());
        } else {
            $x("//button[@data-testid='contract-trade-open-long-btn']").click(ClickOptions.usingJavaScript());
        }
        System.out.println("Mexc Order Opened" + LocalDateTime.now());
    }

    public static void closeOrder(Spread spread) {
        //4 seconds
        //if need to close
        //$x("//button/span[text()='Confirm']").click();
        $x("//span[@data-testid='contract-trade-order-form-tab-close']").click();
        //$x("//div[@style='display: block;']//input[@autocomplete='off']").clear();
        $x("(//div[@style='display: block;']//span[contains(@style, 'left: 100%;')])[1]").click();
        //contract-trade-close-short-btn //contract-trade-close-long-btn
        if (spread.getShortExchange().equals("Mexc")) {
            $x("//button[@data-testid='contract-trade-close-short-btn']").click(ClickOptions.usingJavaScript());
        } else {
            $x("//button[@data-testid='contract-trade-close-long-btn']").click(ClickOptions.usingJavaScript());
        }
        int a = 0;
    }

    /**
     * Converts a symbol to MEXC format (e.g., BTCUSDT -> BTC_USDT).
     */
    public static String toMexcSymbol(String symbol) {
        if (symbol.endsWith("USDT")) {
            return symbol.replace("USDT", "_USDT");
        }
        return symbol;
    }
}
