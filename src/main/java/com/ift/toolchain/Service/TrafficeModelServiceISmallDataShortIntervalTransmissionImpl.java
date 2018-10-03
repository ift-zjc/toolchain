package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModelConfig;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service(value = "smallDataShortIntervalTransmission")
public class TrafficeModelServiceISmallDataShortIntervalTransmissionImpl implements TrafficeModelService {
    @Override
    public List<ApplicationTrafficData> simulate(DateTime startTime, DateTime endTime, List<TrafficModelConfig> trafficModelConfigs, String appName) {
        List<ApplicationTrafficData> applicationTrafficDataList = new ArrayList<>();

        // Get timeinterval & data volume
        Optional<TrafficModelConfig> trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("timeinterval")).findAny();
        int timeinterval = Integer.parseInt(trafficModelConfig.get().getValue());
        trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("datavolume")).findAny();
        int datavolume = Integer.parseInt(trafficModelConfig.get().getValue());

        // Exponential generator
        ExponentialDistribution exponentialDistributionTime = new ExponentialDistribution(timeinterval);
        ExponentialDistribution exponentialDistributionDataVolume = new ExponentialDistribution(datavolume);


        while(startTime.compareTo(endTime) <= 0){
            ApplicationTrafficData applicationTrafficData = new ApplicationTrafficData();
            applicationTrafficData.setTimeString(startTime.toString());
            applicationTrafficData.setTrafficVolumn(Float.parseFloat(String.valueOf(exponentialDistributionDataVolume.sample())));
            applicationTrafficData.setAppName(appName);

            applicationTrafficDataList.add(applicationTrafficData);

            startTime = startTime.plusSeconds((int) exponentialDistributionTime.sample());
        }

        return applicationTrafficDataList;
    }
}
