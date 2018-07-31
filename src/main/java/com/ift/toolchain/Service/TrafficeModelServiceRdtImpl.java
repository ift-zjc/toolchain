package com.ift.toolchain.Service;


import com.ift.toolchain.model.TrafficModel;
import com.ift.toolchain.repository.TrafficeModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value = "rdtTrafficModelService")
public class TrafficeModelServiceRdtImpl implements TrafficeModelService {

    @Autowired
    TrafficeModelRepository trafficeModelRepository;

    @Override
    public double getResult() {
        return 0;
    }

    @Override
    public TrafficModel create(TrafficModel trafficModel) {
        return trafficeModelRepository.save(trafficModel);
    }
}
