import api from "@/lib/api";
import type {
  LoginResponse,
  User,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/auth.types";

export const AuthService = {
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>("/auth/login", data);
    return response.data;
  },

  async register(data: RegisterRequest): Promise<void> {
    await api.post("/auth/register", {
      name: data.name,
      email: data.email,
      password: data.password,
      companyId: data.companyId ?? "",
    });
  },

  async logout(): Promise<void> {
    await api.post("/auth/logout", {});
  },

  async forgotPassword(_data: ForgotPasswordRequest): Promise<void> {
    await api.post("/auth/forgot-password", _data);
  },

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await api.post("/auth/reset-password", {
      token: data.token,
      newPassword: data.password,
    });
  },

  async refresh(refreshToken: string): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>("/auth/refresh", { refreshToken });
    return response.data;
  },

  async me(): Promise<User> {
    const response = await api.get<User>("/auth/me");
    return response.data;
  },
};
