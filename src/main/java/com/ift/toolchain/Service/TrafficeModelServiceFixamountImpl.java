package com.ift.toolchain.Service;

import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.dto.TmOptions;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed Amount traffic model
 */
@Service(value = "fixAmtTrafficModelService")
public class TrafficeModelServiceFixamountImpl implements TrafficeModelService {

    @Override
    public List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs) {
        return null;
    }
}
