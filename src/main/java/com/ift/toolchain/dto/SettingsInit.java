package com.ift.toolchain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SettingsInit {

    @JsonProperty("satellites")
    private List<SatelliteSettings> satelliteSettings;
    @JsonProperty("basestations")
    private List<BaseStationSettings> baseStationSettings;
}
