package api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Spread {
    public String symbol;
    public BigDecimal shortPrice;
    public BigDecimal longPrice;
    public BigDecimal netSpread;
    public String shortExchange;
    public String longExchange;
    public BigDecimal spread;
    public String quantity;

    public static Spread getMaxSpread(List<Spread> spreads) {
        return spreads.stream()
                .max(Comparator.comparing(s -> s.spread))
                .orElse(null); // handle empty list safely
    }
}
