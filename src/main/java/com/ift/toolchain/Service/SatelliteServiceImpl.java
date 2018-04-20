package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.repository.SatelliteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SatelliteServiceImpl implements SatelliteService {

    @Autowired
    SatelliteRepository satelliteRepository;

    @Override
    public Satellite save(Satellite satellite) {
        return satelliteRepository.save(satellite);
    }

    @Override
    public Satellite save(Orbit orbit, String satelliteName, int satOrder) {
        Satellite satellite = new Satellite();
        satellite.setOrbit(orbit);
        satellite.setName(satelliteName);
        satellite.setOrderOnOrbit(satOrder);

        return save(satellite);
    }

    @Override
    public Satellite findByName(String satelliteName) {
        return satelliteRepository.findSatelliteByName(satelliteName);
    }
}
