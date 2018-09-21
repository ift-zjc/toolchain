package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class SatelliteXSatellite {

    private String source;
    private String destination;
    private double distance;
    private double angularVelocity;
}
