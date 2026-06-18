import { FormEvent, useEffect, useState } from 'react';
import {
  createRule,
  deleteRule,
  getCategories,
  getRules,
  getTags,
  updateRule,
} from '../api/budgetApi';
import { ApiError } from '../api/client';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { CategorisationRule, Category, Tag } from '../types/api';

const emptyRule = { matchText: '', categoryId: 0, tagId: null as number | null, active: true, priority: 100 };

export function RulesPage() {
  const [rules, setRules] = useState<CategorisationRule[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [form, setForm] = useState(emptyRule);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    refresh();
  }, []);

  function refresh() {
    setLoading(true);
    Promise.all([getRules(), getCategories(), getTags()])
      .then(([ruleResult, categoryResult, tagResult]) => {
        setRules(ruleResult);
        setCategories(categoryResult);
        setTags(tagResult);
        setError(null);
      })
      .catch((exception: Error) => setError(exception.message))
      .finally(() => setLoading(false));
  }

  async function submitRule(event: FormEvent) {
    event.preventDefault();
    if (!form.matchText.trim() || !form.categoryId) {
      setFieldErrors({
        ...(!form.matchText.trim() ? { matchText: 'Enter text to match in imported descriptions.' } : {}),
        ...(!form.categoryId ? { categoryId: 'Choose the category to apply.' } : {}),
      });
      setError('Enter match text and choose a category.');
      return;
    }

    try {
      if (editingId) {
        await updateRule(editingId, form);
      } else {
        await createRule(form);
      }
      setForm(emptyRule);
      setEditingId(null);
      refresh();
      setError(null);
      setFieldErrors({});
    } catch (exception) {
      setError((exception as Error).message);
      setFieldErrors(exception instanceof ApiError ? exception.fields : {});
    }
  }

  function toggleRule(rule: CategorisationRule) {
    updateRule(rule.id, {
      matchText: rule.matchText,
      categoryId: rule.categoryId,
      tagId: rule.tagId,
      active: !rule.active,
      priority: rule.priority,
    }).then(refresh).catch((exception: Error) => setError(exception.message));
  }

  function deleteRuleWithConfirmation(rule: CategorisationRule) {
    if (!window.confirm(`Delete rule "${rule.matchText}"? This cannot be undone.`)) {
      return;
    }

    deleteRule(rule.id).then(refresh).catch((exception: Error) => setError(exception.message));
  }

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Automation</p>
          <h2>Categorisation Rules</h2>
        </div>
      </header>
      <ErrorMessage message={error} fields={fieldErrors} />
      {loading ? <LoadingState label="Loading categorisation rules" /> : null}
      <form className="form-panel rule-form" onSubmit={submitRule}>
        <label>Match text<input value={form.matchText} onChange={(event) => setForm({ ...form, matchText: event.target.value })} /></label>
        <label>Category<select value={form.categoryId || ''} onChange={(event) => setForm({ ...form, categoryId: Number(event.target.value) })}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
        <label>Tag<select value={form.tagId ?? ''} onChange={(event) => setForm({ ...form, tagId: event.target.value ? Number(event.target.value) : null })}><option value="">None</option>{tags.map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select></label>
        <label>Priority<input min="0" type="number" value={form.priority} onChange={(event) => setForm({ ...form, priority: Number(event.target.value) })} /></label>
        <label className="inline-check"><input checked={form.active} type="checkbox" onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Active</label>
        <button type="submit">{editingId ? 'Save rule' : 'Add rule'}</button>
      </form>
      {!loading && rules.length === 0 ? <EmptyState title="No rules yet" detail="Add a rule to categorise imported transactions automatically." /> : null}
      <div className="item-list">
        {rules.map((rule) => (
          <article key={rule.id}>
            <span>
              <strong>{rule.matchText}</strong>
              <small>{categoryName(categories, rule.categoryId)} - {tagName(tags, rule.tagId)} - priority {rule.priority}</small>
            </span>
            <div>
              <button type="button" onClick={() => toggleRule(rule)}>{rule.active ? 'Deactivate' : 'Activate'}</button>
              <button type="button" onClick={() => { setEditingId(rule.id); setForm({ matchText: rule.matchText, categoryId: rule.categoryId, tagId: rule.tagId, active: rule.active, priority: rule.priority }); }}>Edit</button>
              <button type="button" onClick={() => deleteRuleWithConfirmation(rule)}>Delete</button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function categoryName(categories: Category[], id: number) {
  return categories.find((category) => category.id === id)?.name ?? `Category ${id}`;
}

function tagName(tags: Tag[], id: number | null) {
  if (!id) {
    return 'No tag';
  }
  return tags.find((tag) => tag.id === id)?.name ?? `Tag ${id}`;
}
