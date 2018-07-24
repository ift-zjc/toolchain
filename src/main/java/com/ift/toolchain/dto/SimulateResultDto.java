package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimulateResultDto {

    private String satelliteNameSource;
    private String satelliteNameDest;
    private boolean connected;
    private long offsetMillionSecond;
    private int msgType;
    private int msgSubType;
    private float delay;
    private double angelVelocity;
    private float trafficLoading;


    private String argumentField;   // For DxChant display only.

}
