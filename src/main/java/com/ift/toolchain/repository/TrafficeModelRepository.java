package com.ift.toolchain.repository;

import com.ift.toolchain.model.TrafficModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficeModelRepository extends JpaRepository<TrafficModel, String> {

    public TrafficModel getByCode(String code);
}
