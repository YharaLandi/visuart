package com.atlante.repository;

import com.atlante.model.ExternalSource;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkRepository extends JpaRepository<Work, Long> {

    List<Work> findByType(WorkType type);

    List<Work> findByTitleContainingIgnoreCase(String title);

    Optional<Work> findByExternalSourceAndExternalId(ExternalSource source, String externalId);
}
