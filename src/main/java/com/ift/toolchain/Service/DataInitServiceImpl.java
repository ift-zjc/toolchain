package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataInitServiceImpl implements DataInitService {


    @Autowired
    OrbitService orbitService;
    @Autowired
    SatelliteService satelliteService;

    @Override
    public void initOrbitSatellite(String orbName, String satelliteName, int satelliteOrder) {

        // Get orbit
        Orbit orbit = orbitService.findByName(orbName);
        if(orbit == null){
            orbit = orbitService.save(orbName, 0);
        }

        // Add satellite
        satelliteService.save(orbit, satelliteName, satelliteOrder);
    }
}
