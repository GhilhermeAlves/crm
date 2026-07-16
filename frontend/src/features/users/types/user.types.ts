export type UserStatus = "active" | "inactive" | "locked" | "pending";

export type User = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  name: string;
  phone: string;
  department: string;
  jobTitle: string;
  avatarUrl: string | null;
  companyId: string;
  status: UserStatus;
  isActive: boolean;
  language: string;
  timezone: string;
  notes: string;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateUserRequest = {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  department?: string;
  jobTitle?: string;
  language?: string;
  timezone?: string;
  notes?: string;
};

export type UpdateUserRequest = Partial<CreateUserRequest> & {
  avatarUrl?: string;
};

export type InviteUserRequest = {
  firstName: string;
  lastName: string;
  email: string;
  department?: string;
  jobTitle?: string;
};

export type AcceptInviteRequest = {
  token: string;
  password: string;
};

export type UpdateProfileRequest = {
  firstName?: string;
  lastName?: string;
  phone?: string;
  department?: string;
  jobTitle?: string;
  language?: string;
  timezone?: string;
  notes?: string;
  avatarUrl?: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type ListUsersParams = {
  page?: number;
  pageSize?: number;
  search?: string;
  status?: UserStatus;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
};
