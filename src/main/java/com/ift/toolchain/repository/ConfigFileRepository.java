package com.ift.toolchain.repository;

import com.ift.toolchain.model.ConfigFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigFileRepository extends JpaRepository<ConfigFile, String> {

    public ConfigFile getByFileType(String fileType);
}
