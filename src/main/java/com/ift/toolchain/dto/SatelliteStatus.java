package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class SatelliteStatus {

    private double x, y, z, a, e, i, perigeeArgument, rightAscensionOfAscendingNode, trueAnomaly, meanAnomaly, aDot,
    eccentricAnomaly, avX, avY, avZ;
}
