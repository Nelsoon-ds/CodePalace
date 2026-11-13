export type Language = "javascript" | "java" | string;

export interface AuthResponse {
  accessToken?: string;
  token?: string;
  jwt?: string;
  refreshToken?: string;
  user?: { email?: string };
}

export interface Workspace {
  id: string;
  name: string;
  createdAt?: string;
  sessions?: SessionSummary[];
}

export interface SessionSummary {
  id: string;
  workspaceId?: string;
  kataId?: string;
  name?: string;
  title?: string;
  language?: Language;
  status?: string;
  updatedAt?: string;
}

export interface Kata {
  id: string;
  title: string;
  statementMd?: string;
  description?: string;
  difficulty?: string;
  language?: Language;
  starterCode?: string;
  category?: string | { name?: string };
  tags?: Array<string | { name?: string }>;
}

export interface Submission {
  id?: string;
  code?: string;
  createdAt?: string;
}

export interface Session extends SessionSummary {
  draft?: string | { code?: string };
  draftCode?: string;
  submissions?: Submission[];
  latestSubmission?: Submission;
  kata?: Kata;
}

export interface Assertion {
  name: string;
  passed: boolean;
  ms?: number;
  diff?: string | null;
}

export interface RunResult {
  id?: string;
  runId?: string;
  status?: string;
  durationMs?: number;
  totalTests?: number;
  passedTests?: number;
  failedTests?: number;
  assertions?: Assertion[];
  outputTail?: string;
}

export interface RunEvent {
  event?: string;
  data?: unknown;
  name?: string;
  passed?: boolean;
  ms?: number;
  diff?: string | null;
  status?: string;
  message?: string;
}
