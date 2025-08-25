import api.client.BinanceFuturesClient;
import api.client.MexcFuturesClient;
import api.formatter.PlainFormatter;
import api.model.mexc.Datum;
import api.model.Spread;
import api.model.binance.CoinsResponse;
import api.utils.Utils;
import com.codeborne.selenide.Configuration;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static api.model.Spread.getMaxSpread;
import static com.codeborne.selenide.Selenide.open;

public class BinanceMexc {
    static {
        Logger rootLogger = Logger.getLogger("");
        Arrays.stream(rootLogger.getHandlers()).forEach(handler -> handler.setFormatter(new PlainFormatter()));
    }

    private static final Logger LOGGER = Logger.getLogger(BinanceMexc.class.getName());
    private static final BigDecimal MIN_SPREAD_PERCENT = new BigDecimal("4");
    private static final int SCALE = 10;

    public static void main(String[] args) {
        Configuration.holdBrowserOpen = true;
        Configuration.pageLoadTimeout = 10000;
        Configuration.timeout = 8000;
        Configuration.pageLoadStrategy = "none"; // avoid full page wait
        open("https://www.mexc.com/futures/BTC_USDT?type=linear_swap");
//        Spread spread = Spread.builder().longExchange("Binance").shortExchange("Mexc").symbol("FISUSDT").build();
//        BinanceFuturesClient.makeOrder(spread);
//        BinanceFuturesClient.closeOrder(spread);
        do {
            startMakingMoney();//Thread.local add
        } while (shouldRestart());
    }

    public static void startMakingMoney() {
        //LOGGER.info("Start: " + LocalDateTime.now());
        List<CoinsResponse> binanceCoins = Utils.convertJsonStringToModel(
                BinanceFuturesClient.getAllCoins(),
                new TypeReference<List<CoinsResponse>>() {}
        );
        //LOGGER.info("Binance coins loaded: " + LocalDateTime.now());
        List<Datum> mexcCoins = MexcFuturesClient.getBookTicker().getData();
        //LOGGER.info("MEXC coins loaded: " + LocalDateTime.now());
        List<Spread> spreadList = calculateSpreadBetweenExchanges(binanceCoins, mexcCoins);
        //LOGGER.info("Spread calculation done: " + LocalDateTime.now());
        Spread spread = Spread.getMaxSpread(spreadList);

        //Make deal
        if(!spreadList.isEmpty()){
            MexcFuturesClient.makeOder(spread);
            BinanceFuturesClient.makeOrder2(spread);
        }

        //verifySpreadAndCloseADeal
        if(!spreadList.isEmpty()) while (!checkSpreadForSymbol(getMaxSpread(spreadList))) {
            if (!spreadList.isEmpty()) {
                LOGGER.info("SPREAD status is " + checkSpreadForSymbol(getMaxSpread(spreadList)));
            }
        }

        //Close deal
        if(!spreadList.isEmpty()) {
            MexcFuturesClient.closeOrder(spread);
            BinanceFuturesClient.closeOrder2(spread);
            int a =0;
        }
        //LOGGER.info("Deal closed at: " + LocalDateTime.now());
    }

    private static boolean shouldRestart() {
        // implement your own condition, for example, based on user input, flag, time, etc.
        return true; // always restart for now
    }

