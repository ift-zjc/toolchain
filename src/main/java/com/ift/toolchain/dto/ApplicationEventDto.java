package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class ApplicationEventDto {

    private String path;
    private String timetick;
    private float throughput;
    private String distance;
}
