package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SatelliteDto {

    long time;
    double[] cartesian3;
    String orbName;
}
