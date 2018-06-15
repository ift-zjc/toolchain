package com.ift.toolchain.Service;

import com.ift.toolchain.model.GroundStation;
import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Parameter;
import com.ift.toolchain.model.Satellite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DataInitServiceImpl implements DataInitService {


    @Autowired
    OrbitService orbitService;
    @Autowired
    SatelliteService satelliteService;
    @Autowired
    ParameterService parameterService;
    @Autowired
    GroundStationService groundStationService;

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

    @Override
    public void updateSatellite(String satelliteName, String satelliteId, Map params) {
        // Get name
        satelliteName = satelliteName.replaceAll("GPS ", "");
        // Get satellite record
        Satellite satellite = satelliteService.updateSatellite(satelliteName, satelliteId);

        // Save params
        params.forEach((k, v) -> {
            // Save to parameter table
            parameterService.save(k.toString(), v.toString(), satellite);
        });

    }

    @Override
    public void initGroundStation(String name, String gId, Map params) {
        // New ground station
        GroundStation groundStation = new GroundStation();
        groundStation.setName(name);
        groundStation.setStationId(gId);
        final GroundStation savedGroundStation = groundStationService.save(groundStation);
        params.forEach((k, v) -> {
            Parameter parameter = new Parameter();
            parameter.setName(k.toString());
            parameter.setValue(v.toString());
            parameter.setGroundStation(savedGroundStation);

            parameterService.save(parameter);
        });
    }
}
