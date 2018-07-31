package com.ift.toolchain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataGridTM {

    private int totalCount;
    private List<TrafficeModelDto> items;
}
