package com.ift.toolchain.Service;


import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModelConfig;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(value = "rdtTrafficModelService")
public class TrafficeModelServiceRdtImpl implements TrafficeModelService {

    @Override
    public List<ApplicationTrafficData> simulate(DateTime startOffset, DateTime endOffset, List<TrafficModelConfig> trafficModelConfigs) {
        return null;
    }
}
