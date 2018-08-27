package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class SatelliteDto {

    long time;
    String julianDate;
    double[] cartesian3;
}
