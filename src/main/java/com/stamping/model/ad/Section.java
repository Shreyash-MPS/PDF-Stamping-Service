package com.stamping.model.ad;

import lombok.Data;
import java.util.List;

@Data
public class Section {
    private String sectionId;
    private List<String> sectionPath;
    private List<AdLocation> adLocation;
}
