export type Permission = {
  id: string;
  name: string;
  description: string;
  module: string;
  resource: string;
  action: string;
  createdAt: string;
};

export type Role = {
  id: string;
  name: string;
  description: string;
  companyId: string | null;
  isSystem: boolean;
  isActive: boolean;
  permissions: Permission[];
  createdAt: string;
  updatedAt: string;
};

export type CreateRoleRequest = {
  name: string;
  description?: string;
  permissionIds?: string[];
};

export type UpdateRoleRequest = {
  description?: string;
  isActive?: boolean;
  permissionIds?: string[];
};

export type AssignRoleRequest = {
  roleId: string;
};

export type AssignPermissionRequest = {
  permissionId: string;
};

export type PermissionModule = {
  module: string;
  permissions: Permission[];
};
