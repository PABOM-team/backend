package com.pabom.backend.species.application.service;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.species.application.result.MorphResult;
import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import com.pabom.backend.species.domain.repository.MorphRepository;
import com.pabom.backend.species.domain.repository.SpeciesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesQueryService {

    private final SpeciesRepository speciesRepository;
    private final MorphRepository morphRepository;

    public List<SpeciesResult> getSpecies() {
        return speciesRepository.findAllOrdered().stream().map(SpeciesResult::from).toList();
    }

    public List<MorphResult> getMorphs(Long speciesId) {
        speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(SpeciesErrorCode.SPECIES_NOT_FOUND));
        return morphRepository.findAllBySpeciesId(speciesId).stream().map(MorphResult::from).toList();
    }
}
