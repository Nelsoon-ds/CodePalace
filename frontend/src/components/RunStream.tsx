import { useEffect, useState } from "react";
import {
  EventSourcePolyfill,
  type Event as PolyfillEvent,
  type EventListener as PolyfillEventListener,
  type MessageEvent as PolyfillMessageEvent,
} from "event-source-polyfill";
import { apiUrl, getAccessToken } from "../api/client";
import type { RunEvent } from "../api/types";

type StreamState = "idle" | "connecting" | "live" | "closed" | "error";
const terminal = ["COMPLETED", "FAILED", "TIMED_OUT", "OOM_KILLED", "INTERNAL_ERROR"];

function parseData(raw: string, eventType?: string): RunEvent | null {
  try {
    const value = JSON.parse(raw) as RunEvent;
    return { ...value, event: value.event ?? eventType };
  } catch {
    return eventType ? { event: eventType, data: raw } : null;
  }
}

export function useRunStream(runId?: string) {
  const [events, setEvents] = useState<RunEvent[]>([]);
  const [streamState, setStreamState] = useState<StreamState>("idle");
  const [error, setError] = useState<string>();

  useEffect(() => {
    setEvents([]);
    setError(undefined);
    if (!runId) {
      setStreamState("idle");
      return;
    }

    const source = new EventSourcePolyfill(apiUrl(`/api/runs/${runId}/events`), {
      headers: getAccessToken() ? { Authorization: `Bearer ${getAccessToken()}` } : {},
      heartbeatTimeout: 60_000,
    });
    setStreamState("connecting");
    const handleEvent: PolyfillEventListener = (message: PolyfillEvent) => {
      if (!("data" in message) || typeof message.data !== "string") return;
      const event = parseData(message.data, message.type);
      if (!event) return;
      setEvents((current) => [...current, event]);
      const eventData =
        event.data && typeof event.data === "object"
          ? (event.data as { status?: string })
          : undefined;
      const status = event.status ?? eventData?.status ??
        (typeof event.data === "string" ? event.data : "");
      if (terminal.some((value) => status.toUpperCase().includes(value))) {
        source.close();
        setStreamState("closed");
      }
    };
    source.onopen = () => setStreamState("live");
    source.onmessage = (message: PolyfillMessageEvent) => handleEvent(message);
    ["queued", "running", "assertion", "summary", "status", "error"].forEach((eventName) => {
      source.addEventListener(eventName, handleEvent);
    });
    source.onerror = () => {
      if (source.readyState === EventSourcePolyfill.CLOSED) {
        setError("The live results connection was closed.");
        setStreamState("error");
      }
    };

    return () => source.close();
  }, [runId]);

  return { events, streamState, error };
}
