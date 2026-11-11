import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { apiRequest, authRequest, updateAuth } from "./client";
import type {
  AuthResponse,
  Kata,
  RunResult,
  Session,
  Workspace,
} from "./types";

function collection<T>(value: T[] | { content?: T[]; items?: T[] } | null): T[] {
  if (Array.isArray(value)) return value;
  return value?.content ?? value?.items ?? [];
}

export function useWorkspaces() {
  return useQuery({
    queryKey: ["workspaces"],
    queryFn: async () => collection(await apiRequest<Workspace[] | { content?: Workspace[] }>("/api/workspaces")),
  });
}

export function useWorkspaceMutations() {
  const queryClient = useQueryClient();
  const create = useMutation({
    mutationFn: (name: string) =>
      apiRequest<Workspace>("/api/workspaces", {
        method: "POST",
        body: JSON.stringify({ name }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workspaces"] }),
  });
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiRequest<void>(`/api/workspaces/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workspaces"] }),
  });
  return { create, remove };
}

export function useKatas() {
  return useQuery({
    queryKey: ["katas"],
    queryFn: async () => collection(await apiRequest<Kata[] | { content?: Kata[] }>("/api/katas")),
  });
}

export function useSession(sessionId?: string) {
  return useQuery({
    queryKey: ["sessions", sessionId],
    queryFn: () => apiRequest<Session>(`/api/sessions/${sessionId}`),
    enabled: Boolean(sessionId),
  });
}

export function useSessionMutations(sessionId?: string) {
  const queryClient = useQueryClient();
  const saveDraft = useMutation({
    mutationFn: ({ targetSessionId, code }: { targetSessionId: string; code: string }) =>
      apiRequest<void>(`/api/sessions/${targetSessionId}/draft`, {
        method: "PUT",
        body: JSON.stringify({ code }),
      }),
  });
  const submit = useMutation({
    mutationFn: (code: string) =>
      apiRequest<RunResult>(`/api/sessions/${sessionId}/submissions`, {
        method: "POST",
        body: JSON.stringify({ code }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sessions", sessionId] }),
  });
  const createSession = useMutation({
    mutationFn: ({ workspaceId, kataId }: { workspaceId: string; kataId: string }) =>
      apiRequest<Session>(`/api/workspaces/${workspaceId}/sessions`, {
        method: "POST",
        body: JSON.stringify({ kataId }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workspaces"] }),
  });
  return { saveDraft, submit, createSession };
}

export function useRun(runId?: string) {
  return useQuery({
    queryKey: ["runs", runId],
    queryFn: () => apiRequest<RunResult>(`/api/runs/${runId}`),
    enabled: Boolean(runId),
  });
}

export async function authenticate(path: "/api/auth/login" | "/api/auth/register", email: string, password: string) {
  const result = await authRequest(path, { email, password });
  if (!result || typeof result !== "object") {
    throw new Error("The auth endpoint returned no token response.");
  }
  updateAuth(result as AuthResponse);
  if (!result.accessToken && !result.token && !result.jwt) {
    throw new Error("The auth response did not include an access token.");
  }
  return result;
}
