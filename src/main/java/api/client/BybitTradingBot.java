package api.client;


import api.model.Spread;
import api.model.bybit.BybitMarketTickersResponse;
import api.model.bybit.ListResponse;
import api.utils.Utils;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiPositionRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class BybitTradingBot {
    private static final Logger LOGGER = Logger.getLogger(BybitTradingBot.class.getName());
    private static final String API_KEY = "T0faTcYQ8fVFg6JKgH";
    private static final String SECRET_KEY = "t8Mv36HxiFihvTmv2CRGOAmi6zEhlJm0Jcwv";
    private static final BybitApiClientFactory factory = BybitApiClientFactory.newInstance(API_KEY, SECRET_KEY);
    private static final BybitApiTradeRestClient tradeClient = factory.newTradeRestClient();
    private static final BybitApiPositionRestClient positionClient = factory.newPositionRestClient();
    private static final BybitApiMarketRestClient marketClient = factory.newMarketDataRestClient();

    public static void main(String[] args) throws InterruptedException {
        api.model.Spread spread = api.model.Spread.builder().shortExchange("Binance").longExchange("Bybit")
                .shortPrice(BigDecimal.valueOf(10.0)).longPrice(BigDecimal.valueOf(10.0)).symbol("ESPORTSUSDT").quantity("65").build();
        makeOrder3(spread);
        System.out.println(LocalDateTime.now());
        Thread.sleep(333);
        closeOrder2(spread);
        System.out.println(LocalDateTime.now());
    }

    public static BybitMarketTickersResponse getAllCoins() {
        MarketDataRequest request = MarketDataRequest.builder()
                .category(CategoryType.LINEAR)
                .build();

        return Utils.convertStringToPOJO(
                Utils.convertPOJOToString(marketClient.getMarketTickers(request)), BybitMarketTickersResponse.class);
    }

    public static BybitMarketTickersResponse getCoin(Spread spread) {
        MarketDataRequest request = MarketDataRequest.builder()
                .symbol(spread.getSymbol()) // or remove this to get all
                .category(CategoryType.LINEAR)
                .build();
        return Utils.convertStringToPOJO(
                Utils.convertPOJOToString(marketClient.getMarketTickers(request)), BybitMarketTickersResponse.class);
    }

    public static void makeOrder3(Spread spread) {
        try {
            String symbol = spread.getSymbol();
            BigDecimal markPrice;
            if(spread.shortExchange.equals("Bybit")){
                markPrice = spread.shortPrice;
            } else {
                markPrice = spread.longPrice;
            }
            // e. Calculate PostOnly LIMIT price
            boolean isSell = "Bybit".equals(spread.getShortExchange());

            // f. Prepare order
            LinkedHashMap<String, Object> orderParams = new LinkedHashMap<>();
            orderParams.put("symbol", symbol);
            orderParams.put("orderType", "Limit");
            orderParams.put("price", markPrice.toPlainString());
            orderParams.put("side", isSell ? "Sell" : "Buy");
            orderParams.put("qty", spread.getQuantity());
            orderParams.put("category", CategoryType.LINEAR);
            orderParams.put("isLeverage", true);
            orderParams.put("marginMode", "ISOLATED");
            orderParams.put("positionIdx", 0);
            // ✅ Set timeInForce to GTC (default = persist until filled or canceled)
            orderParams.put("timeInForce", "GTC");
            System.out.println("Bybit order model " + LocalDateTime.now() + "  " + orderParams);
            Object result = tradeClient.createOrder(orderParams);
            LOGGER.info(LocalDateTime.now() + " Bybit Позиція відкрита: " + result);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error creating Maker order: " + e.getMessage(), e);
        }
    }

    public static void closeOrder2(Spread spread) {
        try {
            String symbol = spread.getSymbol();

            // a. Get open position info
            PositionDataRequest positionRequest = PositionDataRequest.builder()
                    .symbol(symbol)
                    .category(CategoryType.LINEAR)
                    .build();

            BybitMarketTickersResponse positionInfo = Utils.convertStringToPOJO(
                    Utils.convertPOJOToString(positionClient.getPositionInfo(positionRequest)),
                    BybitMarketTickersResponse.class
            );

            if (positionInfo == null || positionInfo.result == null || positionInfo.result.list.isEmpty()) {
                LOGGER.warning("No open position for symbol: " + symbol);
                return;
            }

            double positionAmt = positionInfo.result.list.get(0).getSize();

            if (positionAmt > 0) {
                // b. Get mark price
                MarketDataRequest priceRequest = MarketDataRequest.builder()
                        .symbol(symbol)
                        .category(CategoryType.LINEAR)
                        .build();

                BybitMarketTickersResponse priceResponse = Utils.convertStringToPOJO(
                        Utils.convertPOJOToString(marketClient.getMarketTickers(priceRequest)),
                        BybitMarketTickersResponse.class
                );
                System.out.println(priceResponse);

                BigDecimal markPrice;
                if(spread.longExchange.equals("Bybit")){
                    markPrice = new BigDecimal(String.valueOf(priceResponse.result.getList().get(0).getBid1Price()));
                }else {
                    markPrice = new BigDecimal(String.valueOf(priceResponse.result.getList().get(0).getAsk1Price()));
                }

                // c. Get tickSize and precision from instrumentsInfo
                MarketDataRequest instrumentRequest = MarketDataRequest.builder()
                        .symbol(symbol)
                        .category(CategoryType.LINEAR)
                        .build();

                String instrumentsInfoStr = Utils.convertPOJOToString(marketClient.getInstrumentsInfo(instrumentRequest));
                JSONObject instrumentJson = new JSONObject(instrumentsInfoStr);
                JSONObject priceFilter = instrumentJson.getJSONObject("result")
                        .getJSONArray("list")
                        .getJSONObject(0)
                        .getJSONObject("priceFilter");

                BigDecimal tickSize = new BigDecimal(priceFilter.getString("tickSize"));
                int pricePrecision = tickSize.stripTrailingZeros().scale();

                // d. Set PostOnly limit price slightly better than market
                boolean isSell = "Bybit".equals(spread.getShortExchange());
                BigDecimal limitPrice = isSell
                        ? markPrice.subtract(tickSize) // Sell closing → post below to avoid match
                        : markPrice.add(tickSize);     // Buy closing → post above to avoid match


                limitPrice = markPrice.setScale(pricePrecision, RoundingMode.DOWN);

                // e. Build close limit order
                LinkedHashMap<String, Object> closeParams = new LinkedHashMap<>();
                closeParams.put("symbol", symbol);
                closeParams.put("orderType", "Limit");
                closeParams.put("price", limitPrice.toPlainString());
                closeParams.put("qty", spread.getQuantity());
                closeParams.put("reduceOnly", true);
                closeParams.put("side", isSell ? "Buy" : "Sell"); // opposite direction to close
                closeParams.put("category", CategoryType.LINEAR);
                closeParams.put("isLeverage", true);
                closeParams.put("marginMode", "ISOLATED");
                closeParams.put("positionIdx", 0);
                // ✅ Set timeInForce to GTC (default = persist until filled or canceled)
                closeParams.put("timeInForce", "GTC");

                System.out.println(LocalDateTime.now() + " Binance close order model " + closeParams);

                // f. Submit order
                Object response = tradeClient.createOrder(closeParams);
                LOGGER.info("✅ Maker (PostOnly) close order placed: " + response);
            } else {
                LOGGER.info("ℹ️ Position size is zero. Nothing to close.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error closing position as Maker: " + e.getMessage(), e);
        }
    }

    public static void closeOrder3(Spread spread) {
        try {
            String symbol = spread.getSymbol();

                BigDecimal markPrice;
                if(spread.longExchange.equals("Bybit")){
                    markPrice = spread.longPrice;
                }else {
                    markPrice = spread.shortPrice;
                }

                // d. Set PostOnly limit price slightly better than market
                boolean isSell = "Bybit".equals(spread.getShortExchange());


                // e. Build close limit order
                LinkedHashMap<String, Object> closeParams = new LinkedHashMap<>();
                closeParams.put("symbol", symbol);
                closeParams.put("orderType", "Limit");
                closeParams.put("price", markPrice.toPlainString());
                closeParams.put("qty", spread.getQuantity());
                closeParams.put("reduceOnly", true);
                closeParams.put("side", isSell ? "Buy" : "Sell"); // opposite direction to close
                closeParams.put("category", CategoryType.LINEAR);
                closeParams.put("isLeverage", true);
                closeParams.put("marginMode", "ISOLATED");
                closeParams.put("positionIdx", 0);
                // ✅ Set timeInForce to GTC (default = persist until filled or canceled)
                closeParams.put("timeInForce", "GTC");
                System.out.println(LocalDateTime.now() + " Binance close order model " + closeParams);
                // f. Submit order
                Object response = tradeClient.createOrder(closeParams);
                LOGGER.info(LocalDateTime.now() + " ✅ Maker close order sent to Bybit: " + response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error closing position as Maker: " + e.getMessage(), e);
        }
    }

    // 5. Precision helper
    public static int getPrecisionForSymbol(String symbol) {
        MarketDataRequest request = MarketDataRequest.builder()
                .symbol(symbol)
                .category(CategoryType.LINEAR)
                .build();

        // Cast Object to String and parse
        String response = Utils.convertPOJOToString(marketClient.getInstrumentsInfo(request));

        String stepSize = new JSONObject(response)
                .getJSONObject("result")
                .getJSONArray("list")
                .getJSONObject(0)
                .getJSONObject("lotSizeFilter")
                .getString("qtyStep");

        return new BigDecimal(stepSize).stripTrailingZeros().scale();
    }

    public static void setLeverageForAllCoins() {

        List<ListResponse> response = getAllCoins().result.getList();

        // Використаємо пул потоків (підбери під свій ліміт API, напр. 5–10)
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ListResponse r : response) {
            CompletableFuture<Void> fut = CompletableFuture.runAsync(() -> {
                try {
                    PositionDataRequest leverageRequest = PositionDataRequest.builder()
                            .symbol(r.getSymbol())
                            .buyLeverage("1")
                            .sellLeverage("1")
                            .category(CategoryType.LINEAR)
                            .build();

                    positionClient.setPositionLeverage(leverageRequest); // саме setLeverage, не getPositionInfo
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

