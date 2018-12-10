package com.ift.toolchain.dto;

import lombok.Data;

import java.util.LinkedList;

@Data
public class ShortestPath {

    private String appName;
    private LinkedList<String> path = new LinkedList<>();
}
