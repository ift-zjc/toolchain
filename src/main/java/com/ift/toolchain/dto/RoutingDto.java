package com.ift.toolchain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RoutingDto {

    @JsonProperty(value = "traceroute")
    private List<RoutingDtoItem> routingItems;
}
