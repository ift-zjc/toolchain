package com.ift.toolchain.Service;

import com.ift.toolchain.model.ConfigFile;
import com.ift.toolchain.repository.ConfigFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigFileService {

    @Autowired
    ConfigFileRepository configFileRepository;


    public ConfigFile save(ConfigFile configFile){
        return configFileRepository.save(configFile);
    }

    public ConfigFile getConfigFile(String fileType){
        return configFileRepository.getByFileType(fileType);
    }
}
