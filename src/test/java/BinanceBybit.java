import api.client.BinanceFuturesClient;
import api.client.BybitTradingBot;
import api.formatter.PlainFormatter;
import api.model.Spread;
import api.model.SpreadCheckResult;
import api.model.binance.CoinsResponse;
import api.model.bybit.BybitMarketTickersResponse;
import api.model.bybit.ListResponse;
import api.model.bybit.Result;
import api.utils.Utils;
import com.codeborne.selenide.Configuration;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static api.client.BybitTradingBot.getAllCoins;

public class BinanceBybit {
    static {
        Logger rootLogger = Logger.getLogger("");
        Arrays.stream(rootLogger.getHandlers()).forEach(handler -> handler.setFormatter(new PlainFormatter()));
    }

    private static final Logger LOGGER = Logger.getLogger(BinanceBybit.class.getName());
    private static final BigDecimal MIN_SPREAD_PERCENT = new BigDecimal("0.9");
    private static final BigDecimal THRESHOLD = new BigDecimal("0.4");
    // Константи фі (тейкер), у частках від 1
    private static final BigDecimal BINANCE_TAKER_FEE = new BigDecimal("0.001");  // 0.1%
    private static final BigDecimal BYBIT_TAKER_FEE   = new BigDecimal("0.0022");  // 0.22%
    private static final int SCALE = 10;
    private static final BigDecimal BASE_NOTIONAL_USDT = new BigDecimal("10");
    // Пул для паралельної ініціалізації (піджени під свій rate limit)
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);
    private static final ConcurrentHashMap<String, Integer> PRECISION = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        warmupAllPrecisions();
        setLeverageForAllCoins();
        do {
            startMakingMoney();
        } while (shouldRestart());
    }

    public static void setLeverageForAllCoins(){
        System.out.println("Start setting leverage for all coins " + LocalDateTime.now());

        CompletableFuture<Void> binanceLeverage =
                CompletableFuture.runAsync(BinanceFuturesClient::setLeverageForAllCoins);
        CompletableFuture<Void> bybitLeverage =
                CompletableFuture.runAsync(BybitTradingBot::setLeverageForAllCoins);

        CompletableFuture.allOf(binanceLeverage, bybitLeverage).join();
        System.out.println("Leverage was set to all coins " + LocalDateTime.now());
    }

    public static void startMakingMoney() {
        CompletableFuture<List<CoinsResponse>> binanceFutureCoins = CompletableFuture.supplyAsync(() ->
                Utils.convertJsonStringToModel(
                        BinanceFuturesClient.getAllCoins(),
                        new TypeReference<List<CoinsResponse>>() {}
                )
        );

        CompletableFuture<List<ListResponse>> bybitFutureCoins = CompletableFuture.supplyAsync(() ->
                getAllCoins().result.getList()
        );

        System.out.println("Starting collecting coins " + LocalDateTime.now());
        CompletableFuture.allOf(binanceFutureCoins, bybitFutureCoins).join();
        System.out.println("Coins collected  " + LocalDateTime.now());

        List<CoinsResponse> binanceCoins = binanceFutureCoins.join();
        List<ListResponse> bybitCoins = bybitFutureCoins.join();

        System.out.println("Starting collecting spread " + LocalDateTime.now());
        List<Spread> spreadList = calculateSpreadBetweenExchanges2(binanceCoins, bybitCoins);
        Spread spread = Spread.getMaxSpread(spreadList);

        if (!spreadList.isEmpty()) {
            spread.setQuantity(getQuantity2(spread));
            CompletableFuture<Void> bybitFuture = CompletableFuture.runAsync(() -> BybitTradingBot.makeOrder3(spread));
            CompletableFuture<Void> binanceFuture = CompletableFuture.runAsync(() -> BinanceFuturesClient.makeOrder2(spread));
            CompletableFuture.allOf(bybitFuture, binanceFuture).join(); // Waits for both to complete
        }

        SpreadCheckResult spreadCheckResult = SpreadCheckResult.builder().needToClose(false).build();
        if (!spreadList.isEmpty()) {
            do {
                Spread maxSpread = Spread.getMaxSpread(spreadList);
                spreadCheckResult = checkSpreadForSymbol3(maxSpread);
                if (!spreadList.isEmpty()) {
                    LOGGER.info("SPREAD status is " + spreadCheckResult.isNeedToClose());
                }
            } while (!spreadCheckResult.isNeedToClose());
        }

        if (!spreadList.isEmpty()) {

            SpreadCheckResult finalSpreadCheckResult = spreadCheckResult;
            CompletableFuture<Void> bybitFuture = CompletableFuture.runAsync(() -> BybitTradingBot.closeOrder3(finalSpreadCheckResult.getSpread()));
            CompletableFuture<Void> binanceFuture = CompletableFuture.runAsync(() -> BinanceFuturesClient.closeOrder3(finalSpreadCheckResult.getSpread()));
            CompletableFuture.allOf(bybitFuture, binanceFuture).join(); // Waits for both to complete
            int a=0;
        }
    }

    private static boolean shouldRestart() {
        return true;
    }

    public static BigDecimal calculateSpread(BigDecimal shortPrice, BigDecimal longPrice) {
        BigDecimal avg = shortPrice.add(longPrice).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        return shortPrice.subtract(longPrice)
                .divide(avg, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // Брутто-спред у %, формула: (short - long) / avg * 100
    private static BigDecimal grossSpreadPct(BigDecimal shortPx, BigDecimal longPx) {
        BigDecimal avg = shortPx.add(longPx).divide(new BigDecimal("2"), 16, RoundingMode.HALF_UP);
        return shortPx.subtract(longPx)
                .divide(avg, 16, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal toPct(BigDecimal x) { return x.multiply(new BigDecimal("100")); }

    // Нетто-спред у % після комісій двох ніг
    private static BigDecimal netSpreadPct(BigDecimal grossPct, BigDecimal shortFee, BigDecimal longFee) {
        BigDecimal totalFeesPct = toPct(shortFee.add(longFee)); // 0.0005+0.0011=0.0016 → 0.16%
        return grossPct.subtract(totalFeesPct);
    }

    // ЯДРО: рахуємо обидва варіанти та беремо кращий
    public static List<Spread> calculateSpreadBetweenExchanges2(List<CoinsResponse> binanceList,
                                                               List<ListResponse> bybitList) {
        List<Spread> out = new ArrayList<>();

        Map<String, ListResponse> bybitMap = new HashMap<>();
        for (ListResponse b : bybitList) {
            if (b.getSymbol() != null) bybitMap.put(normalizeSymbol(b.getSymbol()), b);
        }

        for (CoinsResponse bn : binanceList) {
            if (bn.symbol == null) continue;
            if (bn.symbol.equals("WLFIUSDT")) continue; // skip this coin and move to next
            ListResponse by = bybitMap.get(normalizeSymbol(bn.symbol));
            if (by == null) continue;

            try {
                // ВАЖЛИВО: bid = short (sell), ask = long (buy)
                BigDecimal binanceBid = new BigDecimal(bn.bidPrice);
                BigDecimal binanceAsk = new BigDecimal(bn.askPrice);
                BigDecimal bybitBid   = new BigDecimal(by.getBid1Price());
                BigDecimal bybitAsk   = new BigDecimal(by.getAsk1Price());

                // Варіант A: Short Binance (bid) / Long Bybit (ask)
                BigDecimal grossA = grossSpreadPct(binanceBid, bybitAsk);
                BigDecimal netA   = netSpreadPct(grossA, BINANCE_TAKER_FEE, BYBIT_TAKER_FEE);

                // Варіант B: Short Bybit (bid) / Long Binance (ask)
                BigDecimal grossB = grossSpreadPct(bybitBid, binanceAsk);
                BigDecimal netB   = netSpreadPct(grossB, BYBIT_TAKER_FEE, BINANCE_TAKER_FEE);

                boolean useA = netA.compareTo(netB) >= 0;
                BigDecimal net   = useA ? netA   : netB;
                BigDecimal gross = useA ? grossA : grossB;

                if (gross.compareTo(MIN_SPREAD_PERCENT) > 0) {
                    BigDecimal shortPx = useA ? binanceBid : bybitBid;
                    BigDecimal longPx  = useA ? bybitAsk   : binanceAsk;
                    String shortEx     = useA ? "Binance"  : "Bybit";
                    String longEx      = useA ? "Bybit"    : "Binance";

                    out.add(Spread.builder()
                            .symbol(bn.symbol)
                            .shortExchange(shortEx)
                            .longExchange(longEx)
                            .shortPrice(shortPx)
                            .longPrice(longPx)
                            .netSpread(net)       // нетто-спред після фі, %
                            .spread(gross)        // брутто-спред, %
                            .build());

                    LOGGER.info(String.format(
                            LocalDateTime.now() + " Symbol: %s | %s Short %.6f / %s Long %.6f | Gross %.4f%% | Net %.4f%%",
                            bn.symbol, shortEx, shortPx, longEx, longPx, gross, net));
                    System.out.println("Spread collected " + LocalDateTime.now());

                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Invalid price for symbol: " + bn.symbol, e);
            }
        }
        return out;
    }


    private static String normalizeSymbol(String symbol) {
        return symbol.replace("_", "").toUpperCase();
    }
    
    public static SpreadCheckResult checkSpreadForSymbol3(Spread spread) {
        boolean needToClose = true;
        Spread updatedSpread = spread;

        try {
            // --- BINANCE (async) ---
            CompletableFuture<CoinsResponse> binanceFuture = CompletableFuture.supplyAsync(() -> {
                LinkedHashMap<String, Object> params = new LinkedHashMap<>();
                params.put("symbol", spread.symbol);
                return Utils.convertStringToPOJO(
                        BinanceFuturesClient.getCoin(params),
                        CoinsResponse.class
                );
            });

            // --- BYBIT (async) ---
            CompletableFuture<BybitMarketTickersResponse> bybitFuture =
                    CompletableFuture.supplyAsync(() -> BybitTradingBot.getCoin(spread));

            CompletableFuture.allOf(binanceFuture, bybitFuture).join();

            CoinsResponse binance = binanceFuture.join();
            BybitMarketTickersResponse bybit = bybitFuture.join();

            // IMPORTANT: bid = short (sell), ask = long (buy)
            BigDecimal binanceBid = new BigDecimal(binance.bidPrice);
            BigDecimal binanceAsk = new BigDecimal(binance.askPrice);
            BigDecimal bybitBid   = new BigDecimal(bybit.result.getList().get(0).getBid1Price());
            BigDecimal bybitAsk   = new BigDecimal(bybit.result.getList().get(0).getAsk1Price());

            // Build current prices according to the DIRECTION saved in `spread`
            BigDecimal shortPx, longPx;
            BigDecimal shortFee, longFee;
            String shortEx, longEx;

            if ("Binance".equalsIgnoreCase(spread.shortExchange)) {
                shortPx  = binanceBid;  shortFee = BINANCE_TAKER_FEE;
                longPx   = bybitAsk;    longFee  = BYBIT_TAKER_FEE;
                shortEx  = "Binance";
                longEx   = "Bybit";
            } else {
                shortPx  = bybitBid;    shortFee = BYBIT_TAKER_FEE;
                longPx   = binanceAsk;  longFee  = BINANCE_TAKER_FEE;
                shortEx  = "Bybit";
                longEx   = "Binance";
            }

            // Compute spreads
            BigDecimal grossNow = grossSpreadPct(shortPx, longPx);
            BigDecimal netNow   = netSpreadPct(grossNow, shortFee, longFee);

            LOGGER.info(String.format(
                    LocalDateTime.now() + " Symbol: %s | %s Short %s / %s Long %s | Gross: %.4f%% | Net: %.4f%% | Threshold: %.4f%%",
                    spread.symbol, shortEx, shortPx.toPlainString(), longEx, longPx.toPlainString(),
                    grossNow, netNow, THRESHOLD));

            if (grossNow.compareTo(THRESHOLD) > 0) {
                needToClose = false;
            }

            // Побудуємо оновлений Spread
            updatedSpread = Spread.builder()
                    .symbol(spread.symbol)
                    .shortExchange(shortEx)
                    .longExchange(longEx)
                    .shortPrice(shortPx)
                    .longPrice(longPx)
                    .spread(grossNow)
                    .netSpread(netNow)
                    .build();
            updatedSpread.setQuantity(getQuantity2(updatedSpread));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking spread for " + spread.symbol, e);
        }

        return new SpreadCheckResult(updatedSpread, needToClose);
    }

    public static String getQuantity2(Spread spread) {
        BigDecimal quantity = BigDecimal.valueOf(SCALE).divide(spread.getShortPrice(), SCALE, RoundingMode.DOWN);
        // Set scale and return as string
        return quantity.setScale(PRECISION.get(spread.getSymbol()), RoundingMode.DOWN).toPlainString();
    }

    /** Прогріваємо кеш precision для всіх монет. Викликаємо при старті. */
    public static void warmupAllPrecisions() {

        System.out.println("Warmup start " + LocalDateTime.now());
        List<ListResponse> response = getAllCoins().result.getList();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ListResponse s : response) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    int prec = BybitTradingBot.getPrecisionForSymbol(s.getSymbol());
                    PRECISION.put(s.getSymbol(), prec);
                } catch (Exception e) {
                    System.err.println("❌ Failed to get precision for " + s + " → " + e.getMessage());
                }
            }, EXECUTOR));
        }
        // дочекаємось завершення всіх тасків
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Warmup end   " + LocalDateTime.now());
    }
}