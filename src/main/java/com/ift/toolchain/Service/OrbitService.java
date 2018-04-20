package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;

public interface OrbitService {

    public Orbit save(Orbit orbit);
    public Orbit save(String orbitName, int orbitOrder);

    public Orbit findByName(String orbitName);
}
