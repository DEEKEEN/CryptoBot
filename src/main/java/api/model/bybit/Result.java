package api.model.bybit;

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
public class Result {
    @JsonProperty("nextPageCursor")
    public Object nextPageCursor;
    @JsonProperty("category")
    public String category;
    @JsonProperty("list")
    public List<ListResponse> list;
}
