import { decode, escapeHtml as e, rubyHtml, stripToBase, toHtml } from './public/richtext.js';

export const LIMITS = {
    varchar: 255,     // VARCHAR(255): characters
    text: 65535,      // TEXT(65535): bytes
};

// Suggestions only; course_level is a free VARCHAR(255).
const LEVEL_SUGGESTIONS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'JLPT N5', 'JLPT N4', 'JLPT N3', 'JLPT N2', 'JLPT N1', 'Beginner', 'Intermediate', 'Advanced'];

const formatDate = value => (value ? new Date(value).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '–');

function layout({ title, breadcrumbs = [], body, scripts = false }) {
    const crumbs = [['Courses', '/admin'], ...breadcrumbs]
        .map(([label, href], i, all) => (i === all.length - 1 || !href
            ? `<span>${e(label)}</span>`
            : `<a href="${href}">${e(label)}</a>`))
        .join('<span class="crumb-sep">/</span>');
    return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${e(title)} · Glossa Admin</title>
<link rel="stylesheet" href="/admin/assets/admin.css">
</head>
<body>
<header class="topbar">
  <a class="brand" href="/admin">Glossa <span>Admin</span></a>
  <nav class="crumbs">${crumbs}</nav>
</header>
<main>
${body}
</main>
${scripts ? FURIGANA_DIALOG + '<script type="module" src="/admin/assets/admin.js"></script>' : ''}
</body>
</html>`;
}

const FURIGANA_DIALOG = `
<dialog id="furigana-dialog">
  <form>
    <h2>Add furigana</h2>
    <label>Text<input name="base" placeholder="漢字" autocomplete="off"></label>
    <label>Reading<input name="reading" placeholder="かんじ" autocomplete="off"></label>
    <p class="hint">The characters { } | are not allowed</p>
    <div class="furigana-preview ruby-text" hidden></div>
    <div class="actions">
      <button type="button" class="btn ghost" data-action="cancel">Cancel</button>
      <button type="submit" class="btn primary" data-action="add">Add</button>
    </div>
  </form>
</dialog>`;

function errorList(errors) {
    const messages = Object.values(errors);
    if (messages.length === 0) return '';
    return `<div class="alert" role="alert"><strong>Please fix the following:</strong><ul>${messages.map(m => `<li>${e(m)}</li>`).join('')}</ul></div>`;
}

function field({ name, label, value, errors, hint = '', maxlength = LIMITS.varchar, list = '', extra = '' }) {
    return `
<div class="field${errors[name] ? ' has-error' : ''}">
  <label for="${name}">${label}</label>
  <div class="input-row">
    <input id="${name}" name="${name}" value="${e(value ?? '')}" maxlength="${maxlength}" required autocomplete="off"${list ? ` list="${list}"` : ''}>
    ${extra}
  </div>
  ${errors[name] ? `<p class="error">${e(errors[name])}</p>` : hint ? `<p class="hint">${hint}</p>` : ''}
</div>`;
}

// ---------------------------------------------------------------- courses

export function courseList(courses) {
    const rows = courses.map(c => `
<a class="card course-card" href="/admin/courses/${c.id}">
  <div class="card-title">${rubyHtml(c.course_name)}</div>
  <div class="tags"><span class="tag">${e(c.course_language)}</span><span class="tag subtle">${e(c.course_level)}</span></div>
  <p class="card-desc">${e(stripToBase(c.course_description))}</p>
  <div class="meta">Updated ${formatDate(c.updated_at)}</div>
</a>`).join('');

    return layout({
        title: 'Courses',
        body: `
<div class="page-head">
  <div><h1>Official courses</h1><p class="sub">${courses.length} ${courses.length === 1 ? 'course' : 'courses'}</p></div>
  <a class="btn primary" href="/admin/courses/new">+ Create Course</a>
</div>
${courses.length ? `<div class="grid">${rows}</div>` : `<div class="empty">No courses yet. Create the first one.</div>`}`,
    });
}

export function courseForm({ values = {}, errors = {}, languages = [] }) {
    const options = (id, items) => `<datalist id="${id}">${[...new Set(items)].map(v => `<option value="${e(v)}">`).join('')}</datalist>`;
    return layout({
        title: 'Create course',
        breadcrumbs: [['New course']],
        scripts: true,
        body: `
<div class="page-head"><h1>Create course</h1></div>
${errorList(errors)}
<form class="panel form" method="post" action="/admin/courses" data-confirm-discard novalidate>
  ${field({ name: 'course_name', label: 'Name', value: values.course_name, errors, hint: 'Up to 255 characters. Use ふ to add furigana.',
        extra: '<button type="button" class="btn furigana" data-furigana-for="course_name" title="Add furigana">ふ</button>' })}
  <div class="field-pair">
    ${field({ name: 'course_language', label: 'Language', value: values.course_language, errors, list: 'languages', hint: 'e.g. Japanese' })}
    ${field({ name: 'course_level', label: 'Level', value: values.course_level, errors, list: 'levels', hint: 'e.g. A1 or JLPT N5' })}
  </div>
  ${options('languages', languages)}
  ${options('levels', LEVEL_SUGGESTIONS)}
  <div class="field${errors.course_description ? ' has-error' : ''}">
    <label for="course_description">Description</label>
    <textarea id="course_description" name="course_description" rows="6" required>${e(values.course_description ?? '')}</textarea>
    ${errors.course_description ? `<p class="error">${e(errors.course_description)}</p>` : '<p class="hint">Plain text, up to 65,535 bytes.</p>'}
  </div>
  <div class="actions">
    <a class="btn ghost" href="/admin">Cancel</a>
    <button class="btn primary" type="submit">Create course</button>
  </div>
</form>`,
    });
}

export function courseDetail(course, lectures) {
    const items = lectures.map((l, i) => `
<a class="card lecture-row" href="/admin/lectures/${l.id}">
  <span class="index">${i + 1}</span>
  <span class="lecture-name">${rubyHtml(l.lecture_name)}</span>
  <span class="meta">Updated ${formatDate(l.updated_at)}</span>
</a>`).join('');

    return layout({
        title: stripToBase(course.course_name),
        breadcrumbs: [[stripToBase(course.course_name)]],
        body: `
<section class="panel course-head">
  <h1>${rubyHtml(course.course_name)}</h1>
  <div class="tags"><span class="tag">${e(course.course_language)}</span><span class="tag subtle">${e(course.course_level)}</span></div>
  <p class="desc">${e(course.course_description)}</p>
  <div class="meta">Created ${formatDate(course.created_at)} · Updated ${formatDate(course.updated_at)}</div>
</section>
<div class="page-head">
  <div><h2>Lectures</h2><p class="sub">${lectures.length} ${lectures.length === 1 ? 'lecture' : 'lectures'}</p></div>
  <a class="btn primary" href="/admin/courses/${course.id}/lectures/new">+ Create Lecture</a>
</div>
${lectures.length ? `<div class="list">${items}</div>` : `<div class="empty">This course has no lectures yet.</div>`}`,
    });
}

// ---------------------------------------------------------------- lectures

export function lectureForm({ course, lecture = null, values = {}, errors = {} }) {
    const isEdit = lecture !== null;
    const action = isEdit ? `/admin/lectures/${lecture.id}` : `/admin/courses/${course.id}/lectures`;
    const title = isEdit ? 'Edit lecture' : 'Create lecture';
    const crumbs = [[stripToBase(course.course_name), `/admin/courses/${course.id}`]];
    if (isEdit) crumbs.push([stripToBase(lecture.lecture_name), `/admin/lectures/${lecture.id}`]);
    crumbs.push([isEdit ? 'Edit' : 'New lecture']);

    return layout({
        title,
        breadcrumbs: crumbs,
        scripts: true,
        body: `
<div class="page-head"><h1>${title}</h1></div>
${errorList(errors)}
<form id="lecture-form" class="panel form" method="post" action="${action}" data-confirm-discard novalidate>
  ${field({ name: 'lecture_name', label: 'Title', value: values.lecture_name, errors, hint: 'Up to 255 characters. Use ふ to add furigana.',
        extra: '<button type="button" class="btn furigana" data-furigana-for="lecture_name" title="Add furigana">ふ</button>' })}
  <div class="field${errors.lecture_contents ? ' has-error' : ''}">
    <div class="label-row">
      <label for="rich-editor">Contents</label>
      <button type="button" class="btn ghost small" id="rich-preview-toggle" aria-pressed="false">Preview</button>
    </div>
    <div class="rte">
      <div id="rich-toolbar" class="rte-toolbar">
        <div class="rte-main"></div>
        <div class="rte-panel" hidden></div>
      </div>
      <div id="rich-editor" class="rte-content" data-input="lecture_contents" data-max-bytes="${LIMITS.text}" data-placeholder="Write the lecture…" role="textbox" aria-multiline="true"></div>
      <div id="rich-preview" class="rte-content ruby-text" hidden></div>
    </div>
    <input type="hidden" id="lecture_contents" name="lecture_contents" value="${e(values.lecture_contents ?? '')}">
    ${errors.lecture_contents ? `<p class="error">${e(errors.lecture_contents)}</p>` : ''}
    <p class="hint" id="rich-counter"></p>
  </div>
  <div class="actions">
    <a class="btn ghost" href="${isEdit ? `/admin/lectures/${lecture.id}` : `/admin/courses/${course.id}`}">Cancel</a>
    <button class="btn primary" type="submit">${isEdit ? 'Save changes' : 'Create lecture'}</button>
  </div>
</form>`,
    });
}

export function lectureDetail(course, lecture) {
    return layout({
        title: stripToBase(lecture.lecture_name),
        breadcrumbs: [[stripToBase(course.course_name), `/admin/courses/${course.id}`], [stripToBase(lecture.lecture_name)]],
        body: `
<div class="page-head">
  <h1 class="ruby-text">${rubyHtml(lecture.lecture_name)}</h1>
  <a class="btn primary" href="/admin/lectures/${lecture.id}/edit">Edit</a>
</div>
<article class="panel lecture-body ruby-text">${toHtml(decode(lecture.lecture_contents))}</article>
<p class="meta">Created ${formatDate(lecture.created_at)} · Updated ${formatDate(lecture.updated_at)}</p>`,
    });
}

export function notFound(what) {
    return layout({
        title: 'Not found',
        body: `<div class="empty"><h1>${e(what)} not found</h1><p><a class="btn ghost" href="/admin">Back to courses</a></p></div>`,
    });
}
