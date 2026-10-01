/**
 * JobConnect — dashboard pages (candidate, recruiter, admin). Each page calls the init it needs.
 */
(function () {
    'use strict';
    const $ = id => document.getElementById(id);
    const E = JC.esc;
    const err = (box, e) => { box.innerHTML = '<div class="p-4"><div class="alert-x error"><i class="bi bi-exclamation-octagon"></i><div>' + E(e.message) + '</div></div></div>'; };
    const STAGES = ['APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW', 'SELECTED'];

    function track(status) {
        const idx = STAGES.indexOf(status);
        const rejected = status === 'REJECTED';
        return '<div class="status-track ' + (rejected ? 'is-rejected' : '') + '">' + ['Applied', 'Review', 'Shortlist', 'Interview', 'Hired'].map((l, i) =>
            '<div class="st ' + (i <= idx ? 'done' : '') + (i === idx || (rejected && i === 0) ? ' current' : '') + '"><span class="dot"></span>' + (rejected && i === 0 ? 'Closed' : l) + '</div>').join('') + '</div>';
    }

    function appRow(a) {
        return '<div class="list-row">' + JC.logoHtml(a.companyName, a.companyLogoUrl, 'sm') +
            '<div class="flex-grow-1 min-w-0"><a href="/jobs/' + a.jobId + '" class="fw-semibold text-ink d-block text-truncate">' + E(a.jobTitle) + '</a>' +
            '<div class="small text-muted">' + E(a.companyName) + ' · Applied ' + JC.formatDate(a.appliedAt) + '</div></div>' + JC.statusPill(a.status) + '</div>';
    }

    async function loadRecentApplications(box, size) {
        try {
            const res = await JC.api('/api/applications/my?size=' + size);
            box.innerHTML = res.data.content.length ? res.data.content.map(appRow).join('') :
                JC.emptyState('bi-send', 'No applications yet', 'Find a role you like and apply in one click.', '<a href="/jobs" class="btn btn-primary">Browse jobs</a>');
        } catch (e) { err(box, e); }
    }

    function renderProfileStrength(p) {
        const items = [['Headline', p.headline], ['Summary', p.bio], ['Location', p.currentLocation], ['Experience', p.totalExperienceYears > 0],
            ['Education', p.highestQualification], ['Skills', p.skills && p.skills.length], ['Phone', p.phone], ['Resume', p.primaryResumeId]];
        const done = items.filter(i => i[1]).length;
        const pct = Math.round(done / items.length * 100);
        $('profile-pct').textContent = pct + '%';
        $('profile-bar').style.width = pct + '%';
        $('profile-checklist').innerHTML = items.map(i => '<li><i class="bi ' + (i[1] ? 'bi-check-circle-fill text-success' : 'bi-circle text-muted') + ' me-2"></i>' + i[0] + '</li>').join('');
    }

    async function seekerApplications() {
        const box = $('applications-list');
        let filter = '';
        let all = [];
        const render = () => {
            const list = filter ? all.filter(a => a.status === filter) : all;
            box.innerHTML = list.length ? list.map(a =>
                '<div class="card-x card-x-body mb-3"><div class="d-flex gap-3 align-items-start">' + JC.logoHtml(a.companyName, a.companyLogoUrl, 'md') +
                '<div class="flex-grow-1 min-w-0"><div class="d-flex justify-content-between gap-2 flex-wrap"><div><a href="/jobs/' + a.jobId + '" class="h6 text-ink">' + E(a.jobTitle) + '</a>' +
                '<div class="small text-muted">' + E(a.companyName) + ' · ' + E(a.jobLocation) + ' · Applied ' + JC.formatDate(a.appliedAt) + '</div></div>' + JC.statusPill(a.status) + '</div>' +
                track(a.status) + (a.recruiterNotes ? '<div class="alert-x info mt-3 mb-0"><i class="bi bi-chat-left-text"></i><div><strong>Recruiter note:</strong> ' + E(a.recruiterNotes) + '</div></div>' : '') +
                '</div></div></div>').join('') :
                '<div class="card-x">' + JC.emptyState('bi-send', 'Nothing here yet', 'Applications you submit will appear here with live status updates.', '<a href="/jobs" class="btn btn-primary">Browse jobs</a>') + '</div>';
        };
        document.querySelectorAll('#app-tabs button').forEach(b => b.addEventListener('click', () => {
            document.querySelectorAll('#app-tabs button').forEach(x => x.classList.remove('active'));
            b.classList.add('active'); filter = b.dataset.status; render();
        }));
        try { all = (await JC.api('/api/applications/my?size=100')).data.content; render(); } catch (e) { err(box, e); }
    }

    async function seekerSaved() {
        const box = $('saved-list');
        const load = async page => {
            try {
                const res = await JC.api('/api/saved-jobs?size=10&page=' + page);
                box.innerHTML = res.data.content.length ? res.data.content.map(j => JC.jobCard(j)).join('') :
                    '<div class="card-x">' + JC.emptyState('bi-bookmark', 'No saved jobs', 'Tap the bookmark on any job to keep it here for later.', '<a href="/jobs" class="btn btn-primary">Browse jobs</a>') + '</div>';
                JC.pagination($('saved-pagination'), res.data, load);
            } catch (e) { err(box, e); }
        };
        document.addEventListener('jc:saved-changed', () => load(0));
        load(0);
    }

    function seekerResumes() {
        const input = $('resume-file-input');
        const dz = $('resume-dropzone');
        const upload = async file => {
            if (!/\.(pdf|docx?|DOCX?|PDF)$/.test(file.name)) return JC.toast('Only PDF, DOC or DOCX files are accepted.', 'error');
            if (file.size > 10485760) return JC.toast('File is larger than 10 MB.', 'error');
            const fd = new FormData(); fd.append('file', file);
            dz.innerHTML = '<div class="spinner-border text-primary"></div><div class="small mt-2">Uploading…</div>';
            try { await JC.api('/api/resumes/upload', { method: 'POST', body: fd }); JC.toast('Resume uploaded.'); setTimeout(() => location.reload(), 600); }
            catch (e) { JC.toast(e.message, 'error'); setTimeout(() => location.reload(), 1200); }
        };
        dz.addEventListener('click', () => input.click());
        input.addEventListener('change', () => input.files[0] && upload(input.files[0]));
        ['dragenter', 'dragover'].forEach(t => dz.addEventListener(t, e => { e.preventDefault(); dz.classList.add('drag'); }));
        ['dragleave', 'drop'].forEach(t => dz.addEventListener(t, e => { e.preventDefault(); dz.classList.remove('drag'); }));
        dz.addEventListener('drop', e => e.dataTransfer.files[0] && upload(e.dataTransfer.files[0]));
    }

    async function act(url, method, body, okMsg, reload) {
        try { const r = await JC.api(url, { method: method, body: body }); JC.toast(okMsg || r.message); if (reload !== false) setTimeout(() => location.reload(), 600); return r; }
        catch (e) { JC.toast(e.message, 'error'); }
    }

    window.Dash = {
        track, appRow, loadRecentApplications, renderProfileStrength, seekerApplications, seekerSaved, seekerResumes, act,
        setPrimary: id => act('/api/resumes/' + id + '/primary', 'PUT', null, 'Primary resume updated.'),
        deleteResume: async id => { if (await JC.confirmDialog({ title: 'Delete resume?', message: 'This cannot be undone.', confirmText: 'Delete', danger: true })) act('/api/resumes/' + id, 'DELETE', null, 'Resume deleted.'); }
    };
})();
