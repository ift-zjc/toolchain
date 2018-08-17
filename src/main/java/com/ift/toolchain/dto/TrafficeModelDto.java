package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class
TrafficeModelDto {

    private String tmId;
    private String tmCode;
    private String tmName;
    private String tmDesc;

    List<TrafficModelConfigDto> trafficModelConfigDtoList;
}
