package com.ift.toolchain.repository;

import com.ift.toolchain.model.GroundStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroundStationRepository extends JpaRepository<GroundStation, String> {

    public GroundStation findByName(String name);
    public GroundStation findByStationId(String stationid);
}
