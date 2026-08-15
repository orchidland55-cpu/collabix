package com.trio.backend.entity.base;
import jakarta.persistence.*;

import java.util.UUID;
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    public UUID getId() {
        return id;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}