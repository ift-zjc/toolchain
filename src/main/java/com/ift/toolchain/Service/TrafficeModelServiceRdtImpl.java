package com.ift.toolchain.Service;


import com.ift.toolchain.dto.ApplicationTrafficData;
import com.ift.toolchain.dto.TmOptions;
import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.model.TrafficModelConfig;
import com.ift.toolchain.repository.TrafficeModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(value = "rdtTrafficModelService")
public class TrafficeModelServiceRdtImpl implements TrafficeModelService {

    @Override
    public List<ApplicationTrafficData> simulate(long startOffset, long endOffset, List<TrafficModelConfig> trafficModelConfigs) {
        return null;
    }
}
