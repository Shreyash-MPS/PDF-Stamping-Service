package com.stamping.model.ad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdData {
    private String adString;
    private String adHtml;
    private String adId;
    private String adStartDate;
    private String adEndDate;
    private String adFrequency;
}
