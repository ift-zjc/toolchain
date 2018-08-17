package com.ift.toolchain.Service;


import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;

import java.util.List;

public interface TrafficeModelService {
    List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs);
}
