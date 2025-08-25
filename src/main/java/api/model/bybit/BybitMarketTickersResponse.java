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
public class BybitMarketTickersResponse {
    @JsonProperty("retCode")
    public Integer retCode;
    @JsonProperty("retMsg")
    public String retMsg;
    @JsonProperty("result")
    public Result result;
    @JsonProperty("retExtInfo")
    public Object retExtInfo;
    @JsonProperty("time")
    public Long time;
}
