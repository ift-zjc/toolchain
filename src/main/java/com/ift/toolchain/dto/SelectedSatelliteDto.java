package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelectedSatelliteDto {

    List<NameValue> satelliteProperties;
    List<SatelliteXSatellite> satelliteXSatellites;
}
