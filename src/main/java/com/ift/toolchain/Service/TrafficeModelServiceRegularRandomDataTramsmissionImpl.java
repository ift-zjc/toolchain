package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModelConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;


@Service(value = "regularRandomDataTransmission")
public class TrafficeModelServiceRegularRandomDataTramsmissionImpl implements TrafficeModelService {
    @Override
    public List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs) {


        List<ApplicationTrafficData> applicationTrafficDataList = new ArrayList<>();

        // Get timeinterval & data volume
        Optional<TrafficModelConfig> trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("timeinterval")).findAny();
        int timeinterval = Integer.parseInt(trafficModelConfig.get().getValue());
        trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("datavolume")).findAny();
        int datavolume = Integer.parseInt(trafficModelConfig.get().getValue());
        trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("timeintervaldelta")).findAny();
        int timeintervaldelta = Integer.parseInt(trafficModelConfig.get().getValue());
        trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("datavolumedelta")).findAny();
        int datavolumedelta = Integer.parseInt(trafficModelConfig.get().getValue());

        for(long j = startOffset; j <= endOffset;  j += (new Random().nextGaussian()*(timeintervaldelta/1.85) + timeinterval)) {

            ApplicationTrafficData applicationTrafficData = new ApplicationTrafficData();
            applicationTrafficData.setOffsetMillionSecond(j);
            applicationTrafficData.setTrafficVolumn(Float.parseFloat(String.valueOf(new Random().nextGaussian()*(datavolumedelta/1.85) + datavolume)));

            applicationTrafficDataList.add(applicationTrafficData);
        }

        return applicationTrafficDataList;


    }
}
