package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.model.Satellite;

import java.util.List;

public interface SatelliteService {

    public Satellite save(Satellite satellite);
    public Satellite save(Orbit orbit, String satelliteName, int satOrder);

    public Satellite findById(String id);
    public Satellite findByName(String satelliteName);
    public List<Satellite> getSatelliesOnSameOrb(String satelliteName);
    public Satellite updateSatellite(String satelliteName, String satelliteId);
    public List<Satellite> getAll();
}
