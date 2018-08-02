package com.ift.toolchain.Service;

import com.ift.toolchain.model.GroundStation;

import java.util.List;

public interface GroundStationService {
    public GroundStation save(GroundStation groundStation);
    public List<GroundStation> getAll();
    public GroundStation findById(String id);
    public GroundStation findByName(String name);
}
