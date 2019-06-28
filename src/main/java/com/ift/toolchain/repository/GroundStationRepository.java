package com.ift.toolchain.repository;

import com.ift.toolchain.model.GroundStation;
import jdk.nashorn.internal.runtime.options.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroundStationRepository extends JpaRepository<GroundStation, String> {

    public GroundStation findByName(String name);
    public GroundStation findByStationId(String stationid);
    public Optional<GroundStation> findByGsId(int gsId);
}
