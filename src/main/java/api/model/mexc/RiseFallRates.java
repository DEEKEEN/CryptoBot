package api.model.mexc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiseFallRates {
    @JsonProperty("zone")
    public String zone;
    @JsonProperty("r")
    public Float r;
    @JsonProperty("v")
    public Float v;
    @JsonProperty("r7")
    public Float r7;
    @JsonProperty("r30")
    public Float r30;
    @JsonProperty("r90")
    public Float r90;
    @JsonProperty("r180")
    public Float r180;
    @JsonProperty("r365")
    public Float r365;
}
