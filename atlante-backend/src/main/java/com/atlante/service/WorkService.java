package com.atlante.service;

import com.atlante.dto.Dtos.WorkRequest;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import com.atlante.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WorkService {

    private final WorkRepository workRepository;
    private final com.atlante.repository.ConnectionRepository connectionRepository;
    private final com.atlante.repository.FavoriteRepository favoriteRepository;
    private final com.atlante.repository.WorkImageRepository workImageRepository;
    private final ImageUploadService imageUploads;

    public WorkService(WorkRepository workRepository,
                       com.atlante.repository.ConnectionRepository connectionRepository,
                       com.atlante.repository.FavoriteRepository favoriteRepository,
                       com.atlante.repository.WorkImageRepository workImageRepository,
                       ImageUploadService imageUploads) {
        this.imageUploads = imageUploads;
        this.workRepository = workRepository;
        this.connectionRepository = connectionRepository;
        this.favoriteRepository = favoriteRepository;
        this.workImageRepository = workImageRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkResponse> findAll(WorkType type, String q) {
        List<Work> works;
        if (q != null && !q.isBlank()) {
            works = workRepository.findByTitleContainingIgnoreCase(q.trim());
        } else if (type != null) {
            works = workRepository.findByType(type);
        } else {
            works = workRepository.findAll();
        }
        return works.stream()
                .filter(w -> type == null || w.getType() == type)
                .map(WorkResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Work getEntity(Long id) {
        return workRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opera non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public WorkResponse findById(Long id) {
        return WorkResponse.from(getEntity(id));
    }

    /**
     * Opera aggiunta a mano da un utente, quando non è negli archivi esterni: l'immagine è
     * quella caricata (/images/uploads/...) e la palette si ricava da lì.
     */
    public WorkResponse create(WorkRequest req) {
        Work work = new Work(req.type(), req.title().trim(), blankToNull(req.creator()), req.year());
        String image = imageUploads.requireUploaded(req.imageUrl());
        work.setImageUrl(image);
        work.setDescription(blankToNull(req.description()));
        work.setPalette(req.palette() != null ? req.palette() : imageUploads.paletteOf(image));
        return WorkResponse.from(workRepository.save(work));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public WorkResponse update(Long id, WorkRequest req) {
        Work work = getEntity(id);
        work.setType(req.type());
        work.setTitle(req.title());
        work.setCreator(req.creator());
        work.setYear(req.year());
        work.setImageUrl(req.imageUrl());
        work.setDescription(req.description());
        work.setPalette(req.palette());
        return WorkResponse.from(work);
    }

    /**
     * Un'opera citata in una connessione non si elimina (si perderebbe la critica di qualcuno):
     * prima vanno tolte le connessioni. Preferiti e immagine salvata spariscono con lei.
     */
    public void delete(Long id) {
        Work work = getEntity(id);
        int used = connectionRepository.findAllTouching(id).size();
        if (used > 0) {
            throw new IllegalArgumentException(
                    "Opera usata in " + used + " connessioni: elimina prima quelle connessioni");
        }
        favoriteRepository.deleteAll(favoriteRepository.findByWorkId(id));
        workImageRepository.findByWorkId(id).ifPresent(workImageRepository::delete);
        workRepository.delete(work);
    }
}
