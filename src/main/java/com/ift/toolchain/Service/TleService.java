package com.ift.toolchain.Service;

import com.ift.toolchain.model.Tle;
import com.ift.toolchain.repository.TleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * Get tle entity by satellite name
     * @param satelliteName
     * @return
     */
    public Tle getByName(String satelliteName){
        return tleRepository.getTleByName(satelliteName);
    }

    /**
     * Load all TLE data
     * @return
     */
    public List<Tle> getAllTles(){return tleRepository.getTleByEnabled(true);}

    /**
     * Remove all records
     */
    public void removeRecords(){
        tleRepository.deleteAll();
    }
}
