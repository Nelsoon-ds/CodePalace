import type { AuthResponse } from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";
let accessToken: string | null = null;
let refreshToken: string | null = null;
let refreshPromise: Promise<boolean> | null = null;
const authListeners = new Set<() => void>();

export function getAccessToken() {
  return accessToken;
}

export function updateAuth(response: AuthResponse) {
  accessToken = response.accessToken ?? response.token ?? response.jwt ?? null;
  refreshToken = response.refreshToken ?? null;
  authListeners.forEach((listener) => listener());
  return accessToken;
}

export function clearAuth() {
  accessToken = null;
  refreshToken = null;
  authListeners.forEach((listener) => listener());
}

export function subscribeAuth(listener: () => void) {
  authListeners.add(listener);
  return () => authListeners.delete(listener);
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function attemptRefresh() {
  const token = refreshToken ?? accessToken;
  if (!token) return false;
  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(refreshToken ? { refreshToken } : {}),
    })
      .then(async (response) => {
        if (!response.ok) return false;
        const data = (await response.json()) as AuthResponse;
        return Boolean(updateAuth(data));
      })
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.text();
    throw new ApiError(body || `Request failed (${response.status})`, response.status);
  }
  if (response.status === 204) return undefined as T;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return undefined as T;
  return (await response.json()) as T;
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  retryAuth = true,
): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (response.status === 401 && retryAuth) {
    if (await attemptRefresh()) return apiRequest<T>(path, init, false);
    clearAuth();
  }
  return parseResponse<T>(response);
}

export async function authRequest(path: string, body: unknown) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return parseResponse<AuthResponse>(response);
}

export function apiUrl(path: string) {
  return `${API_BASE}${path}`;
}
