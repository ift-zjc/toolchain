package com.ift.toolchain.Service;

import com.ift.toolchain.model.TrafficModelConfig;
import com.ift.toolchain.repository.TrafficModelConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrafficModelConfigServiceImpl implements TrafficModelConfigService {

    @Autowired
    TrafficModelConfigRepository trafficModelConfigRepository;

    @Override
    public TrafficModelConfig save(TrafficModelConfig trafficModelConfig) {
        return trafficModelConfigRepository.save(trafficModelConfig);
    }
}
