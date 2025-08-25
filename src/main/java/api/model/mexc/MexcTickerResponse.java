package api.model.mexc;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MexcTickerResponse {
    @JsonProperty("success")
    public Boolean success;
    @JsonProperty("code")
    public Integer code;
    @JsonProperty("data")
    public List<Datum> data;
}