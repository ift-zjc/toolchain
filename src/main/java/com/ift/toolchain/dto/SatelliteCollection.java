package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SatelliteCollection {

    String name;
    String leftSatelliteName;
    String rightSatelliteName;
    List<SatelliteDto> satellites;
}
