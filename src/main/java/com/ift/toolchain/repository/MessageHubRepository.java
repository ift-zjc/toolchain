package com.ift.toolchain.repository;

import com.ift.toolchain.model.MessageHub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageHubRepository extends JpaRepository<MessageHub, String> {

}
