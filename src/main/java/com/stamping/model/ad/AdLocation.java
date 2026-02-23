package com.stamping.model.ad;

import lombok.Data;
import java.util.List;

@Data
public class AdLocation {
    private String positionId;
    private String positionName;
    private List<AdData> adData;
}
