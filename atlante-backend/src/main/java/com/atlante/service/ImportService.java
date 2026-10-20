package com.atlante.service;

import com.atlante.client.MetMuseumClient;
import com.atlante.client.TmdbClient;
import com.atlante.dto.Dtos.ExternalSearchResult;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.ExternalSource;
import com.atlante.model.Work;
import com.atlante.model.WorkImage;
import com.atlante.model.WorkType;
import com.atlante.repository.WorkImageRepository;
import com.atlante.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Transactional
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final TmdbClient tmdbClient;
    private final MetMuseumClient metClient;
    private final WorkRepository workRepository;
    private final WorkImageRepository workImageRepository;
    private final Path storageDir;

    public ImportService(TmdbClient tmdbClient, MetMuseumClient metClient,
                         WorkRepository workRepository, WorkImageRepository workImageRepository,
                         @Value("${app.images.storage-dir}") String storageDir) {
        this.tmdbClient = tmdbClient;
        this.metClient = metClient;
        this.workRepository = workRepository;
        this.workImageRepository = workImageRepository;
        this.storageDir = Path.of(storageDir);
    }

    public WorkResponse importFromTmdb(String tmdbId) {
        return importExternal(
                tmdbClient.findById(tmdbId).orElseThrow(
                        () -> new ResourceNotFoundException("Film non trovato su TMDB: " + tmdbId)),
                WorkType.FILM, ExternalSource.TMDB);
    }

    public WorkResponse importFromMet(String objectId) {
        return importExternal(
                metClient.findById(objectId).orElseThrow(
                        () -> new ResourceNotFoundException("Opera non trovata al Met: " + objectId)),
                WorkType.ARTWORK, ExternalSource.MET);
    }

    private WorkResponse importExternal(ExternalSearchResult result, WorkType type, ExternalSource source) {
        var existing = workRepository.findByExternalSourceAndExternalId(source, result.externalId());
        if (existing.isPresent()) {
            return WorkResponse.from(existing.get());
        }

        Work work = new Work(type, result.title(), result.creator(), result.year());
        work.setExternalSource(source);
        work.setExternalId(result.externalId());
        work.setImageUrl(result.imageUrl());
        workRepository.save(work);

        if (result.imageUrl() != null) {
            snapshotImage(work, result.imageUrl());
        }
        return WorkResponse.from(work);
    }

    /**
     * Importa nel database un risultato di ricerca esterno (Wikipedia, AIC,
     * TMDB, Met): se già presente lo restituisce, altrimenti lo crea con
     * snapshot immagine e palette estratta.
     */
    public com.atlante.dto.Dtos.WorkResponse importExternalResult(
            com.atlante.dto.Dtos.ExternalImportRequest req) {
        ExternalSource source;
        try {
            source = req.source() != null
                    ? ExternalSource.valueOf(req.source().toUpperCase())
                    : ExternalSource.MANUAL;
        } catch (IllegalArgumentException e) {
            source = ExternalSource.MANUAL;
        }

        if (req.externalId() != null && source != ExternalSource.MANUAL) {
            var existing = workRepository.findByExternalSourceAndExternalId(source, req.externalId());
            if (existing.isPresent()) {
                return com.atlante.dto.Dtos.WorkResponse.from(existing.get());
            }
        }

        Work work = new Work(req.type(), req.title(), req.creator(), req.year());
        work.setExternalSource(source);
        work.setExternalId(req.externalId());
        work.setImageUrl(req.imageUrl());
        workRepository.save(work);

        if (req.imageUrl() != null) {
            snapshotImage(work, req.imageUrl());
        }
        return com.atlante.dto.Dtos.WorkResponse.from(work);
    }

    /**
     * Aggancia un'immagine remota a un'opera esistente: snapshot su disco,
     * palette estratta solo se non già curata a mano.
     */
    public void attachImage(Long workId, String url) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("Opera non trovata: " + workId));
        snapshotImage(work, url);
        workRepository.save(work);
    }

    private void snapshotImage(Work work, String url) {
        try {
            Files.createDirectories(storageDir);
            String ext = url.toLowerCase().endsWith(".png") ? ".png" : ".jpg";
            String baseName = work.getExternalId() != null
                    ? work.getExternalSource() + "-" + work.getExternalId()
                    : "work-" + work.getId();
            Path target = storageDir.resolve(baseName + ext);
            var connection = URI.create(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent",
                    "VisuArt/1.0 (progetto didattico Epicode)");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(20_000);
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            workImageRepository.save(new WorkImage(work, url, target.toString()));
            work.setImageUrl("/images/" + target.getFileName());
            if (work.getPalette() == null) {
                work.setPalette(PaletteExtractor.extract(target, 4));
            }
        } catch (Exception e) {
            log.warn("Snapshot immagine fallito per work {}: {}", work.getId(), e.getMessage());
        }
    }
}
