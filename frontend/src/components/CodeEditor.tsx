import { useEffect, useRef } from "react";
import { Yace } from "yace";
import { code } from "yace/highlighters/code";
import { autoClose, history, preserveIndent, tab } from "yace/plugins";
import styles from "../styles/Workspace.module.css";

type Props = {
  value: string;
  language?: string;
  onChange: (value: string) => void;
  onBlur: () => void;
};

export function CodeEditor({ value, language, onChange, onBlur }: Props) {
  const mountRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<Yace | null>(null);
  const onChangeRef = useRef(onChange);
  const onBlurRef = useRef(onBlur);
  const blurHandlerRef = useRef<() => void>(() => undefined);
  onChangeRef.current = onChange;
  onBlurRef.current = onBlur;

  useEffect(() => {
    if (!mountRef.current) return;
    const editor = new Yace(mountRef.current, {
      value,
      lineNumbers: true,
      highlighters: [code()],
      plugins: [history(), tab(), preserveIndent(), autoClose()],
      styles: {
        fontSize: "13px",
        fontFamily: '"DM Mono", monospace',
        lineHeight: "1.65",
        color: "#dce1d8",
      },
    });
    editor.textarea.setAttribute("aria-label", `${language ?? "Code"} editor`);
    editor.textarea.spellcheck = false;
    editor.onUpdate((nextValue) => onChangeRef.current(nextValue));
    blurHandlerRef.current = () => onBlurRef.current();
    editor.textarea.addEventListener("blur", blurHandlerRef.current);
    editorRef.current = editor;
    return () => {
      editor.textarea.removeEventListener("blur", blurHandlerRef.current);
      editor.destroy();
      editorRef.current = null;
    };
  }, []);

  useEffect(() => {
    editorRef.current?.update({ value });
  }, [value]);

  return <div className={styles.editorMount} ref={mountRef} />;
}
