package api.client;

import api.model.Spread;
import api.model.binance.CoinsResponse;

import api.utils.Utils;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONArray;
import org.json.JSONObject;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class BinanceFuturesClient {
    private static final Logger LOGGER = Logger.getLogger(BinanceFuturesClient.class.getName());

    private static final String API_KEY = "wC5b34fDjPpUHDPWkNykHW1Sh582oYEAZovHH3SduUw2KFWc7ao6pLItZ4uia8Ti";
    private static final String SECRET_KEY = "9mPO1jUgqxECEkEmkneBqSmj34uziJAZZEgaeElPzab25Xe8eeJHnVW3Y99JsPUy";
    private static final UMFuturesClientImpl client = new UMFuturesClientImpl(API_KEY, SECRET_KEY);
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);
    private static final int SCALE = 10;

    public static void main(String[] args) throws InterruptedException {
        api.model.Spread spread = api.model.Spread.builder().shortExchange("Binance").longExchange("Bybit")
                .shortPrice(BigDecimal.valueOf(0.511800)).longPrice(BigDecimal.valueOf(0.505500)).symbol("XPLUSDT").build();
        //makeOrder(spread);
        Thread.sleep(333);
        closeOrder3(spread);
    }


    public static String getAllCoins() {
        String a = client.market().bookTicker(new LinkedHashMap<>());
        return a;
    }

    public static String getCoin(LinkedHashMap<String, Object> param) {
        return client.market().bookTicker(param);
    }

    public static void makeOrder2(Spread spread) {
        try {
            String symbol = spread.getSymbol();

            BigDecimal markPrice;
            if(spread.shortExchange.equals("Binance")){
                markPrice = spread.shortPrice;
            }else {
                markPrice = spread.longPrice;
            }

            // 4. Determine maker limit price
            boolean isSell = "Binance".equals(spread.getShortExchange());

            LinkedHashMap<String, Object> orderParams = new LinkedHashMap<>();
            orderParams.put("symbol", symbol);
            orderParams.put("type", "LIMIT");
            orderParams.put("side", isSell ? "SELL" : "BUY");
            orderParams.put("price", markPrice.toPlainString());
            orderParams.put("quantity", spread.getQuantity());
            orderParams.put("timeInForce", "GTC"); //
            orderParams.put("marginType", "ISOLATED");
            //orderParams.put("recvWindow", 5000);                 // захист від лагів
            System.out.println("Binance order model " + LocalDateTime.now() + "  " + orderParams);

            // 2) відправляємо асинхронно, не блокуємось
            CompletableFuture
                    .supplyAsync(() -> client.account().newOrder(orderParams), EXEC)
                    .thenAccept(res -> LOGGER.info(LocalDateTime.now() + " Binance Позиція відкрита: " + res))
                    .exceptionally(ex -> { LOGGER.severe("Order failed: " + ex); return null; });

//            // 6. Send order
//            String result = client.account().newOrder(orderParams);
//            System.out.println(LocalDateTime.now() + " Binance Позиція відкрита:");
//            System.out.println(result);

        } catch (Exception e) {
            System.err.println("❌ Помилка при створенні ордера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void closeOrder3(Spread spread) {
        try {
            String symbol = spread.getSymbol();

            BigDecimal markPrice;
            if(spread.longExchange.equals("Binance")){
                markPrice = spread.longPrice;
            }else {
                markPrice = spread.shortPrice;
            }

            boolean isClosingShort = "Binance".equals(spread.getShortExchange());

            // 5. Build Maker-style PostOnly LIMIT close order
            LinkedHashMap<String, Object> closeParams = new LinkedHashMap<>();
            closeParams.put("symbol", symbol);
            closeParams.put("type", "LIMIT");
            closeParams.put("price", markPrice.toPlainString());
            closeParams.put("quantity", spread.getQuantity());
            closeParams.put("reduceOnly", "true");
            closeParams.put("marginType", "ISOLATED");
            closeParams.put("timeInForce", "GTC"); // ✅ PostOnly
            closeParams.put("side", isClosingShort ? "BUY" : "SELL"); // opposite of open position

            System.out.println(closeParams);
            String response = client.account().newOrder(closeParams);
            System.out.println(LocalDateTime.now() + " ✅ Maker close order sent to Binance:");
            System.out.println(response);

        } catch (Exception e) {
            System.err.println("❌ Error closing Binance position as Maker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int getPrecisionForSymbol(String symbol) {
        String response = client.market().exchangeInfo(); // ⬅ без параметрів

        JSONObject json = new JSONObject(response);
        JSONArray symbols = json.getJSONArray("symbols");

        for (int i = 0; i < symbols.length(); i++) {
            JSONObject sym = symbols.getJSONObject(i);
            if (symbol.equals(sym.getString("symbol"))) {
                JSONArray filters = sym.getJSONArray("filters");
                for (int j = 0; j < filters.length(); j++) {
                    JSONObject filter = filters.getJSONObject(j);
                    if ("LOT_SIZE".equals(filter.getString("filterType"))) {
                        String stepSize = filter.getString("stepSize");
                        return getDecimalPlaces(stepSize);
                    }
                }
            }
        }

        throw new RuntimeException("StepSize не знайдено для символу " + symbol);
    }

    private static int getDecimalPlaces(String stepSize) {
        BigDecimal bd = new BigDecimal(stepSize);
        return Math.max(0, bd.stripTrailingZeros().scale());
    }

    public static BigDecimal getTickSize(String symbol) {
        try {
            String exchangeInfoStr = client.market().exchangeInfo();
            JSONObject json = new JSONObject(exchangeInfoStr);
            JSONArray symbols = json.getJSONArray("symbols");

            for (int i = 0; i < symbols.length(); i++) {
                JSONObject s = symbols.getJSONObject(i);
                if (s.getString("symbol").equals(symbol)) {
                    JSONArray filters = s.getJSONArray("filters");
                    for (int j = 0; j < filters.length(); j++) {
                        JSONObject filter = filters.getJSONObject(j);
                        if ("PRICE_FILTER".equals(filter.getString("filterType"))) {
                            return new BigDecimal(filter.getString("tickSize"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching tickSize: " + e.getMessage());
            e.printStackTrace();
        }

        return BigDecimal.valueOf(0.00001); // fallback default (not recommended long term)
    }



    public static void setLeverageForAllCoins() {
        List<CoinsResponse> response = Utils.convertJsonStringToModel(
                                BinanceFuturesClient.getAllCoins(),
                                new TypeReference<List<CoinsResponse>>() {}
                        );

        // Використаємо пул потоків (підбери під свій ліміт API, напр. 5–10)
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (CoinsResponse r : response) {
            CompletableFuture<Void> fut = CompletableFuture.runAsync(() -> {
                try {
                    // 3. Set leverage
                    LinkedHashMap<String, Object> leverageParams = new LinkedHashMap<>();
                    leverageParams.put("symbol", r.getSymbol());
                    leverageParams.put("leverage", 1);
                    client.account().changeInitialLeverage(leverageParams);
                } catch (Exception e) {
                    LOGGER.warning("❌ Failed to set leverage for " + r.getSymbol() + " → " + e.getMessage());
                }
            }, executor);

            futures.add(fut);
        }

        // Чекаємо завершення всіх запитів
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

    }
}
