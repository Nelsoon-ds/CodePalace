import { create } from "zustand";

type SaveState = "saved" | "saving" | "unsaved" | "error";
type UiState = {
  consoleOpen: boolean;
  saveState: SaveState;
  toggleConsole: () => void;
  setSaveState: (saveState: SaveState) => void;
};

export const useUiStore = create<UiState>((set) => ({
  consoleOpen: true,
  saveState: "saved",
  toggleConsole: () => set((state) => ({ consoleOpen: !state.consoleOpen })),
  setSaveState: (saveState) => set({ saveState }),
}));
