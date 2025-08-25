package api.model.bybit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListResponse {
    @JsonProperty("symbol")
    public String symbol;
    @JsonProperty("lastPrice")
    public Float lastPrice;
    @JsonProperty("indexPrice")
    public String indexPrice;
    @JsonProperty("markPrice")
    public String markPrice;
    @JsonProperty("prevPrice24h")
    public String prevPrice24h;
    @JsonProperty("price24hPcnt")
    public String price24hPcnt;
    @JsonProperty("highPrice24h")
    public String highPrice24h;
    @JsonProperty("lowPrice24h")
    public String lowPrice24h;
    @JsonProperty("prevPrice1h")
    public String prevPrice1h;
    @JsonProperty("openInterest")
    public String openInterest;
    @JsonProperty("openInterestValue")
    public String openInterestValue;
    @JsonProperty("turnover24h")
    public String turnover24h;
    @JsonProperty("volume24h")
    public String volume24h;
    @JsonProperty("fundingRate")
    public String fundingRate;
    @JsonProperty("nextFundingTime")
    public String nextFundingTime;
    @JsonProperty("predictedDeliveryPrice")
    public String predictedDeliveryPrice;
    @JsonProperty("basisRate")
    public String basisRate;
    @JsonProperty("deliveryFeeRate")
    public String deliveryFeeRate;
    @JsonProperty("deliveryTime")
    public String deliveryTime;
    @JsonProperty("ask1Size")
    public String ask1Size;
    @JsonProperty("bid1Price")
    public String bid1Price;
    @JsonProperty("ask1Price")
    public String ask1Price;
    @JsonProperty("bid1Size")
    public String bid1Size;
    @JsonProperty("basis")
    public String basis;
    @JsonProperty("preOpenPrice")
    public String preOpenPrice;
    @JsonProperty("preQty")
    public String preQty;
    @JsonProperty("curPreListingPhase")
    public String curPreListingPhase;



    //For close order
    @JsonProperty("leverage")
    public String leverage;
    @JsonProperty("autoAddMargin")
    public Integer autoAddMargin;
    @JsonProperty("avgPrice")
    public String avgPrice;
    @JsonProperty("liqPrice")
    public String liqPrice;
    @JsonProperty("riskLimitValue")
    public String riskLimitValue;
    @JsonProperty("takeProfit")
    public String takeProfit;
    @JsonProperty("positionValue")
    public String positionValue;
    @JsonProperty("isReduceOnly")
    public Boolean isReduceOnly;
    @JsonProperty("positionIMByMp")
    public String positionIMByMp;
    @JsonProperty("tpslMode")
    public String tpslMode;
    @JsonProperty("riskId")
    public Integer riskId;
    @JsonProperty("trailingStop")
    public String trailingStop;
    @JsonProperty("liqPriceByMp")
    public String liqPriceByMp;
    @JsonProperty("unrealisedPnl")
    public String unrealisedPnl;
    @JsonProperty("adlRankIndicator")
    public Integer adlRankIndicator;
    @JsonProperty("cumRealisedPnl")
    public String cumRealisedPnl;
    @JsonProperty("positionMM")
    public String positionMM;
    @JsonProperty("createdTime")
    public String createdTime;
    @JsonProperty("positionIdx")
    public Integer positionIdx;
    @JsonProperty("positionIM")
    public String positionIM;
    @JsonProperty("positionMMByMp")
    public String positionMMByMp;
    @JsonProperty("seq")
    public Long seq;
    @JsonProperty("updatedTime")
    public String updatedTime;
    @JsonProperty("side")
    public String side;
    @JsonProperty("bustPrice")
    public String bustPrice;
    @JsonProperty("positionBalance")
    public String positionBalance;
    @JsonProperty("leverageSysUpdatedTime")
    public String leverageSysUpdatedTime;
    @JsonProperty("curRealisedPnl")
    public String curRealisedPnl;
    @JsonProperty("size")
    public Double size;
    @JsonProperty("positionStatus")
    public String positionStatus;
    @JsonProperty("mmrSysUpdatedTime")
    public String mmrSysUpdatedTime;
    @JsonProperty("stopLoss")
    public String stopLoss;
    @JsonProperty("tradeMode")
    public Integer tradeMode;
    @JsonProperty("sessionAvgPrice")
    public String sessionAvgPrice;
}
