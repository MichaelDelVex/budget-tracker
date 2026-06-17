interface LoadingStateProps {
  label?: string;
}

export function LoadingState({ label = 'Loading' }: LoadingStateProps) {
  return <p className="status-text loading-state" aria-live="polite">{label}...</p>;
}
