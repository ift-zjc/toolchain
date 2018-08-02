package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ObjectDto {

    private String id;
    private String name;
    private String category;

    public ObjectDto(){}
}
