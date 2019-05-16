package com.ift.toolchain.repository;

import com.ift.toolchain.model.MSAApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MSAApplicationEventRepository extends JpaRepository<MSAApplicationEvent, String> {
}
