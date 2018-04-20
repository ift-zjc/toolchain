package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Satellite;

public interface SatelliteService {

    public Satellite save(Satellite satellite);
    public Satellite save(Orbit orbit, String satelliteName, int satOrder);

    public Satellite findByName(String satelliteName);
}
