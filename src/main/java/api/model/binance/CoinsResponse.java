package api.model.binance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinsResponse {
    @JsonProperty("symbol")
    public String symbol;
    @JsonProperty("bidPrice")
    public String bidPrice;
    @JsonProperty("bidQty")
    public String bidQty;
    @JsonProperty("askPrice")
    public String askPrice;
    @JsonProperty("askQty")
    public String askQty;
    @JsonProperty("time")
    public Long time;
    @JsonProperty("lastUpdateId")
    public Long lastUpdateId;
}
