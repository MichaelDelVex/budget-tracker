interface ErrorMessageProps {
  message: string | null;
  fields?: Record<string, string>;
}

export function ErrorMessage({ message, fields = {} }: ErrorMessageProps) {
  if (!message) {
    return null;
  }

  const entries = Object.entries(fields);

  return (
    <section className="error-message" role="alert">
      <p>{message}</p>
      {entries.length > 0 ? (
        <ul>
          {entries.map(([field, detail]) => (
            <li key={field}>{field}: {detail}</li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
