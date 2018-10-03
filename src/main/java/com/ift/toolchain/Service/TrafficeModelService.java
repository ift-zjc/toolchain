package com.ift.toolchain.Service;


import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;
import org.joda.time.DateTime;

import java.util.List;

public interface TrafficeModelService {
    List<ApplicationTrafficData> simulate(DateTime startTime, DateTime endTime, List<TrafficModelConfig> trafficModelConfigs, String appName);
}
