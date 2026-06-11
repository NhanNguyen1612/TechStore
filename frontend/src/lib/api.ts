import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios";
import { authStorage } from "./authStorage";
import type { ApiResponse, AuthPayload } from "../types/api";

const baseURL = import.meta.env.VITE_API_URL?.replace(/\/$/, "") ?? "";

interface RetryableRequest extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

export const api = axios.create({
  baseURL,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = authStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.data instanceof FormData) {
    config.headers.delete("Content-Type");
  }
  return config;
});

let refreshPromise: Promise<string> | null = null;

const refreshAccessToken = async () => {
  const refreshToken = authStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error("Không có mã làm mới phiên đăng nhập");
  }
  const response = await axios.post<ApiResponse<AuthPayload>>(
    `${baseURL}/api/auth/refresh`,
    { refreshToken },
  );
  const payload = response.data.data;
  authStorage.setTokens(payload.accessToken, payload.refreshToken);
  window.dispatchEvent(new CustomEvent("auth:refreshed"));
  return payload.accessToken;
};

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetryableRequest | undefined;
    const isAuthEndpoint =
      original?.url?.includes("/api/auth/login") ||
      original?.url?.includes("/api/auth/register") ||
      original?.url?.includes("/api/auth/refresh");

    if (
      error.response?.status !== 401 ||
      !original ||
      original._retry ||
      isAuthEndpoint
    ) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const token = await refreshPromise;
      original.headers.Authorization = `Bearer ${token}`;
      return api(original);
    } catch (refreshError) {
      authStorage.clear();
      window.dispatchEvent(new CustomEvent("auth:expired"));
      return Promise.reject(refreshError);
    }
  },
);

export const apiData = async <T>(
  request: Promise<{ data: ApiResponse<T> }>,
): Promise<T> => {
  const response = await request;
  return response.data.data;
};
