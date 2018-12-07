package com.ift.toolchain.repository;

import com.ift.toolchain.model.AppTrafficData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppTrafficDataRepository extends JpaRepository<AppTrafficData, String> {
}
