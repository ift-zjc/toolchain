package com.ift.toolchain.repository;

import com.ift.toolchain.model.Tle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TleRepository extends JpaRepository<Tle, String> {

    Tle getTleByName(String name);
    List<Tle> getTleByEnabled(boolean enabled);
}
