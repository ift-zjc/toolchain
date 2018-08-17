package com.ift.toolchain.repository;

import com.ift.toolchain.model.TrafficModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrafficeModelRepository extends JpaRepository<TrafficModel, String> {

    public Optional<TrafficModel> getByCode(String code);
}
