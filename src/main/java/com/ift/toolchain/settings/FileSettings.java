package com.ift.toolchain.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "data.file.orb")
@Data
public class FileSettings {

    private String basedir;
}
