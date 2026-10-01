/**
 * JobConnect — core front-end helpers shared by every page.
 */
(function () {
    'use strict';

    // ---------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------

    /**
     * Calls a JSON API endpoint and returns the parsed `ApiResponse`.
     * Throws an Error with a human readable message on failure.
     */
    async function api(url, options = {}) {
        const opts = Object.assign({ method: 'GET', headers: {} }, options);
        if (opts.body && !(opts.body instanceof FormData) && typeof opts.body !== 'string') {
            opts.body = JSON.stringify(opts.body);
            opts.headers['Content-Type'] = 'application/json';
        }
        opts.headers['Accept'] = 'application/json';

        let response;
        try {
            response = await fetch(url, opts);
        } catch (e) {
            throw new Error('Network error — please check your connection and try again.');
        }

        if (response.status === 401) {
            window.location.href = '/login?next=' + encodeURIComponent(window.location.pathname + window.location.search);
            throw new Error('Please sign in to continue.');
        }

        let json = null;
        try { json = await response.json(); } catch (e) { /* non-JSON body */ }

        if (!response.ok || (json && json.success === false)) {
            let message = (json && json.message) || 'Something went wrong (HTTP ' + response.status + ').';
            if (json && json.validationErrors) {
                const first = Object.values(json.validationErrors)[0];
                if (first) message = first;
            }
            if (response.status === 403 && !(json && json.message)) {
                message = 'You do not have permission to perform this action.';
            }
            const err = new Error(message);
            err.status = response.status;
            err.data = json;
            throw err;
        }
        return json;
    }

    // ---------------------------------------------------------------
    // Formatting
    // ---------------------------------------------------------------

    function esc(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function money(n) {
        n = Number(n);
        if (!n) return '';
        if (n >= 1000) return '$' + (Math.round(n / 100) / 10).toString().replace(/\.0$/, '') + 'k';
        return '$' + n.toLocaleString();
    }

    function formatSalary(min, max) {
        if (!min && !max) return 'Salary not disclosed';
        if (min && !max) return 'From ' + money(min);
        if (!min && max) return 'Up to ' + money(max);
        return money(min) + ' – ' + money(max);
    }

    function formatDate(value, withTime) {
        if (!value) return '—';
        const d = new Date(value);
        if (isNaN(d)) return esc(value);
        const opts = { month: 'short', day: 'numeric', year: 'numeric' };
        if (withTime) { opts.hour = 'numeric'; opts.minute = '2-digit'; }
        return d.toLocaleDateString(undefined, opts);
    }

    function timeAgo(value) {
        if (!value) return '';
        const d = new Date(value);
        const s = Math.floor((Date.now() - d.getTime()) / 1000);
        if (s < 60) return 'just now';
        const m = Math.floor(s / 60); if (m < 60) return m + 'm ago';
        const h = Math.floor(m / 60); if (h < 24) return h + 'h ago';
        const days = Math.floor(h / 24); if (days < 30) return days + 'd ago';
        const mo = Math.floor(days / 30); if (mo < 12) return mo + 'mo ago';
        return Math.floor(mo / 12) + 'y ago';
    }

    function fileSize(bytes) {
        if (!bytes) return '—';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
        return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    }

    function initials(name) {
        if (!name) return '?';
        const parts = String(name).trim().split(/\s+/);
        return ((parts[0] || '')[0] + ((parts.length > 1 ? parts[parts.length - 1] : '')[0] || '')).toUpperCase();
    }

    // Same colour formula as the Thymeleaf `ui :: logo` fragment, so a company looks identical everywhere.
    function monoClass(name) {
        return 'mono-' + ((name || '').length % 8);
    }

    function logoHtml(name, url, size) {
        size = size || 'md';
        const letter = esc((name || '?').trim().charAt(0).toUpperCase());
        if (url) {
            return '<span class="co-logo sz-' + size + ' ' + monoClass(name) + '" data-initial="' + letter + '">' +
                '<img src="' + esc(url) + '" alt="' + esc(name) + ' logo" loading="lazy" onerror="JC.logoFallback(this)"></span>';
        }
        return '<span class="co-logo sz-' + size + ' ' + monoClass(name) + '" aria-hidden="true">' + letter + '</span>';
    }

    function logoFallback(img) {
        const holder = img.parentElement;
        holder.textContent = holder.getAttribute('data-initial') || '?';
    }

    const STATUS_LABELS = {
        APPLIED: 'Applied', UNDER_REVIEW: 'Under review', SHORTLISTED: 'Shortlisted', INTERVIEW: 'Interview',
        SELECTED: 'Hired', REJECTED: 'Not selected', PENDING: 'Pending review', APPROVED: 'Active', CLOSED: 'Closed'
    };

    function statusPill(status, label) {
        if (!status) return '';
        return '<span class="pill pill-' + esc(status.toLowerCase()) + '">' + esc(label || STATUS_LABELS[status] || status) + '</span>';
    }

    function jobStatusPill(status) {
        if (status === 'REJECTED') return '<span class="pill pill-rejected">Rejected</span>';
        return statusPill(status);
    }

    // ---------------------------------------------------------------
    // Job cards
    // ---------------------------------------------------------------

    function jobCard(job, options) {
        options = options || {};
        const grid = options.grid;
        const skills = (job.skills || []).slice(0, grid ? 3 : 4)
            .map(s => '<span class="tag tag-outline">' + esc(s) + '</span>').join('');
        const extraSkills = (job.skills || []).length - (grid ? 3 : 4);
        const bookmark = options.hideBookmark ? '' :
            '<button type="button" class="bookmark-btn ' + (job.saved ? 'saved' : '') + '" onclick="JC.toggleSave(' + job.id + ', this)" ' +
            'aria-label="' + (job.saved ? 'Remove from saved jobs' : 'Save job') + '" title="' + (job.saved ? 'Saved' : 'Save job') + '">' +
            '<i class="bi ' + (job.saved ? 'bi-bookmark-fill' : 'bi-bookmark') + '"></i></button>';
        const applied = job.hasApplied ? '<span class="pill pill-selected">Applied</span>' : '';
        const featured = job.featured ? '<span class="pill pill-featured"><i class="bi bi-star-fill"></i>Featured</span>' : '';

        const meta =
            '<span class="tag"><i class="bi bi-geo-alt"></i>' + esc(job.location) + '</span>' +
            '<span class="tag"><i class="bi bi-briefcase"></i>' + esc(job.employmentType) + '</span>' +
            '<span class="tag"><i class="bi bi-buildings"></i>' + esc(job.workplaceType) + '</span>';

        if (grid) {
            return '' +
                '<div class="job-card is-grid ' + (job.featured ? 'is-featured' : '') + '">' +
                '  <div class="d-flex align-items-start justify-content-between gap-2 mb-3">' +
                       logoHtml(job.companyName, job.companyLogoUrl, 'md') +
                '      <div class="actions d-flex gap-2 align-items-center">' + featured + bookmark + '</div>' +
                '  </div>' +
                '  <div class="company mb-1">' + esc(job.companyName) + '</div>' +
                '  <h3 class="job-title"><a href="/jobs/' + job.id + '">' + esc(job.title) + '</a></h3>' +
                '  <div class="meta">' + meta + '</div>' +
                '  <div class="d-flex flex-wrap gap-xs mt-3">' + skills + '</div>' +
                '  <div class="job-foot"><span class="salary">' + formatSalary(job.minSalary, job.maxSalary) + '</span>' +
                '  <span class="posted">' + (applied || esc(job.postedTimeAgo || '')) + '</span></div>' +
                '</div>';
        }

        return '' +
            '<div class="job-card ' + (job.featured ? 'is-featured' : '') + '">' +
                logoHtml(job.companyName, job.companyLogoUrl, 'md') +
            '  <div class="flex-grow-1 min-w-0">' +
            '    <div class="d-flex justify-content-between gap-3">' +
            '      <div class="min-w-0">' +
            '        <h3 class="job-title"><a href="/jobs/' + job.id + '">' + esc(job.title) + '</a></h3>' +
            '        <div class="company">' + esc(job.companyName) + ' <span class="text-muted">·</span> <span class="text-muted">' + esc(job.experienceLevel || '') + '</span></div>' +
            '      </div>' +
            '      <div class="actions d-flex gap-2 align-items-start">' + featured + bookmark + '</div>' +
            '    </div>' +
            '    <div class="meta">' + meta + '</div>' +
            '    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3">' +
            '      <div class="d-flex flex-wrap gap-xs">' + skills + (extraSkills > 0 ? '<span class="tag">+' + extraSkills + '</span>' : '') + '</div>' +
            '      <div class="d-flex align-items-center gap-3"><span class="salary">' + formatSalary(job.minSalary, job.maxSalary) + '</span>' +
            '      <span class="posted">' + (applied || esc(job.postedTimeAgo || '')) + '</span></div>' +
            '    </div>' +
            '  </div>' +
            '</div>';
    }

    function skeletonCards(n) {
        let html = '';
        for (let i = 0; i < (n || 4); i++) {
            html += '<div class="sk-card mb-3"><div class="skeleton" style="width:48px;height:48px;border-radius:12px"></div>' +
                '<div class="flex-grow-1"><div class="skeleton mb-2" style="height:16px;width:55%"></div>' +
                '<div class="skeleton mb-3" style="height:12px;width:30%"></div>' +
                '<div class="d-flex gap-2"><div class="skeleton" style="height:22px;width:90px"></div><div class="skeleton" style="height:22px;width:80px"></div><div class="skeleton" style="height:22px;width:70px"></div></div></div></div>';
        }
        return html;
    }

    function emptyState(icon, title, text, actionHtml) {
        return '<div class="empty-state"><div class="ico"><i class="bi ' + icon + '"></i></div>' +
            '<h3>' + esc(title) + '</h3><p>' + esc(text) + '</p>' + (actionHtml || '') + '</div>';
    }

    // ---------------------------------------------------------------
    // Pagination (Spring `Page` objects)
    // ---------------------------------------------------------------

    function pagination(container, page, onPage) {
        if (!container) return;
        if (!page || page.totalPages <= 1) { container.innerHTML = ''; return; }
        const total = page.totalPages;
        const current = page.number;
        const item = (i, label, disabled, active) =>
            '<li class="page-item ' + (disabled ? 'disabled' : '') + (active ? ' active' : '') + '">' +
            '<button type="button" class="page-link" data-page="' + i + '"' + (disabled ? ' tabindex="-1"' : '') + '>' + label + '</button></li>';

        let html = '<nav aria-label="Pagination"><ul class="pagination justify-content-center flex-wrap mb-0">';
        html += item(current - 1, '<i class="bi bi-chevron-left"></i>', current === 0);
        for (let i = 0; i < total; i++) {
            if (i === 0 || i === total - 1 || Math.abs(i - current) <= 1) {
                html += item(i, i + 1, false, i === current);
            } else if (Math.abs(i - current) === 2) {
                html += '<li class="page-item disabled"><span class="page-link">…</span></li>';
            }
        }
        html += item(current + 1, '<i class="bi bi-chevron-right"></i>', current >= total - 1);
        html += '</ul></nav>';
        container.innerHTML = html;
        container.querySelectorAll('[data-page]').forEach(btn => {
            btn.addEventListener('click', () => {
                const p = parseInt(btn.getAttribute('data-page'), 10);
                if (p >= 0 && p < total && p !== current) onPage(p);
            });
        });
    }

    // ---------------------------------------------------------------
    // Feedback: toasts & confirm dialogs
    // ---------------------------------------------------------------

    function toast(message, type) {
        type = type || 'success';
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.setAttribute('aria-live', 'polite');
            document.body.appendChild(container);
        }
        const icons = { success: 'bi-check-circle-fill', error: 'bi-exclamation-octagon-fill', info: 'bi-info-circle-fill' };
        const el = document.createElement('div');
        el.className = 'toast-x ' + type;
        el.setAttribute('role', type === 'error' ? 'alert' : 'status');
        el.innerHTML = '<i class="bi ' + (icons[type] || icons.info) + ' ti"></i><div class="tx">' + esc(message) + '</div>' +
            '<button type="button" aria-label="Dismiss"><i class="bi bi-x-lg"></i></button>';
        const remove = () => { el.classList.add('leaving'); setTimeout(() => el.remove(), 250); };
        el.querySelector('button').addEventListener('click', remove);
        container.appendChild(el);
        setTimeout(remove, type === 'error' ? 6000 : 4000);
    }

    function confirmDialog(opts) {
        opts = Object.assign({ title: 'Are you sure?', message: '', confirmText: 'Confirm', cancelText: 'Cancel', danger: false }, opts);
        return new Promise(resolve => {
            const wrap = document.createElement('div');
            wrap.innerHTML =
                '<div class="modal fade" tabindex="-1" aria-modal="true" role="dialog">' +
                '<div class="modal-dialog modal-dialog-centered" style="max-width:440px"><div class="modal-content">' +
                '<div class="modal-body p-4">' +
                '<div class="d-flex gap-3"><div class="kpi-ico ' + (opts.danger ? 'ico-danger' : 'ico-brand') + '" style="width:44px;height:44px;border-radius:12px;display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;font-size:1.2rem">' +
                '<i class="bi ' + (opts.danger ? 'bi-exclamation-triangle' : 'bi-question-circle') + '"></i></div>' +
                '<div><h5 class="mb-1">' + esc(opts.title) + '</h5><p class="text-muted mb-0 small">' + esc(opts.message) + '</p></div></div>' +
                '</div><div class="modal-footer bg-canvas" style="border-radius:0 0 16px 16px">' +
                '<button type="button" class="btn btn-outline" data-act="cancel">' + esc(opts.cancelText) + '</button>' +
                '<button type="button" class="btn ' + (opts.danger ? 'btn-danger' : 'btn-primary') + '" data-act="ok">' + esc(opts.confirmText) + '</button>' +
                '</div></div></div></div>';
            const modalEl = wrap.firstChild;
            document.body.appendChild(modalEl);
            const modal = new bootstrap.Modal(modalEl);
            let result = false;
            modalEl.querySelector('[data-act="ok"]').addEventListener('click', () => { result = true; modal.hide(); });
            modalEl.querySelector('[data-act="cancel"]').addEventListener('click', () => modal.hide());
            modalEl.addEventListener('hidden.bs.modal', () => { modalEl.remove(); resolve(result); });
            modal.show();
        });
    }

    function setLoading(btn, loading, text) {
        if (!btn) return;
        if (loading) {
            btn.dataset.originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>' + esc(text || 'Please wait…');
        } else {
            btn.disabled = false;
            if (btn.dataset.originalHtml) btn.innerHTML = btn.dataset.originalHtml;
        }
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    async function toggleSave(jobId, btn) {
        if (document.body.dataset.role && document.body.dataset.role !== 'ROLE_JOB_SEEKER') {
            toast('Only candidate accounts can save jobs.', 'info');
            return;
        }
        if (!document.body.dataset.role) {
            window.location.href = '/login?next=' + encodeURIComponent(window.location.pathname + window.location.search);
            return;
        }
        try {
            const res = await api('/api/saved-jobs/' + jobId, { method: 'POST' });
            const saved = !!res.data;
            if (btn) {
                btn.classList.toggle('saved', saved);
                const icon = btn.querySelector('i');
                if (icon) icon.className = 'bi ' + (saved ? 'bi-bookmark-fill' : 'bi-bookmark');
                btn.setAttribute('aria-label', saved ? 'Remove from saved jobs' : 'Save job');
                const label = btn.querySelector('.label');
                if (label) label.textContent = saved ? 'Saved' : 'Save';
            }
            toast(saved ? 'Job saved to your list.' : 'Job removed from your saved list.', saved ? 'success' : 'info');
            document.dispatchEvent(new CustomEvent('jc:saved-changed', { detail: { jobId: jobId, saved: saved } }));
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    // ---------------------------------------------------------------
    // Page chrome
    // ---------------------------------------------------------------

    function initActiveNav() {
        const path = window.location.pathname;
        document.querySelectorAll('.site-nav .nav-link, .app-sidebar .sb-link').forEach(link => {
            const href = link.getAttribute('href');
            if (!href || href === '#') return;
            const exact = link.hasAttribute('data-exact');
            if (href === path || (!exact && href !== '/' && path.startsWith(href + '/')) || (!exact && href !== '/' && path === href)) {
                link.classList.add('active');
                link.setAttribute('aria-current', 'page');
            }
        });
    }

    function initSidebar() {
        const sidebar = document.querySelector('.app-sidebar');
        const backdrop = document.querySelector('.sb-backdrop');
        if (!sidebar) return;
        const close = () => { sidebar.classList.remove('open'); backdrop && backdrop.classList.remove('show'); };
        document.querySelectorAll('[data-toggle-sidebar]').forEach(btn => btn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            backdrop && backdrop.classList.toggle('show');
        }));
        backdrop && backdrop.addEventListener('click', close);
    }

    function initPasswordToggles() {
        document.querySelectorAll('[data-toggle-password]').forEach(btn => {
            btn.addEventListener('click', () => {
                const input = document.getElementById(btn.getAttribute('data-toggle-password'));
                if (!input) return;
                const show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                btn.innerHTML = '<i class="bi ' + (show ? 'bi-eye-slash' : 'bi-eye') + '"></i>';
                btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            });
        });
    }

    function initNewsletter() {
        document.querySelectorAll('form[data-newsletter]').forEach(form => {
            form.addEventListener('submit', e => {
                e.preventDefault();
                form.reset();
                toast('You\'re subscribed — look out for weekly job alerts.', 'success');
            });
        });
    }

    /**
     * Wires a tag-style skill input. `root` is the .skill-editor element; returns {get, set}.
     */
    function skillEditor(root, initial) {
        const input = root.querySelector('input');
        let skills = [];
        const render = () => {
            root.querySelectorAll('.tag').forEach(t => t.remove());
            skills.forEach((s, i) => {
                const tag = document.createElement('span');
                tag.className = 'tag';
                tag.innerHTML = esc(s) + '<button type="button" aria-label="Remove ' + esc(s) + '"><i class="bi bi-x"></i></button>';
                tag.querySelector('button').addEventListener('click', () => { skills.splice(i, 1); render(); });
                root.insertBefore(tag, input);
            });
        };
        const add = value => {
            value.split(',').map(v => v.trim()).filter(Boolean).forEach(v => {
                if (!skills.some(s => s.toLowerCase() === v.toLowerCase()) && skills.length < 25) skills.push(v);
            });
            render();
        };
        input.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ',') {
                e.preventDefault();
                add(input.value);
                input.value = '';
            } else if (e.key === 'Backspace' && !input.value && skills.length) {
                skills.pop();
                render();
            }
        });
        input.addEventListener('blur', () => { if (input.value.trim()) { add(input.value); input.value = ''; } });
        root.addEventListener('click', e => { if (e.target === root) input.focus(); });
        if (initial) add(initial.join(','));
        return { get: () => skills.slice(), set: list => { skills = []; add((list || []).join(',')); } };
    }

    /**
     * Turns plain text (as stored in the DB) into paragraphs and bullet lists.
     * Lines starting with "-", "*" or "•" become list items; blank lines split paragraphs.
     */
    function formatProse(el) {
        const text = el.textContent || '';
        const lines = text.split(/\r?\n/);
        let html = '';
        let list = [];
        let para = [];
        const flushList = () => { if (list.length) { html += '<ul>' + list.map(l => '<li>' + esc(l) + '</li>').join('') + '</ul>'; list = []; } };
        const flushPara = () => { if (para.length) { html += '<p>' + esc(para.join(' ')) + '</p>'; para = []; } };
        lines.forEach(raw => {
            const line = raw.trim();
            const bullet = line.match(/^[-*•]\s+(.*)$/);
            if (bullet) { flushPara(); list.push(bullet[1]); }
            else if (!line) { flushList(); flushPara(); }
            else { flushList(); para.push(line); }
        });
        flushList();
        flushPara();
        el.innerHTML = html;
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-format]').forEach(formatProse);
        initActiveNav();
        initSidebar();
        initPasswordToggles();
        initNewsletter();
    });

    window.JC = {
        api, esc, money, formatSalary, formatDate, timeAgo, fileSize, initials, monoClass, logoHtml, logoFallback,
        statusPill, jobStatusPill, STATUS_LABELS, jobCard, skeletonCards, emptyState, pagination, toast,
        confirmDialog, setLoading, toggleSave, skillEditor
    };
    // Backwards-compatible global used by older markup
    window.showToast = toast;
})();
