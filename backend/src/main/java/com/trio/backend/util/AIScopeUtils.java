package com.trio.backend.util;

import com.trio.backend.enums.AIScopeType;

import java.util.UUID;

public final class AIScopeUtils {

    private AIScopeUtils() {
    }

    public static AIScopeType resolveScope(AIScopeType explicitScope, UUID projectId, UUID teamId) {
        if (explicitScope != null) {
            return explicitScope;
        }
        if (teamId != null) {
            return AIScopeType.TEAM;
        }
        if (projectId != null) {
            return AIScopeType.PROJECT;
        }
        return AIScopeType.DEPARTMENT;
    }
}
