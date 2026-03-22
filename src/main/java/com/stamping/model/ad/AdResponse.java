package com.stamping.model.ad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdResponse {
    private String publisherId;
    private String journlcode;
    private List<Section> section;
}
