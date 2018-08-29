package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SatellitePopulated {
    private List<SatelliteCollection> satelliteCollections;
    private List<SatelliteItem> satelliteItems;
}
