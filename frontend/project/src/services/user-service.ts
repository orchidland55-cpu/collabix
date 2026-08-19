import { apiClient } from '../lib/api';
import type {
  UserResponse,
  UserProfileResponse,
  CreateUserRequest,
  UpdateUserRequest,
  UpdateProfileRequest,
  AssignRolesRequest,
  UserSearchCriteria,
  UserStatisticsResponse,
  PageResponse,
} from '../types';

export function userService(workspaceId: string) {
  const base = `/workspaces/${workspaceId}/users`;

  return {
    list: () =>
      apiClient.get<UserResponse[]>(base),

    search: (criteria: UserSearchCriteria, page: number, size: number, sort?: string) => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (sort) params.set('sort', sort);
      if (criteria.keyword) params.set('keyword', criteria.keyword);
      if (criteria.firstName) params.set('firstName', criteria.firstName);
      if (criteria.lastName) params.set('lastName', criteria.lastName);
      if (criteria.email) params.set('email', criteria.email);
      if (criteria.status) params.set('status', criteria.status);
      if (criteria.role) params.set('role', criteria.role);
      if (criteria.departmentId) params.set('departmentId', criteria.departmentId);
      if (criteria.teamId) params.set('teamId', criteria.teamId);
      if (criteria.memberType) params.set('memberType', criteria.memberType);
      if (criteria.createdAfter) params.set('createdAfter', criteria.createdAfter);
      if (criteria.createdBefore) params.set('createdBefore', criteria.createdBefore);
      if (criteria.lastLoginAfter) params.set('lastLoginAfter', criteria.lastLoginAfter);
      if (criteria.lastLoginBefore) params.set('lastLoginBefore', criteria.lastLoginBefore);
      const q = params.toString();
      return apiClient.get<PageResponse<UserResponse>>(`${base}/search?${q}`);
    },

    getById: (id: string) =>
      apiClient.get<UserResponse>(`${base}/${id}`),

    create: (data: CreateUserRequest) =>
      apiClient.post<UserResponse>(base, data),

    update: (id: string, data: UpdateUserRequest) =>
      apiClient.put<UserResponse>(`${base}/${id}`, data),

    delete: (id: string) =>
      apiClient.delete<void>(`${base}/${id}`),

    deletePermanent: (id: string) =>
      apiClient.delete<void>(`${base}/${id}/permanent`),

    activate: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/activate`),

    deactivate: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/deactivate`),

    suspend: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/suspend`),

    reactivate: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/reactivate`),

    archive: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/archive`),

    restore: (id: string) =>
      apiClient.put<UserResponse>(`${base}/${id}/restore`),

    assignRoles: (id: string, data: AssignRolesRequest) =>
      apiClient.put<UserResponse>(`${base}/${id}/roles`, data),

    updateProfile: (data: UpdateProfileRequest) =>
      apiClient.put<UserProfileResponse>(`${base}/me`, data),

    statistics: () =>
      apiClient.get<UserStatisticsResponse>(`${base}/statistics`),
  };
}
