package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.CaseStep;
import ru.rt.rostelecom_tms.domain.cases.CaseTag;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseNotFoundException;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseTagRepository;
import ru.rt.rostelecom_tms.repository.projects.ProjectMemberRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ru.rt.rostelecom_tms.service.cases.CaseStepService.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseTagRepository caseTagRepository;
    private final CaseGroupService caseGroupService;
    private final ProjectMemberRepository projectMemberRepository;

    public record CreateCaseCommand(
            String title,
            Integer groupId,
            String description,
            String preconditions,
            String postconditions,
            List<String> tags,
            List<StepCommand> steps
    ) {
    }

    public record UpdateCaseCommand(
            String title,
            Integer groupId,
            String description,
            String preconditions,
            String postconditions,
            List<String> tags,
            List<StepCommand> steps
    ) {
    }

    public List<Case> findAll(User caller) {
        return caseRepository.findAllBy().stream()
                .filter(testCase -> hasReadAccess(testCase, caller))
                .toList();
    }

    public List<Case> findAllByGroup(int groupId, User caller) {
        List<Integer> groupIds = caseGroupService.findSubtreeGroupIds(groupId, caller);
        return caseRepository.findDistinctByGroupIdIn(groupIds).stream()
                .filter(testCase -> hasReadAccess(testCase, caller))
                .toList();
    }

    public List<Case> findAllByPlan(int planId, User caller) {
        return caseRepository.findDistinctByPlansId(planId).stream()
                .filter(testCase -> hasReadAccess(testCase, caller))
                .toList();
    }

    public List<Case> findAllWithFilters(
            Integer groupId,
            Integer planId,
            String title,
            String tag,
            Instant createdFrom,
            Instant createdTo,
            User caller
    ) {
        List<Case> base;
        if (planId != null) {
            base = findAllByPlan(planId, caller);
        } else if (groupId != null) {
            base = findAllByGroup(groupId, caller);
        } else {
            base = findAll(caller);
        }

        String normalizedTitle = title == null ? null : title.trim().toLowerCase();
        String normalizedTag = tag == null ? null : tag.trim().toLowerCase();

        return base.stream()
                .filter(testCase -> normalizedTitle == null || normalizedTitle.isBlank()
                        || testCase.getTitle().toLowerCase().contains(normalizedTitle))
                .filter(testCase -> normalizedTag == null || normalizedTag.isBlank()
                        || testCase.getTags().stream().anyMatch(caseTag -> normalizedTag.equals(caseTag.getName())))
                .filter(testCase -> createdFrom == null || !testCase.getCreatedAt().isBefore(createdFrom))
                .filter(testCase -> createdTo == null || !testCase.getCreatedAt().isAfter(createdTo))
                .toList();
    }

    public Case findOne(int id, User caller) {
        Case testCase = caseRepository.findOneById(id)
                .orElseThrow(() -> new CaseNotFoundException("Couldn't find case with id: " + id));
        if (!hasReadAccess(testCase, caller)) {
            throw new org.springframework.security.access.AccessDeniedException("No access to case");
        }
        return testCase;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public Case create(CreateCaseCommand cmd, User caller) {
        validateStepCommands(cmd.steps());
        CaseGroup group = caseGroupService.findOne(cmd.groupId(), caller);
        ensureWriteAccess(group, caller);
        ensureCaseTitleIsUnique(cmd.title(), group.getId(), null);

        Case newCase = new Case();
        newCase.setTitle(cmd.title());
        newCase.setGroup(group);
        newCase.setDescription(cmd.description());
        newCase.setPreconditions(cmd.preconditions());
        newCase.setPostconditions(cmd.postconditions());
        newCase.setCreatedAt(Instant.now());
        newCase.setTags(resolveTags(cmd.tags()));

        Case saved = caseRepository.save(newCase);

        if (cmd.steps() != null && !cmd.steps().isEmpty()) {
            List<CaseStep> steps = buildSteps(cmd.steps(), saved);
            saved.getCaseSteps().clear();
            saved.getCaseSteps().addAll(steps);
            caseRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void update(int id, UpdateCaseCommand cmd, User caller) {
        Case existingCase = findOne(id, caller);
        ensureWriteAccess(existingCase.getGroup(), caller);
        validateStepCommands(cmd.steps());

        String nextTitle = cmd.title() != null ? cmd.title() : existingCase.getTitle();
        Integer nextGroupId = cmd.groupId() != null ? cmd.groupId() : existingCase.getGroup().getId();
        ensureCaseTitleIsUnique(nextTitle, nextGroupId, existingCase);

        if (cmd.title() != null) {
            existingCase.setTitle(cmd.title());
        }
        if (cmd.groupId() != null) {
            CaseGroup group = caseGroupService.findOne(cmd.groupId(), caller);
            ensureWriteAccess(group, caller);
            existingCase.setGroup(group);
        }
        if (cmd.description() != null) {
            existingCase.setDescription(cmd.description());
        }
        if (cmd.preconditions() != null) {
            existingCase.setPreconditions(cmd.preconditions());
        }
        if (cmd.postconditions() != null) {
            existingCase.setPostconditions(cmd.postconditions());
        }
        if (cmd.tags() != null) {
            existingCase.getTags().clear();
            existingCase.getTags().addAll(resolveTags(cmd.tags()));
        }
        if (cmd.steps() != null) {
            existingCase.getCaseSteps().clear();
            caseRepository.saveAndFlush(existingCase);

            List<CaseStep> steps = buildSteps(cmd.steps(), existingCase);
            existingCase.getCaseSteps().addAll(steps);
        }

        caseRepository.save(existingCase);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void delete(int id, User caller) {
        Case testCase = findOne(id, caller);
        ensureWriteAccess(testCase.getGroup(), caller);
        caseRepository.deleteById(id);
    }

    private void ensureCaseTitleIsUnique(String title, Integer groupId, Case currentCase) {
        boolean duplicateExists = caseRepository.existsByTitleAndGroupId(title, groupId);
        if (!duplicateExists) {
            return;
        }

        if (currentCase != null) {
            boolean sameTitle = title.equals(currentCase.getTitle());
            boolean sameGroup = groupId.equals(currentCase.getGroup().getId());
            if (sameTitle && sameGroup) {
                return;
            }
        }

        throw new CaseAlreadyExistsException(
                "Case with title '" + title + "' already exists in group with id '" + groupId + "'"
        );
    }

    private Set<CaseTag> resolveTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<String> normalized = normalizeTags(rawTags);
        List<CaseTag> existing = caseTagRepository.findAllByNameIn(normalized);
        Set<String> existingNames = existing.stream()
                .map(CaseTag::getName)
                .collect(java.util.stream.Collectors.toSet());

        Set<CaseTag> resolved = new LinkedHashSet<>(existing);
        for (String tagName : normalized) {
            if (existingNames.contains(tagName)) {
                continue;
            }
            CaseTag created = new CaseTag();
            created.setName(tagName);
            resolved.add(caseTagRepository.save(created));
        }

        return resolved;
    }

    private Set<String> normalizeTags(Collection<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        for (String rawTag : tags) {
            if (rawTag == null) {
                continue;
            }
            String cleaned = rawTag.trim().toLowerCase();
            if (cleaned.isEmpty()) {
                continue;
            }
            result.add(cleaned);
        }
        return result;
    }

    private boolean hasReadAccess(Case testCase, User caller) {
        if (caller == null) {
            return false;
        }

        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return true;
        }

        CaseGroup group = testCase.getGroup();
        if (group.getProject() == null) {
            return RoleSlugs.TEAMLEAD.equals(slug);
        }

        Integer projectId = group.getProject().getId();
        boolean isOwner = Objects.equals(group.getProject().getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());

        if (RoleSlugs.TEAMLEAD.equals(slug)) {
            return isOwner || isMember;
        }
        return isMember;
    }

    private void ensureWriteAccess(CaseGroup group, User caller) {
        if (caller == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return;
        }

        if (!RoleSlugs.TEAMLEAD.equals(slug)) {
            if (!RoleSlugs.USER.equals(slug)) {
                throw new org.springframework.security.access.AccessDeniedException("No access to modify cases");
            }
            if (group.getProject() == null) {
                throw new org.springframework.security.access.AccessDeniedException("User can only modify cases inside projects");
            }
            Integer projectId = group.getProject().getId();
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());
            if (!isMember) {
                throw new org.springframework.security.access.AccessDeniedException("User can only modify entrusted project cases");
            }
            return;
        }

        if (group.getProject() == null) {
            return;
        }

        Integer projectId = group.getProject().getId();
        boolean isOwner = Objects.equals(group.getProject().getOwner().getId(), caller.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, caller.getId());
        if (!isOwner && !isMember) {
            throw new org.springframework.security.access.AccessDeniedException("Teamlead can only modify cases in owned or entrusted projects");
        }
    }


}
