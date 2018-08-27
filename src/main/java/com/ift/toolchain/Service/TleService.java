package com.ift.toolchain.Service;

import com.ift.toolchain.model.Tle;
import com.ift.toolchain.repository.TleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TleService {

    @Autowired
    TleRepository tleRepository;

    /**
     * Save TLE instance
     * @param tle
     * @return
     */
    public Tle save(Tle tle){
        return tleRepository.save(tle);
    }
}
