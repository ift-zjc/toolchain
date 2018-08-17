package com.ift.toolchain.repository;

import com.ift.toolchain.model.TrafficModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TrafficModelConfigRepository extends JpaRepository<TrafficModelConfig, String> {
}
