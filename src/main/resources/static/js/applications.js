/**
 * JobConnect — job details page: apply flow, share link and similar jobs.
 */
(function () {
    'use strict';

    const $ = id => document.getElementById(id);
    const ALLOWED = ['.pdf', '.doc', '.docx'];
    const MAX_BYTES = 10 * 1024 * 1024;

    function validateFile(file) {
        const name = file.name.toLowerCase();
        if (!ALLOWED.some(ext => name.endsWith(ext))) return 'Only PDF, DOC or DOCX files are accepted.';
        if (file.size > MAX_BYTES) return 'The file is larger than 10 MB.';
        return null;
    }

    function initShare() {
        const btn = $('btn-share');
        if (!btn) return;
        btn.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(window.location.href);
                JC.toast('Link copied to clipboard.', 'success');
            } catch (e) {
                JC.toast('Copy failed — you can copy the URL from the address bar.', 'info');
            }
        });
    }

    async function loadSimilar() {
        const ctx = window.JOB_CONTEXT;
        const wrap = $('similar-jobs-wrap');
        if (!ctx || !wrap) return;
        try {
            const res = await JC.api('/api/jobs?size=4&category=' + encodeURIComponent(ctx.category));
            const jobs = (res.data.content || []).filter(j => j.id !== ctx.id).slice(0, 3);
            if (!jobs.length) return;
            $('similar-jobs').innerHTML = jobs.map(j => JC.jobCard(j)).join('');
            wrap.classList.remove('d-none');
        } catch (e) { /* non-critical */ }
    }

    function initApplyForm() {
        const form = $('apply-job-form');
        if (!form) return;

        const fileInput = $('apply-new-resume-file');
        const dropzone = $('apply-dropzone');
        const fileName = $('apply-file-name');
        const cover = $('apply-cover-letter');
        const errorBox = $('apply-error');
        const submitBtn = $('btn-submit-application');

        const showError = msg => {
            errorBox.innerHTML = '<i class="bi bi-exclamation-octagon"></i><div>' + JC.esc(msg) + '</div>';
            errorBox.classList.remove('d-none');
        };

        const pickFile = file => {
            const problem = validateFile(file);
            if (problem) { showError(problem); fileInput.value = ''; return; }
            errorBox.classList.add('d-none');
            fileName.textContent = file.name + ' · ' + JC.fileSize(file.size);
            fileName.classList.remove('d-none');
            document.querySelectorAll('input[name="resumeId"]').forEach(r => { r.checked = false; });
        };

        dropzone.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', () => { if (fileInput.files[0]) pickFile(fileInput.files[0]); });
        ['dragenter', 'dragover'].forEach(evt => dropzone.addEventListener(evt, e => { e.preventDefault(); dropzone.classList.add('drag'); }));
        ['dragleave', 'drop'].forEach(evt => dropzone.addEventListener(evt, e => { e.preventDefault(); dropzone.classList.remove('drag'); }));
        dropzone.addEventListener('drop', e => {
            const file = e.dataTransfer.files[0];
            if (!file) return;
            const dt = new DataTransfer();
            dt.items.add(file);
            fileInput.files = dt.files;
            pickFile(file);
        });
        document.querySelectorAll('input[name="resumeId"]').forEach(r => r.addEventListener('change', () => {
            fileInput.value = '';
            fileName.classList.add('d-none');
        }));

        cover.addEventListener('input', () => { $('cover-count').textContent = cover.value.length; });

        form.addEventListener('submit', async e => {
            e.preventDefault();
            errorBox.classList.add('d-none');

            const selected = document.querySelector('input[name="resumeId"]:checked');
            let resumeId = selected ? selected.value : null;
            const newFile = fileInput.files[0];

            if (!resumeId && !newFile) {
                showError('Please choose a resume or upload a new one.');
                return;
            }

            try {
                if (newFile) {
                    JC.setLoading(submitBtn, true, 'Uploading resume…');
                    const data = new FormData();
                    data.append('file', newFile);
                    const up = await JC.api('/api/resumes/upload', { method: 'POST', body: data });
                    resumeId = up.data.id;
                }
                JC.setLoading(submitBtn, true, 'Submitting…');
                await JC.api('/api/applications', {
                    method: 'POST',
                    body: { jobId: parseInt($('apply-job-id').value, 10), resumeId: parseInt(resumeId, 10), coverLetter: cover.value.trim() }
                });

                bootstrap.Modal.getInstance($('applyJobModal')).hide();
                JC.toast('Application submitted! You can track its progress from your dashboard.', 'success');
                const main = $('main-apply-btn');
                if (main) {
                    main.outerHTML = '<button type="button" class="btn btn-success-soft btn-lg w-100" disabled><i class="bi bi-check2-circle me-2"></i>Application submitted</button>' +
                        '<a href="/seeker/applied-jobs" class="btn btn-link w-100 mt-1">Track application status</a>';
                }
            } catch (err) {
                showError(err.message);
                JC.setLoading(submitBtn, false);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        initShare();
        initApplyForm();
        loadSimilar();
    });
})();
