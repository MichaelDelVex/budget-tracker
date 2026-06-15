import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';

describe('App', () => {
  it('renders the budget tracker layout', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: /budget tracker/i })).toBeInTheDocument();
    expect(screen.getByText(/local-first personal finance/i)).toBeInTheDocument();
  });
});
