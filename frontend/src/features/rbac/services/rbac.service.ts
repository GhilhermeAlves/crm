import api from "@/lib/api";
import type {
  Role,
  Permission,
  CreateRoleRequest,
  UpdateRoleRequest,
  AssignRoleRequest,
  AssignPermissionRequest,
} from "../types/rbac.types";

const ROLES_PATH = "/roles";
const PERMISSIONS_PATH = "/permissions";
const USERS_PATH = "/users";

export const RbacService = {
  async listRoles(): Promise<Role[]> {
    const response = await api.get<Role[]>(ROLES_PATH);
    return response.data;
  },

  async getRoleById(id: string): Promise<Role> {
    const response = await api.get<Role>(`${ROLES_PATH}/${id}`);
    return response.data;
  },

  async createRole(data: CreateRoleRequest): Promise<Role> {
    const response = await api.post<Role>(ROLES_PATH, data);
    return response.data;
  },

  async updateRole(id: string, data: UpdateRoleRequest): Promise<Role> {
    const response = await api.put<Role>(`${ROLES_PATH}/${id}`, data);
    return response.data;
  },

  async deleteRole(id: string): Promise<void> {
    await api.delete(`${ROLES_PATH}/${id}`);
  },

  async assignPermission(
    roleId: string,
    data: AssignPermissionRequest,
  ): Promise<void> {
    await api.post(`${ROLES_PATH}/${roleId}/permissions`, data);
  },

  async removePermission(roleId: string, permissionId: string): Promise<void> {
    await api.delete(`${ROLES_PATH}/${roleId}/permissions/${permissionId}`);
  },

  async assignRoleToUser(
    userId: string,
    data: AssignRoleRequest,
  ): Promise<void> {
    await api.post(`${ROLES_PATH}/user/${userId}`, data);
  },

  async removeRoleFromUser(userId: string, roleId: string): Promise<void> {
    await api.delete(`${ROLES_PATH}/user/${userId}/roles/${roleId}`);
  },

  async getUserRoles(userId: string): Promise<Role[]> {
    const response = await api.get<Role[]>(`${ROLES_PATH}/user/${userId}`);
    return response.data;
  },

  async listAllPermissions(): Promise<Permission[]> {
    const response = await api.get<Permission[]>(PERMISSIONS_PATH);
    return response.data;
  },

  async listPermissionsByModule(module: string): Promise<Permission[]> {
    const response = await api.get<Permission[]>(
      `${PERMISSIONS_PATH}/module/${module}`,
    );
    return response.data;
  },

  async getPermissionById(id: string): Promise<Permission> {
    const response = await api.get<Permission>(`${PERMISSIONS_PATH}/${id}`);
    return response.data;
  },
};
