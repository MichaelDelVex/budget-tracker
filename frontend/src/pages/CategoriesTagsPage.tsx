import { FormEvent, useEffect, useState } from 'react';
import {
  createCategory,
  createTag,
  deleteCategory,
  deleteTag,
  getCategories,
  getTags,
  updateCategory,
  updateTag,
} from '../api/budgetApi';
import { ApiError } from '../api/client';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { Category, CategoryType, Tag } from '../types/api';

const emptyCategory = { name: '', type: 'EXPENSE' as CategoryType, defaultCategory: false, active: true, sortOrder: 100 };
const emptyTag = { name: '', color: '#336699' };

export function CategoriesTagsPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [categoryForm, setCategoryForm] = useState(emptyCategory);
  const [tagForm, setTagForm] = useState(emptyTag);
  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null);
  const [editingTagId, setEditingTagId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    refresh();
  }, []);

  function refresh() {
    setLoading(true);
    Promise.all([getCategories(), getTags()])
      .then(([categoryResult, tagResult]) => {
        setCategories(categoryResult);
        setTags(tagResult);
        setError(null);
      })
      .catch((exception: Error) => setError(exception.message))
      .finally(() => setLoading(false));
  }

  async function submitCategory(event: FormEvent) {
    event.preventDefault();
    if (!categoryForm.name.trim()) {
      setError('Category name is required.');
      setFieldErrors({ name: 'Enter a category name.' });
      return;
    }

    try {
      if (editingCategoryId) {
        await updateCategory(editingCategoryId, categoryForm);
      } else {
        await createCategory(categoryForm);
      }
      setCategoryForm(emptyCategory);
      setEditingCategoryId(null);
      refresh();
      setError(null);
      setFieldErrors({});
    } catch (exception) {
      setError((exception as Error).message);
      setFieldErrors(exception instanceof ApiError ? exception.fields : {});
    }
  }

  async function submitTag(event: FormEvent) {
    event.preventDefault();
    const validationErrors: Record<string, string> = {};
    if (!tagForm.name.trim()) {
      validationErrors.name = 'Enter a tag name.';
    }
    if (!/^#[0-9A-Fa-f]{6}$/.test(tagForm.color)) {
      validationErrors.color = 'Use a hex colour like #336699.';
    }
    if (Object.keys(validationErrors).length > 0) {
      setError('Tag details need attention.');
      setFieldErrors(validationErrors);
      return;
    }

    try {
      if (editingTagId) {
        await updateTag(editingTagId, tagForm);
      } else {
        await createTag(tagForm);
      }
      setTagForm(emptyTag);
      setEditingTagId(null);
      refresh();
      setError(null);
      setFieldErrors({});
    } catch (exception) {
      setError((exception as Error).message);
      setFieldErrors(exception instanceof ApiError ? exception.fields : {});
    }
  }

  function confirmDelete(label: string) {
    return window.confirm(`Delete ${label}? This cannot be undone.`);
  }

  function deleteCategoryWithConfirmation(category: Category) {
    if (!confirmDelete(category.name)) {
      return;
    }

    deleteCategory(category.id).then(refresh).catch((exception: Error) => setError(exception.message));
  }

  function deleteTagWithConfirmation(tag: Tag) {
    if (!confirmDelete(tag.name)) {
      return;
    }

    deleteTag(tag.id).then(refresh).catch((exception: Error) => setError(exception.message));
  }

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Organise</p>
          <h2>Categories and Tags</h2>
        </div>
      </header>
      <ErrorMessage message={error} fields={fieldErrors} />
      {loading ? <LoadingState label="Loading categories and tags" /> : null}
      <div className="two-column">
        <section className="work-panel">
          <h3>Categories</h3>
          <form className="compact-form" onSubmit={submitCategory}>
            <input aria-label="Category name" placeholder="Name" value={categoryForm.name} onChange={(event) => setCategoryForm({ ...categoryForm, name: event.target.value })} />
            <select aria-label="Category type" value={categoryForm.type} onChange={(event) => setCategoryForm({ ...categoryForm, type: event.target.value as CategoryType })}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select>
            <input aria-label="Category sort order" min="0" type="number" value={categoryForm.sortOrder} onChange={(event) => setCategoryForm({ ...categoryForm, sortOrder: Number(event.target.value) })} />
            <label className="inline-check"><input checked={categoryForm.active} type="checkbox" onChange={(event) => setCategoryForm({ ...categoryForm, active: event.target.checked })} /> Active</label>
            <button type="submit">{editingCategoryId ? 'Save category' : 'Add category'}</button>
          </form>
          {!loading && categories.length === 0 ? <EmptyState title="No categories" detail="Add a category to classify transactions." /> : null}
          <div className="item-list">{categories.map((category) => <article key={category.id}><span><strong>{category.name}</strong><small>{category.type} - sort {category.sortOrder}</small></span><div><button type="button" onClick={() => { setEditingCategoryId(category.id); setCategoryForm(category); }}>Edit</button><button type="button" onClick={() => deleteCategoryWithConfirmation(category)}>Delete</button></div></article>)}</div>
        </section>
        <section className="work-panel">
          <h3>Tags</h3>
          <form className="compact-form" onSubmit={submitTag}>
            <input aria-label="Tag name" placeholder="Name" value={tagForm.name} onChange={(event) => setTagForm({ ...tagForm, name: event.target.value })} />
            <input aria-label="Tag color" value={tagForm.color} onChange={(event) => setTagForm({ ...tagForm, color: event.target.value })} />
            <button type="submit">{editingTagId ? 'Save tag' : 'Add tag'}</button>
          </form>
          {!loading && tags.length === 0 ? <EmptyState title="No tags" detail="Add a tag for optional transaction labels." /> : null}
          <div className="item-list">{tags.map((tag) => <article key={tag.id}><span><i style={{ backgroundColor: tag.color }} /> <strong>{tag.name}</strong><small>{tag.color}</small></span><div><button type="button" onClick={() => { setEditingTagId(tag.id); setTagForm(tag); }}>Edit</button><button type="button" onClick={() => deleteTagWithConfirmation(tag)}>Delete</button></div></article>)}</div>
        </section>
      </div>
    </section>
  );
}
