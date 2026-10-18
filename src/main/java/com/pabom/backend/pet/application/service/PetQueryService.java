package com.pabom.backend.pet.application.service;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.repository.PetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetQueryService {

    private final PetRepository petRepository;

    public List<PetResult> getAll(Long userId) {
        return petRepository.findAllByUserId(userId).stream().map(PetResult::from).toList();
    }

    public PetResult get(Long userId, Long petId) {
        return petRepository.findByIdAndUserId(petId, userId)
                .map(PetResult::from)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
    }
}
