package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimulationMessage {

    private String message;
    private float percentage;
}
