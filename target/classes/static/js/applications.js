/**
 * JobConnect - Applications & Resume Manager
 */

document.addEventListener('DOMContentLoaded', () => {
    initApplicationForm();
    initResumeUpload();
});

function initApplicationForm() {
    const applyForm = document.getElementById('apply-job-form');
    if (!applyForm) return;

    applyForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const submitBtn = document.getElementById('btn-submit-application');
        const jobId = document.getElementById('apply-job-id').value;
        const resumeSelect = document.getElementById('apply-resume-select');
        const coverLetter = document.getElementById('apply-cover-letter').value;

        let resumeId = resumeSelect ? resumeSelect.value : null;

        // If user is uploading a new resume directly from modal
        const newResumeFile = document.getElementById('apply-new-resume-file')?.files[0];
        if (newResumeFile) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Uploading resume...';

            const formData = new FormData();
            formData.append('file', newResumeFile);

            try {
                const uploadRes = await fetch('/api/resumes/upload', {
                    method: 'POST',
                    body: formData
                });
                const uploadJson = await uploadRes.json();
                if (uploadJson.success && uploadJson.data) {
                    resumeId = uploadJson.data.id;
                } else {
                    showToast(uploadJson.message || 'Failed to upload resume', 'error');
                    submitBtn.disabled = false;
                    submitBtn.innerText = 'Submit Application';
                    return;
                }
            } catch (err) {
                console.error('Resume upload error:', err);
                showToast('Network error uploading resume', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = 'Submit Application';
                return;
            }
        }

        if (!resumeId) {
            showToast('Please select or upload a resume to apply', 'error');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Submitting...';

        try {
            const response = await fetch('/api/applications', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    jobId: parseInt(jobId),
                    resumeId: parseInt(resumeId),
                    coverLetter: coverLetter
                })
            });

            const res = await response.json();
            if (res.success) {
                showToast('Congratulations! Application submitted successfully.', 'success');

                // Close modal
                const modalEl = document.getElementById('applyJobModal');
                if (modalEl) {
                    const modal = bootstrap.Modal.getInstance(modalEl);
                    if (modal) modal.hide();
                }

                // Update UI apply button on job details page
                const mainApplyBtn = document.getElementById('main-apply-btn');
                if (mainApplyBtn) {
                    mainApplyBtn.disabled = true;
                    mainApplyBtn.className = 'btn btn-success btn-lg px-4';
                    mainApplyBtn.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i>Applied';
                }
            } else {
                showToast(res.message || 'Failed to submit application', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = 'Submit Application';
            }
        } catch (err) {
            console.error('Error submitting application:', err);
            showToast('Network error submitting application', 'error');
            submitBtn.disabled = false;
            submitBtn.innerText = 'Submit Application';
        }
    });
}

function initResumeUpload() {
    const resumeForm = document.getElementById('resume-upload-form');
    if (!resumeForm) return;

    resumeForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fileInput = document.getElementById('resume-file-input');
        const submitBtn = document.getElementById('btn-upload-resume');

        if (!fileInput || !fileInput.files[0]) {
            showToast('Please choose a resume file to upload', 'error');
            return;
        }

        const file = fileInput.files[0];
        const validExtensions = ['.pdf', '.doc', '.docx'];
        const fileName = file.name.toLowerCase();
        const isValid = validExtensions.some(ext => fileName.endsWith(ext));

        if (!isValid) {
            showToast('Only PDF, DOC, or DOCX files are permitted', 'error');
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            showToast('File size must not exceed 10MB', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Uploading...';

        try {
            const response = await fetch('/api/resumes/upload', {
                method: 'POST',
                body: formData
            });

            const res = await response.json();
            if (res.success) {
                showToast('Resume uploaded successfully!', 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                showToast(res.message || 'Upload failed', 'error');
                submitBtn.disabled = false;
                submitBtn.innerText = 'Upload Resume';
            }
        } catch (err) {
            console.error('Upload error:', err);
            showToast('Network error while uploading resume', 'error');
            submitBtn.disabled = false;
            submitBtn.innerText = 'Upload Resume';
        }
    });
}

async function setPrimaryResume(resumeId) {
    try {
        const response = await fetch(`/api/resumes/${resumeId}/primary`, { method: 'PUT' });
        const res = await response.json();
        if (res.success) {
            showToast('Primary resume updated!', 'success');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to update', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}

async function deleteResume(resumeId) {
    if (!confirm('Are you sure you want to delete this resume?')) return;

    try {
        const response = await fetch(`/api/resumes/${resumeId}`, { method: 'DELETE' });
        const res = await response.json();
        if (res.success) {
            showToast('Resume deleted successfully!', 'info');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to delete', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}
