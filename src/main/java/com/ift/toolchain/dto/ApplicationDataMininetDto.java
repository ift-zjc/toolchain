package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationDataMininetDto {

    private String appName;
    private String startTime;
    private String endTime;
    private String protocol;

    private List<ApplicationEventDto> events;
}
