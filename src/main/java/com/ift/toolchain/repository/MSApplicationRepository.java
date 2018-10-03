package com.ift.toolchain.repository;

import com.ift.toolchain.model.MSAApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MSApplicationRepository extends JpaRepository<MSAApplication, String> {

}
