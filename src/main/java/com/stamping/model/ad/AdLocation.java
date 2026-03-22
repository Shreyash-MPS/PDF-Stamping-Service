package com.stamping.model.ad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdLocation {
    private String positionId;
    private String positionName;
    private List<AdData> adData;
}
