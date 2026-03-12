package ru.rt.rostelecom_tms.service.cases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.cases.CaseGroup;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupAlreadyExistsException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupNotDeletableException;
import ru.rt.rostelecom_tms.domain.cases.exceptions.CaseGroupNotFoundException;
import ru.rt.rostelecom_tms.repository.cases.CaseRepository;
import ru.rt.rostelecom_tms.repository.cases.CaseGroupRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CaseGroupService {

    private final CaseGroupRepository caseGroupRepository;
    private final CaseRepository caseRepository;

    public record CreateGroupCommand(String name, String slug) {
    }

    public record UpdateGroupCommand(String name, String slug) {
    }

    public List<CaseGroup> findAll() {
        return caseGroupRepository.findAll();
    }

    public CaseGroup findOne(int id) {
        return caseGroupRepository.findById(id)
                .orElseThrow(CaseGroupNotFoundException::new);
    }

    @Transactional
    public CaseGroup create(CreateGroupCommand cmd) {
        if (caseGroupRepository.existsByName(cmd.name())) {
            throw new CaseGroupAlreadyExistsException("Case group with name '" + cmd.name() + "' already exists");
        }
        if (caseGroupRepository.existsBySlug(cmd.slug())) {
            throw new CaseGroupAlreadyExistsException("Case group with slug '" + cmd.slug() + "' already exists");
        }

        CaseGroup group = new CaseGroup();
        group.setName(cmd.name());
        group.setSlug(cmd.slug());
        return caseGroupRepository.save(group);
    }

    @Transactional
    public void update(int id, UpdateGroupCommand cmd) {
        CaseGroup group = findOne(id);

        if (cmd.name() != null && !cmd.name().equals(group.getName())) {
            if (caseGroupRepository.existsByName(cmd.name())) {
                throw new CaseGroupAlreadyExistsException("Case group with name '" + cmd.name() + "' already exists");
            }
            group.setName(cmd.name());
        }

        if (cmd.slug() != null && !cmd.slug().equals(group.getSlug())) {
            if (caseGroupRepository.existsBySlug(cmd.slug())) {
                throw new CaseGroupAlreadyExistsException("Case group with slug '" + cmd.slug() + "' already exists");
            }
            group.setSlug(cmd.slug());
        }

        caseGroupRepository.save(group);
    }

    @Transactional
    public void delete(int id) {
        CaseGroup group = findOne(id);
        if (caseRepository.existsByGroupId(id)) {
            throw new CaseGroupNotDeletableException(
                    "Case group with id '" + id + "' cannot be deleted because it still contains cases"
            );
        }
        caseGroupRepository.delete(group);
    }
}
