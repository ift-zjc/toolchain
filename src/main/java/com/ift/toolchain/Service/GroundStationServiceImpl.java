package com.ift.toolchain.Service;

import com.ift.toolchain.model.GroundStation;
import com.ift.toolchain.repository.GroundStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroundStationServiceImpl implements GroundStationService {

    @Autowired
    GroundStationRepository groundStationRepository;

    @Override
    public GroundStation save(GroundStation groundStation) {
        return groundStationRepository.save(groundStation);
    }

    @Override
    public List<GroundStation> getAll() {
        return groundStationRepository.findAll();
    }

    @Override
    public GroundStation findById(String id) {
        return groundStationRepository.getOne(id);
    }

    @Override
    public GroundStation findByName(String name) {
        return groundStationRepository.findByName(name);
    }
}
