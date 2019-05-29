package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class MininetDataDto {

    private String app;
    private float goodput;
    private float applicationDelay;
    private String time;
    private float tput;
    private float pdelay;

    @Override
    public String toString() {
        return "MininetDataDto{" +
                "app='" + app + '\'' +
                ", goodput=" + goodput +
                ", applicationDelay=" + applicationDelay +
                ", time='" + time + '\'' +
                ", tput=" + tput +
                ", pdelay=" + pdelay +
                '}';
    }
}
