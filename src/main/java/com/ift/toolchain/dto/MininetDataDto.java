package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class MininetDataDto {

    private String app;
    private float goodput;
    private float applicationDelay;
    private String time;
}
