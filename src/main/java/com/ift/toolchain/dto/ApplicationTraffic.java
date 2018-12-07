package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationTraffic {

    private String appId;
    private String appName;
    private List<ApplicationTrafficData> applicationTrafficDataList;
}
