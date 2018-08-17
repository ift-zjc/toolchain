package com.ift.toolchain.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "fcm")
@Component
@Data
public class FcmSettings {

    private String serviceAccountFile;
}
