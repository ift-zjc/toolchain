package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class ApplicationTrafficData {
    private long offsetMillionSecond;
    private float trafficVolumn;
    private String timeString;
    private String appName;
}
