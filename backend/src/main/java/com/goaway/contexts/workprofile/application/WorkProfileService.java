package com.goaway.contexts.workprofile.application;

import com.goaway.contexts.workprofile.api.dto.UpsertWorkProfileRequest;
import com.goaway.contexts.workprofile.domain.WorkProfile;
import com.goaway.contexts.workprofile.infrastructure.persistence.WorkProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkProfileService {

    private final WorkProfileRepository repository;

    public WorkProfileService(WorkProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public WorkProfile getOrEmpty(Long userId) {
        return repository.findByUserId(userId).orElseGet(() -> new WorkProfile(userId));
    }

    @Transactional
    public WorkProfile upsert(Long userId, UpsertWorkProfileRequest request) {
        WorkProfile profile = repository.findByUserId(userId).orElseGet(() -> new WorkProfile(userId));
        profile.setNickname(trim(request.getNickname()));
        profile.setCity(trim(request.getCity()));
        profile.setIndustry(trim(request.getIndustry()));
        profile.setJobType(trim(request.getJobType()));
        profile.setGender(trim(request.getGender()));
        profile.setWorkStart(trim(request.getWorkStart()));
        profile.setWorkEnd(trim(request.getWorkEnd()));
        profile.setHatedRelation(trim(request.getHatedRelation()));
        profile.setHatedNickname(trim(request.getHatedNickname()));
        profile.setHatedTraits(trim(request.getHatedTraits()));
        return repository.save(profile);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
