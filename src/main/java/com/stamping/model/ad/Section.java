package com.stamping.model.ad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {
    private String sectionId;
    private List<String> sectionPath;
    private List<AdLocation> adLocation;
}
