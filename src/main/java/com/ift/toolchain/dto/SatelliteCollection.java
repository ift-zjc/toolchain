package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SatelliteCollection {

    Map<String, List<SatelliteDto>> satellites;
}
