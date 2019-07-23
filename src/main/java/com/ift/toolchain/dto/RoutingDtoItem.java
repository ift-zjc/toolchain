package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoutingDtoItem {

    private int numNode;
    private String color;
    private List<String> nodeID;
}
