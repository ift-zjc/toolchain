package com.ift.toolchain.repository;

import com.ift.toolchain.model.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, String> {
}
