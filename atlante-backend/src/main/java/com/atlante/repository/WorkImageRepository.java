package com.atlante.repository;

import com.atlante.model.WorkImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkImageRepository extends JpaRepository<WorkImage, Long> {

    Optional<WorkImage> findByWorkId(Long workId);

    /** L'ultimo originale scaricato per l'opera che oggi usa questa immagine locale. */
    Optional<WorkImage> findFirstByWork_ImageUrlOrderByIdDesc(String imageUrl);
}
