package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.dto.TmOptions;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service(value = "oneTimeDataTransmissionTrafficModel")
public class TrafficeModelServiceOneTimeDataTransmissionImpl implements TrafficeModelService {

    @Override
    public List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs) {
        // Get parameter from trafficModelConfig
        Optional<TrafficModelConfig> trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("time")).findAny();
        long time = Long.parseLong(trafficModelConfig.get().getValue());

        Optional<TrafficModelConfig> trafficModelConfigDataVollume = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("datavolume")).findAny();
        float dataVolumn = Float.parseFloat(trafficModelConfigDataVollume.get().getValue());

        ApplicationTrafficData applicationTrafficData = new ApplicationTrafficData();
        applicationTrafficData.setTrafficVolumn(dataVolumn);
        // Data transmite time is offset + time (when)
        applicationTrafficData.setOffsetMillionSecond(startOffset + time);

        List<ApplicationTrafficData> trafficData = new ArrayList<>();
        trafficData.add(applicationTrafficData);

        return trafficData;
    }
}