    /**
     * Calculates the spread percentage between two prices.
     */
    public static BigDecimal calculateSpread(BigDecimal shortPrice, BigDecimal longPrice) {
        BigDecimal avg = shortPrice.add(longPrice).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        return shortPrice.subtract(longPrice)
                .divide(avg, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculates spreads between Binance and MEXC coins.
     */
    public static List<Spread> calculateSpreadBetweenExchanges(List<CoinsResponse> binanceList, List<Datum> mexcList) {
        List<Spread> spreadList = new ArrayList<>();
        Map<String, Datum> mexcMap = new HashMap<>();
        for (Datum mexc : mexcList) {
            //Remove TRUMP coins, use only 1
            if (mexc.symbol != null) {
                if(!mexc.symbol.equals("TRUMP_USDT")) {
                    String normalized = normalizeSymbol(mexc.symbol);
                    mexcMap.put(normalized, mexc);
                }
            }
        }

        for (CoinsResponse binance : binanceList) {
            if (binance.symbol == null) continue;
            String normalizedBinanceSymbol = normalizeSymbol(binance.symbol);

            Datum mexc = mexcMap.get(normalizedBinanceSymbol);
            if (mexc == null) continue;

            try {
                BigDecimal binanceAsk = new BigDecimal(binance.askPrice);
                BigDecimal mexcAsk = BigDecimal.valueOf(mexc.ask1);

                boolean binanceIsShort = binanceAsk.compareTo(mexcAsk) > 0;
                BigDecimal shortPrice = binanceIsShort ? binanceAsk : mexcAsk;
                BigDecimal longPrice = binanceIsShort ? mexcAsk : binanceAsk;
                String shortExchange = binanceIsShort ? "Binance" : "Mexc";
                String longExchange = binanceIsShort ? "Mexc" : "Binance";
                String direction = String.format("%s Short %s / %s Long %s", shortExchange, shortPrice, longExchange, longPrice);

                BigDecimal spread = calculateSpread(shortPrice, longPrice);
                if (spread.compareTo(MIN_SPREAD_PERCENT) > 0) {
                    spreadList.add(Spread.builder()
                            .symbol(binance.symbol)
                            .longPrice(longPrice)
                            .shortPrice(shortPrice)
                            .shortExchange(shortExchange)
                            .longExchange(longExchange)
                            .spread(spread)
                            .build());
                    LOGGER.info(String.format("Symbol: %s | Direction: %s | Spread: %.4f%%", binance.symbol, direction, spread));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Invalid price for symbol: %s", binance.symbol), e);
            }
        }
        return spreadList;
    }

    /**
     * Normalizes a symbol by removing underscores and converting to uppercase.
     */
    private static String normalizeSymbol(String symbol) {
        return symbol.replace("_", "").toUpperCase();
    }

    /**
     * Checks the spread for a given symbol and logs if the opportunity is still valid.
     */
    public static boolean checkSpreadForSymbol(Spread spread) {
        boolean needToClose = true;
        try {
            // --- BINANCE ---
            LinkedHashMap<String, Object> binanceParams = new LinkedHashMap<>();
            binanceParams.put("symbol", spread.symbol);
            CoinsResponse binanceCoin = Utils.convertStringToPOJO(
                    BinanceFuturesClient.getCoin(binanceParams),
                    CoinsResponse.class
            );
            BigDecimal binanceAsk = new BigDecimal(binanceCoin.askPrice);

            // --- MEXC ---
            String mexcSymbol = MexcFuturesClient.toMexcSymbol(spread.symbol);
            Datum mexcCoin = MexcFuturesClient.getBookTicker(mexcSymbol).getData();
            BigDecimal mexcAsk = BigDecimal.valueOf(mexcCoin.ask1);

            BigDecimal optSpread;
            String direction;
            if (spread.longExchange.equals("Binance")){
                optSpread = calculateSpread(mexcAsk, binanceAsk);
                direction = String.format("%s Short %s / %s Long %s", spread.shortExchange, mexcAsk, spread.longExchange, binanceAsk);
            }
            else {
                optSpread = calculateSpread(binanceAsk, mexcAsk);
                direction = String.format("%s Short %s / %s Long %s", spread.shortExchange, binanceAsk, spread.longExchange, mexcAsk);
            }

            BigDecimal halfSpread = spread.spread.divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
            LOGGER.info(String.format("Symbol: %s | Direction: %s | Spread: %.4f%%", spread.symbol, direction, optSpread));
            if (optSpread.compareTo(halfSpread) > 0) {
                needToClose = false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error checking spread for %s", spread.symbol), e);
        }
        //return spread;
        return needToClose;
    }
}