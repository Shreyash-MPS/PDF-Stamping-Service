package com.stamping.model.ad;

import lombok.Data;
import java.util.List;

@Data
public class AdResponse {
    private String publisherId;
    private String journlcode;
    private List<Section> section;
}
