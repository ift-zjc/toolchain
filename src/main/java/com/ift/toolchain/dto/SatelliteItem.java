package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class SatelliteItem {

    private String id;
    private String name;
    private boolean expanded;
    private String categoryId;

    public SatelliteItem(String id, String name, boolean expanded){
        this.id = id;
        this.name = name;
        this.expanded = expanded;
    }
}
