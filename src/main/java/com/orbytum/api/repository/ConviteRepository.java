package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Convite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConviteRepository extends JpaRepository<Convite, Long> {
}

