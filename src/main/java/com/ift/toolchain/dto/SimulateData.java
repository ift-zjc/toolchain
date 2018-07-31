package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SimulateData {

    List<SimulateResultDto> simulateResultDtos;
    List<ApplicationTraffic> applicationTraffic;
}
