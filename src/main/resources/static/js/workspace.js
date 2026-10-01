/**
 * JobConnect — recruiter & admin page controllers (requires app.js and dashboards.js).
 */
(function () {
    'use strict';
    const $ = id => document.getElementById(id);
    const E = JC.esc;
    const v = id => ($(id) ? $(id).value.trim() : '');
    const row = (cols, html) => '<tr><td colspan="' + cols + '">' + html + '</td></tr>';

    function table(bodyId, pagId, cols, urlFn, rowFn, empty) {
        const load = async p => {
            const b = $(bodyId);
            b.innerHTML = row(cols, '<div class="text-center py-4 text-muted">Loading…</div>');
            try {
                const r = await JC.api(urlFn(p));
                const list = Array.isArray(r.data) ? r.data : r.data.content;
                b.innerHTML = list.length ? list.map(rowFn).join('') : row(cols, empty);
                if (!Array.isArray(r.data) && pagId) JC.pagination($(pagId), r.data, load);
            } catch (e) { b.innerHTML = row(cols, '<div class="alert-x error m-3">' + E(e.message) + '</div>'); }
        };
        return load;
    }

    function profileForm(P, fields) {
        const sk = $('skills') ? JC.skillEditor($('skills'), P.skills || []) : null;
        $('profile-form').addEventListener('submit', e => {
            e.preventDefault();
            const body = {};
            fields.forEach(f => { body[f] = v(f); });
            if ('totalExperienceYears' in body) body.totalExperienceYears = body.totalExperienceYears ? parseInt(body.totalExperienceYears, 10) : null;
            if (sk) body.skills = sk.get();
            Dash.act('/api/profile', 'PUT', body, 'Profile saved.', false);
        });
    }

    function recruiterDashboard() {
        table('latest-applicants', null, 1, () => '/api/recruiter/applications?size=6', a =>
            '<tr><td><a class="d-flex align-items-center gap-3 text-decoration-none" href="/recruiter/applicants/' + a.id + '"><span class="avatar avatar-sm">' + JC.initials(a.seekerName) + '</span>' +
            '<span class="flex-grow-1 min-w-0"><span class="d-block fw-semibold text-ink small">' + E(a.seekerName) + '</span><span class="d-block small text-muted text-truncate">' + E(a.jobTitle) + ' · ' + JC.timeAgo(a.appliedAt) + '</span></span>' + JC.statusPill(a.status) + '</a></td></tr>',
            JC.emptyState('bi-people', 'No applicants yet', 'Candidates will appear here as soon as they apply.'))(0);
        table('my-jobs', null, 1, () => '/api/recruiter/jobs?size=6', j =>
            '<tr><td><div class="d-flex align-items-center gap-2"><div class="flex-grow-1 min-w-0"><a href="/jobs/' + j.id + '" class="fw-semibold text-ink small d-block text-truncate">' + E(j.title) + '</a>' +
            '<div class="small text-muted">' + j.applicationCount + ' applicants · ' + j.viewsCount + ' views</div></div>' + JC.jobStatusPill(j.status) + '</div></td></tr>',
            JC.emptyState('bi-briefcase', 'No jobs yet', 'Post your first job to start receiving applications.', '<a href="/recruiter/post-job" class="btn btn-primary">Post a job</a>'))(0);
    }

    function recruiterJobs() {
        const load = table('jobs-body', 'jobs-pag', 6, p => '/api/recruiter/jobs?size=10&page=' + p, j =>
            '<tr><td><a href="/jobs/' + j.id + '" class="cell-main">' + E(j.title) + '</a><div class="cell-sub">' + E(j.location) + ' · ' + E(j.employmentType) + ' · posted ' + E(j.postedTimeAgo) + '</div></td>' +
            '<td>' + JC.jobStatusPill(j.status) + '</td><td><a href="/recruiter/applicants?job=' + j.id + '">' + j.applicationCount + '</a></td><td>' + j.viewsCount + '</td><td>' + JC.formatDate(j.deadline) + '</td>' +
            '<td class="text-end text-nowrap"><a class="btn btn-ghost btn-sm" href="/recruiter/edit-job/' + j.id + '"><i class="bi bi-pencil"></i> Edit</a>' +
            (j.status === 'APPROVED' ? '<button class="btn btn-ghost btn-sm" data-status="CLOSED" data-id="' + j.id + '"><i class="bi bi-lock"></i> Close</button>' : '') +
            (j.status === 'CLOSED' ? '<button class="btn btn-ghost btn-sm" data-status="APPROVED" data-id="' + j.id + '"><i class="bi bi-unlock"></i> Reopen</button>' : '') +
            '<button class="btn btn-ghost btn-sm text-danger" data-del="' + j.id + '" aria-label="Delete"><i class="bi bi-trash"></i></button></td></tr>',
            JC.emptyState('bi-briefcase', 'No job posts yet', 'Create your first listing to start hiring.', '<a href="/recruiter/post-job" class="btn btn-primary">Post a job</a>'));
        $('jobs-body').addEventListener('click', async e => {
            const s = e.target.closest('[data-status]'), d = e.target.closest('[data-del]');
            if (s) Dash.act('/api/recruiter/jobs/' + s.dataset.id + '/status', 'PUT', { status: s.dataset.status });
            if (d && await JC.confirmDialog({ title: 'Delete this job?', message: 'The post and all of its applications will be permanently removed.', confirmText: 'Delete', danger: true }))
                Dash.act('/api/jobs/' + d.dataset.del, 'DELETE');
        });
        load(0);
    }

    async function recruiterApplicants() {
        const sel = $('job-filter');
        const load = table('app-body', 'app-pag', 5,
            p => (sel.value ? '/api/recruiter/jobs/' + sel.value + '/applications' : '/api/recruiter/applications') + '?size=15&page=' + p,
            a => '<tr><td><div class="d-flex align-items-center gap-2"><span class="avatar avatar-sm">' + JC.initials(a.seekerName) + '</span><div><div class="cell-main">' + E(a.seekerName) + '</div><div class="cell-sub">' + E(a.seekerHeadline || a.seekerEmail) + '</div></div></div></td>' +
                '<td>' + E(a.jobTitle) + '</td><td>' + JC.formatDate(a.appliedAt) + '</td><td>' + JC.statusPill(a.status) + '</td><td class="text-end"><a class="btn btn-soft btn-sm" href="/recruiter/applicants/' + a.id + '">Review</a></td></tr>',
            JC.emptyState('bi-people', 'No applicants yet', 'Candidates will appear here when they apply.'));
        try {
            const r = await JC.api('/api/recruiter/jobs?size=50');
            r.data.content.forEach(j => sel.insertAdjacentHTML('beforeend', '<option value="' + j.id + '">' + E(j.title) + '</option>'));
        } catch (e) { /* filter is optional */ }
        const q = new URLSearchParams(location.search).get('job');
        if (q) sel.value = q;
        sel.addEventListener('change', () => load(0));
        load(0);
    }

    function applicantDetail(id, status) {
        $('status').value = status;
        $('track').innerHTML = Dash.track(status);
        $('status-form').addEventListener('submit', e => {
            e.preventDefault();
            Dash.act('/api/recruiter/applications/' + id + '/status', 'PUT', { status: v('status'), recruiterNotes: v('notes') }, 'Candidate status updated.');
        });
    }

    function companyForm(size) {
        $('companySize').value = size || '';
        $('company-form').addEventListener('submit', e => {
            e.preventDefault();
            Dash.act('/api/recruiter/company', 'PUT', {
                name: v('name'), industry: v('industry'), website: v('website'), logoUrl: v('logoUrl'), location: v('location'),
                companySize: v('companySize'), foundedYear: v('foundedYear') ? parseInt(v('foundedYear'), 10) : null, description: v('description')
            }, 'Company profile saved.', false);
        });
    }

    function jobForm(job) {
        const sk = JC.skillEditor($('skills'), job ? job.skills : []);
        const map = { 'Full Time': 'FULL_TIME', 'Part Time': 'PART_TIME', 'Contract': 'CONTRACT', 'Internship': 'INTERNSHIP', 'Remote': 'REMOTE', 'Hybrid': 'HYBRID', 'On-site': 'ONSITE',
            'Fresher (0-1 yrs)': 'FRESHER', 'Junior (1-3 yrs)': 'JUNIOR', 'Mid Level (3-5 yrs)': 'MID_LEVEL', 'Experienced (5-8 yrs)': 'EXPERIENCED', 'Senior (8+ yrs)': 'SENIOR' };
        if (job) {
            ['title', 'jobCategory', 'location', 'description', 'responsibilities', 'requirements', 'benefits', 'minExperienceYears', 'maxExperienceYears', 'minSalary', 'maxSalary', 'deadline']
                .forEach(f => { if ($(f) && job[f] != null) $(f).value = job[f]; });
            ['employmentType', 'workplaceType', 'experienceLevel'].forEach(f => { $(f).value = map[job[f]] || ''; });
            $('isFeatured').checked = !!job.featured;
        }
        $('job-form').addEventListener('submit', async e => {
            e.preventDefault();
            const num = f => (v(f) ? Number(v(f)) : null);
            const body = {
                title: v('title'), jobCategory: v('jobCategory'), employmentType: v('employmentType'), workplaceType: v('workplaceType'),
                experienceLevel: v('experienceLevel'), minExperienceYears: num('minExperienceYears') || 0, maxExperienceYears: num('maxExperienceYears'),
                minSalary: num('minSalary'), maxSalary: num('maxSalary'), location: v('location'), description: v('description'),
                responsibilities: v('responsibilities'), requirements: v('requirements'), benefits: v('benefits'),
                deadline: v('deadline') || null, skills: sk.get(), featured: $('isFeatured').checked
            };
            if (body.minSalary && body.maxSalary && body.maxSalary < body.minSalary) return JC.toast('Maximum salary must be greater than the minimum.', 'error');
            const btn = $('btn-save-job');
            JC.setLoading(btn, true, 'Saving…');
            try {
                const r = await JC.api(job ? '/api/jobs/' + job.id : '/api/jobs', { method: job ? 'PUT' : 'POST', body: body });
                JC.toast(job ? 'Job updated.' : (r.data.status === 'PENDING' ? 'Job submitted for approval.' : 'Job published!'));
                setTimeout(() => { location.href = '/recruiter/manage-jobs'; }, 700);
            } catch (err) { JC.setLoading(btn, false); JC.toast(err.message, 'error'); }
        });
    }

    function adminUsers() {
        let users = [];
        const label = { ROLE_JOB_SEEKER: 'Candidate', ROLE_RECRUITER: 'Recruiter', ROLE_ADMIN: 'Admin' };
        const render = () => {
            const q = v('user-search').toLowerCase(), role = v('role-filter');
            const list = users.filter(u => (!role || u.role === role) && (!q || (u.fullName + ' ' + u.email).toLowerCase().includes(q)));
            $('users-body').innerHTML = list.length ? list.map(u =>
                '<tr><td><div class="d-flex align-items-center gap-2"><span class="avatar avatar-sm">' + JC.initials(u.fullName) + '</span><div><div class="cell-main">' + E(u.fullName) + '</div><div class="cell-sub">' + E(u.email) + '</div></div></div></td>' +
                '<td><span class="tag">' + label[u.role] + '</span></td><td>' + E(u.companyName || u.headline || '—') + '</td><td>' + JC.formatDate(u.createdAt) + '</td>' +
                '<td>' + (u.enabled ? '<span class="pill pill-enabled">Active</span>' : '<span class="pill pill-disabled">Disabled</span>') + '</td>' +
                '<td class="text-end">' + (u.role === 'ROLE_ADMIN' ? '' : '<button class="btn btn-sm ' + (u.enabled ? 'btn-danger-soft' : 'btn-success-soft') + '" data-toggle-user="' + u.userId + '">' + (u.enabled ? 'Disable' : 'Enable') + '</button>') + '</td></tr>').join('')
                : row(6, JC.emptyState('bi-people', 'No users found', 'Try a different search.'));
            $('user-count').textContent = list.length + ' of ' + users.length + ' users';
        };
        $('users-body').addEventListener('click', async e => {
            const b = e.target.closest('[data-toggle-user]');
            if (b && await JC.confirmDialog({ title: b.textContent + ' this account?', message: 'Disabled users cannot sign in until re-enabled.', confirmText: b.textContent, danger: b.textContent === 'Disable' }))
                Dash.act('/api/admin/users/' + b.dataset.toggleUser + '/toggle-status', 'PUT');
        });
        ['user-search', 'role-filter'].forEach(id => $(id).addEventListener('input', render));
        JC.api('/api/admin/users').then(r => { users = r.data; render(); }).catch(e => { $('users-body').innerHTML = row(6, E(e.message)); });
    }

    function adminJobs() {
        let status = new URLSearchParams(location.search).get('status') || '';
        const load = table('jobs-body', 'jobs-pag', 6, p => '/api/admin/jobs?size=12&page=' + p + (status ? '&status=' + status : ''), j =>
            '<tr><td><div class="d-flex align-items-center gap-2">' + JC.logoHtml(j.companyName, j.companyLogoUrl, 'sm') + '<div><a href="/jobs/' + j.id + '" class="cell-main">' + E(j.title) + '</a>' + (j.featured ? ' <i class="bi bi-star-fill text-warning small"></i>' : '') +
            '<div class="cell-sub">' + E(j.companyName) + ' · ' + E(j.recruiterName || '') + '</div></div></div></td><td>' + E(j.jobCategory) + '</td><td>' + JC.jobStatusPill(j.status) + '</td><td>' + j.applicationCount + '</td><td>' + E(j.postedTimeAgo) + '</td>' +
            '<td class="text-end text-nowrap">' + (j.status !== 'APPROVED' ? '<button class="btn btn-success-soft btn-sm" data-a="approve" data-id="' + j.id + '">Approve</button> ' : '') +
            (j.status === 'PENDING' || j.status === 'APPROVED' ? '<button class="btn btn-danger-soft btn-sm" data-a="reject" data-id="' + j.id + '">Reject</button> ' : '') +
            '<button class="btn btn-ghost btn-sm" data-a="feature" data-id="' + j.id + '" title="Toggle featured"><i class="bi ' + (j.featured ? 'bi-star-fill text-warning' : 'bi-star') + '"></i></button>' +
            '<button class="btn btn-ghost btn-sm text-danger" data-a="delete" data-id="' + j.id + '" title="Delete"><i class="bi bi-trash"></i></button></td></tr>',
            JC.emptyState('bi-briefcase', 'No jobs here', 'Nothing matches this filter.'));
        document.querySelectorAll('#job-tabs button').forEach(b => {
            b.classList.toggle('active', b.dataset.status === status);
            b.addEventListener('click', () => {
                status = b.dataset.status;
                document.querySelectorAll('#job-tabs button').forEach(x => x.classList.toggle('active', x === b));
                load(0);
            });
        });
        $('jobs-body').addEventListener('click', async e => {
            const b = e.target.closest('[data-a]');
            if (!b) return;
            const id = b.dataset.id, a = b.dataset.a;
            if (a === 'delete') {
                if (await JC.confirmDialog({ title: 'Remove this job?', message: 'It will be permanently deleted with all applications.', confirmText: 'Delete', danger: true })) Dash.act('/api/admin/jobs/' + id, 'DELETE');
            } else Dash.act('/api/admin/jobs/' + id + '/' + a, 'PUT');
        });
        load(0);
    }

    function adminApplications() {
        table('apps-body', 'apps-pag', 5, p => '/api/admin/applications?size=15&page=' + p, a =>
            '<tr><td><div class="cell-main">' + E(a.seekerName) + '</div><div class="cell-sub">' + E(a.seekerEmail) + '</div></td><td><a href="/jobs/' + a.jobId + '">' + E(a.jobTitle) + '</a></td>' +
            '<td>' + E(a.companyName) + '</td><td>' + JC.formatDate(a.appliedAt) + '</td><td>' + JC.statusPill(a.status) + '</td></tr>',
            JC.emptyState('bi-card-checklist', 'No applications yet', 'Applications across the platform will appear here.'))(0);
    }

    function adminMessages() {
        const load = table('msg-body', null, 1, () => '/api/admin/messages', m =>
            '<tr><td><div class="d-flex gap-3"><span class="avatar avatar-sm">' + JC.initials(m.name) + '</span><div class="flex-grow-1 min-w-0">' +
            '<div class="d-flex justify-content-between flex-wrap gap-2"><div><span class="cell-main">' + E(m.subject) + '</span> ' + (m.read ? '' : '<span class="pill pill-applied">New</span>') +
            '<div class="cell-sub">' + E(m.name) + ' · ' + E(m.email) + ' · ' + JC.formatDate(m.createdAt, true) + '</div></div>' +
            '<div class="text-nowrap"><a class="btn btn-soft btn-sm" href="mailto:' + E(m.email) + '">Reply</a> ' +
            (m.read ? '' : '<button class="btn btn-ghost btn-sm" data-read="' + m.id + '">Mark read</button>') +
            '<button class="btn btn-ghost btn-sm text-danger" data-del="' + m.id + '" aria-label="Delete"><i class="bi bi-trash"></i></button></div></div>' +
            '<p class="mt-2 mb-0 small" style="white-space:pre-wrap">' + E(m.message) + '</p></div></div></td></tr>',
            JC.emptyState('bi-inbox', 'Inbox zero', 'Messages sent from the contact page will appear here.'));
        $('msg-body').addEventListener('click', async e => {
            const r = e.target.closest('[data-read]'), d = e.target.closest('[data-del]');
            if (r) Dash.act('/api/admin/messages/' + r.dataset.read + '/read', 'PUT');
            if (d && await JC.confirmDialog({ title: 'Delete message?', confirmText: 'Delete', danger: true })) Dash.act('/api/admin/messages/' + d.dataset.del, 'DELETE');
        });
        load(0);
    }

    function adminCharts(stats) {
        if (typeof Chart === 'undefined') return;
        Chart.defaults.font.family = 'Inter, sans-serif';
        Chart.defaults.color = '#6b7280';
        const order = ['APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW', 'SELECTED', 'REJECTED'];
        const st = stats.applicationsByStatus || {};
        new Chart($('chart-status'), {
            type: 'doughnut',
            data: { labels: order.map(s => JC.STATUS_LABELS[s]), datasets: [{ data: order.map(s => st[s] || 0), backgroundColor: ['#1e4fd6', '#0e7490', '#b7791f', '#6d4ed8', '#0e9f6e', '#d33a3a'], borderWidth: 2, borderColor: '#fff' }] },
            options: { maintainAspectRatio: false, cutout: '68%', plugins: { legend: { position: 'right', labels: { boxWidth: 10, boxHeight: 10 } } } }
        });
        const cat = Object.entries(stats.jobsByCategory || {}).sort((a, b) => b[1] - a[1]);
        new Chart($('chart-category'), {
            type: 'bar',
            data: { labels: cat.map(c => c[0]), datasets: [{ label: 'Active jobs', data: cat.map(c => c[1]), backgroundColor: '#1e4fd6', borderRadius: 6, maxBarThickness: 22 }] },
            options: { indexAxis: 'y', maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#eef0f5' } }, y: { grid: { display: false } } } }
        });
    }

    Object.assign(window.Dash, { profileForm, recruiterDashboard, recruiterJobs, recruiterApplicants, applicantDetail, companyForm, jobForm, adminUsers, adminJobs, adminApplications, adminMessages, adminCharts });
})();
