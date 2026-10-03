import api from "@/lib/api";
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  InviteUserRequest,
  AcceptInviteRequest,
  UpdateProfileRequest,
  ListUsersParams,
  PageResponse,
} from "../types/user.types";

const BASE_PATH = "/users";

export const UserService = {
  async list(params?: ListUsersParams): Promise<PageResponse<User>> {
    const response = await api.get<PageResponse<User>>(BASE_PATH, { params });
    return response.data;
  },

  async findById(id: string): Promise<User> {
    const response = await api.get<User>(`${BASE_PATH}/${id}`);
    return response.data;
  },

  async create(data: CreateUserRequest): Promise<User> {
    const response = await api.post<User>(BASE_PATH, data);
    return response.data;
  },

  async update(id: string, data: UpdateUserRequest): Promise<User> {
    const response = await api.put<User>(`${BASE_PATH}/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`${BASE_PATH}/${id}`);
  },

  async activate(id: string): Promise<void> {
    await api.put(`${BASE_PATH}/${id}/activate`);
  },

  async deactivate(id: string): Promise<void> {
    await api.put(`${BASE_PATH}/${id}/deactivate`);
  },

  async invite(data: InviteUserRequest): Promise<User> {
    const response = await api.post<User>(`${BASE_PATH}/invite`, data);
    return response.data;
  },

  async acceptInvite(data: AcceptInviteRequest): Promise<User> {
    const response = await api.post<User>(`${BASE_PATH}/accept-invite`, data);
    return response.data;
  },

  async getProfile(): Promise<User> {
    const response = await api.get<User>(`${BASE_PATH}/profile`);
    return response.data;
  },

  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await api.put<User>(`${BASE_PATH}/profile`, data);
    return response.data;
  },
};
