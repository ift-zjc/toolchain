package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModelConfig;
import org.apache.commons.math3.analysis.function.Floor;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service(value = "smallDataShortIntervalTransmission")
public class TrafficeModelServiceISmallDataShortIntervalTransmissionImpl implements TrafficeModelService {
    @Override
    public List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs) {
        List<ApplicationTrafficData> applicationTrafficDataList = new ArrayList<>();

        // Get timeinterval & data volume
        Optional<TrafficModelConfig> trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("timeinterval")).findAny();
        int timeinterval = Integer.parseInt(trafficModelConfig.get().getValue());
        trafficModelConfig = trafficModelConfigs.stream().filter(trafficModelConfig1 -> trafficModelConfig1.getName().equalsIgnoreCase("datavolume")).findAny();
        int datavolume = Integer.parseInt(trafficModelConfig.get().getValue());

        // Exponential generator
        ExponentialDistribution exponentialDistributionTime = new ExponentialDistribution(timeinterval);
        ExponentialDistribution exponentialDistributionDataVolume = new ExponentialDistribution(datavolume);
        for(long j = startOffset; j <= endOffset;  j += exponentialDistributionTime.sample() ) {

            ApplicationTrafficData applicationTrafficData = new ApplicationTrafficData();
            applicationTrafficData.setOffsetMillionSecond(j);
            applicationTrafficData.setTrafficVolumn(Float.parseFloat(String.valueOf(exponentialDistributionDataVolume.sample())));

            applicationTrafficDataList.add(applicationTrafficData);
        }

        return applicationTrafficDataList;
    }
}
