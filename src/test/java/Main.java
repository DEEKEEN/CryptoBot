import com.codeborne.selenide.Configuration;
import org.example.BuyPage;

import static com.codeborne.selenide.Selenide.open;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Configuration.holdBrowserOpen = true;
        BuyPage buyPage = new BuyPage();
        open("https://p2p.binance.com/trade/all-payments/USDT?fiat=UAH");
        buyPage.buyUSDT(42.20); // Buy $100 worth of USDT
    }
}