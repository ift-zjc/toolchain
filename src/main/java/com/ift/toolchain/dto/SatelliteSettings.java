package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class SatelliteSettings {

    private String satID;
    private String satName;
    private float outLinkMax;
    private float inLinkMax;
    private float netIfnum;
}
