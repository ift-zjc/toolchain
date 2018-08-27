package com.ift.toolchain.repository;

import com.ift.toolchain.model.Tle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TleRepository extends JpaRepository<Tle, String> {
}
