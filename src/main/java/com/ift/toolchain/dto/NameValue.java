package com.ift.toolchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NameValue {

    private String name;
    private Object value;
}
