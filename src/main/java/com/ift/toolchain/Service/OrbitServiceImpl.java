package com.ift.toolchain.Service;

import com.ift.toolchain.model.Orbit;
import com.ift.toolchain.repository.OrbitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class OrbitServiceImpl implements OrbitService {

    @Autowired
    OrbitRepository orbitRepository;

    @Override
    public Orbit save(Orbit orbit) {
        return orbitRepository.save(orbit);
    }
}
