package com.ift.toolchain.dto;

import lombok.Data;

@Data
public class ApplicationDto {

    private String name;
    private String source;
    private String dest;
    private String protocol;
    private String tm;
    private long startOffset;
    private long endOffset;
}
