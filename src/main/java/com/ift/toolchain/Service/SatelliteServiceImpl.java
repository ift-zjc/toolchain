package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Satellite;
import com.ift.toolchain.repository.SatelliteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


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

    @Override
    public List<Satellite> getSatelliesOnSameOrb(String satelliteName) {

        // Get orb name
        String orbName = findByName(satelliteName).getOrbit().getName();
        return satelliteRepository.findAllByOrbitName(orbName);
    }

    @Override
    public Satellite updateSatellite(String satelliteName, String satelliteId) {
        Satellite satellite = findByName(satelliteName);
        satellite.setSatelliteId(satelliteId);
        return save(satellite);
    }

    @Override
    public List<Satellite> getAll() {
        return satelliteRepository.findAll();
    }
}
