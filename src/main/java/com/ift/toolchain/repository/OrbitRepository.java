package com.ift.toolchain.repository;

import com.ift.toolchain.model.Orbit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrbitRepository extends JpaRepository<Orbit, String> {

}
