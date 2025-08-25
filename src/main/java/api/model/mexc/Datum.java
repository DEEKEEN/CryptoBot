package api.model.mexc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Datum {
    @JsonProperty("contractId")
    public Integer contractId;
    @JsonProperty("symbol")
    public String symbol;
    @JsonProperty("lastPrice")
    public Float lastPrice;
    @JsonProperty("bid1")
    public Float bid1;
    @JsonProperty("ask1")
    public Float ask1;
    @JsonProperty("volume24")
    public Long volume24;
    @JsonProperty("amount24")
    public Float amount24;
    @JsonProperty("holdVol")
    public Integer holdVol;
    @JsonProperty("lower24Price")
    public Float lower24Price;
    @JsonProperty("high24Price")
    public Float high24Price;
    @JsonProperty("riseFallRate")
    public Float riseFallRate;
    @JsonProperty("riseFallValue")
    public Float riseFallValue;
    @JsonProperty("indexPrice")
    public Float indexPrice;
    @JsonProperty("fairPrice")
    public Float fairPrice;
    @JsonProperty("fundingRate")
    public Float fundingRate;
    @JsonProperty("maxBidPrice")
    public Float maxBidPrice;
    @JsonProperty("minAskPrice")
    public Float minAskPrice;
    @JsonProperty("timestamp")
    public Long timestamp;
    @JsonProperty("riseFallRates")
    public RiseFallRates riseFallRates;
    @JsonProperty("riseFallRatesOfTimezone")
    public List<Float> riseFallRatesOfTimezone;
}
