package com.ift.toolchain.repository;

import com.ift.toolchain.model.TimePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimePointRepository extends JpaRepository<TimePoint, String> {

    public TimePoint findByOffset(float offset);
}
