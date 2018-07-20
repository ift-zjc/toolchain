package com.ift.toolchain.Service;

import com.ift.toolchain.model.GroundStation;
import com.ift.toolchain.repository.GroundStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroundStationServiceImpl implements GroundStationService {

    @Autowired
    GroundStationRepository groundStationRepository;

    @Override
    public GroundStation save(GroundStation groundStation) {
        return groundStationRepository.save(groundStation);
    }
}
