package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class ObjectEvent {

    private String sourceid;
    private String destid;
    private double distance;
    private double angularvelocity;

    private long msgEffTime;
    private int msgType;
    private int subMsgType;
}
