package com.ift.toolchain.repository;

import com.ift.toolchain.model.Tle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TleRepository extends JpaRepository<Tle, String> {

    Tle getTleByName(String name);
    Optional<Tle> findByNumber(String number);
    List<Tle> getTleByEnabled(boolean enabled);
}
