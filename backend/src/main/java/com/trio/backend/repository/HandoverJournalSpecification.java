package com.trio.backend.repository;

import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.HandoverJournal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HandoverJournalSpecification {

    private HandoverJournalSpecification() {
    }

    public static Specification<HandoverJournal> accessibleFilter(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            HandoverEntry.Shift shift,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("workspace").get("id"), workspaceId));
            predicates.add(cb.equal(root.get("status"), HandoverJournal.HandoverJournalStatus.ACTIVE));

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (shift != null) {
                predicates.add(cb.equal(root.get("shift"), shift));
            }
            if (from != null && to != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("journalDate"), from));
                predicates.add(cb.lessThan(root.get("journalDate"), to));
            }

            if (query != null) {
                query.orderBy(
                        cb.desc(root.get("journalDate")),
                        cb.desc(root.get("generationDate"))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
