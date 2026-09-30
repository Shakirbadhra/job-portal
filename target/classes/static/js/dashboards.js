/**
 * JobConnect - Dashboards & Admin Operations JavaScript
 */

// Recruiter: Update Candidate Status
async function updateCandidateStatus(applicationId, newStatus, notes = '') {
    try {
        const response = await fetch(`/api/recruiter/applications/${applicationId}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                status: newStatus,
                recruiterNotes: notes
            })
        });

        const res = await response.json();
        if (res.success) {
            showToast(`Application status updated to ${newStatus}`, 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showToast(res.message || 'Failed to update status', 'error');
        }
    } catch (err) {
        console.error('Error updating status:', err);
        showToast('Network error updating status', 'error');
    }
}

// Recruiter: Delete Job Post
async function deleteRecruiterJob(jobId) {
    if (!confirm('Are you sure you want to permanently delete this job posting? This cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch(`/api/jobs/${jobId}`, { method: 'DELETE' });
        const res = await response.json();
        if (res.success) {
            showToast('Job removed successfully', 'success');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to delete job', 'error');
        }
    } catch (err) {
        showToast('Network error deleting job', 'error');
    }
}

// Admin: Toggle User Status
async function toggleUserStatus(userId, btnElement) {
    try {
        const response = await fetch(`/api/admin/users/${userId}/toggle-status`, { method: 'PUT' });
        const res = await response.json();
        if (res.success) {
            showToast(res.message, 'success');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to toggle status', 'error');
        }
    } catch (err) {
        showToast('Network error toggling user', 'error');
    }
}

// Admin: Approve Job
async function approveJob(jobId) {
    try {
        const response = await fetch(`/api/admin/jobs/${jobId}/approve`, { method: 'PUT' });
        const res = await response.json();
        if (res.success) {
            showToast('Job approved successfully!', 'success');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to approve', 'error');
        }
    } catch (err) {
        showToast('Network error approving job', 'error');
    }
}

// Admin: Reject Job
async function rejectJob(jobId) {
    try {
        const response = await fetch(`/api/admin/jobs/${jobId}/reject`, { method: 'PUT' });
        const res = await response.json();
        if (res.success) {
            showToast('Job rejected', 'info');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to reject', 'error');
        }
    } catch (err) {
        showToast('Network error rejecting job', 'error');
    }
}

// Admin: Force Remove Job
async function adminDeleteJob(jobId) {
    if (!confirm('Are you sure you want to remove this job from the platform?')) return;
    try {
        const response = await fetch(`/api/admin/jobs/${jobId}`, { method: 'DELETE' });
        const res = await response.json();
        if (res.success) {
            showToast('Job permanently removed by Admin', 'success');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(res.message || 'Failed to delete job', 'error');
        }
    } catch (err) {
        showToast('Network error deleting job', 'error');
    }
}

// Chart.js initialization for Admin Dashboard
function initAdminCharts(statsData) {
    if (typeof Chart === 'undefined') return;

    // Applications by Status Doughnut Chart
    const statusCtx = document.getElementById('chart-applications-status');
    if (statusCtx && statsData.applicationsByStatus) {
        const labels = Object.keys(statsData.applicationsByStatus);
        const data = Object.values(statsData.applicationsByStatus);

        new Chart(statusCtx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: ['#3b82f6', '#06b6d4', '#f59e0b', '#8b5cf6', '#10b981', '#ef4444'],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    }

    // Jobs by Category Bar Chart
    const catCtx = document.getElementById('chart-jobs-category');
    if (catCtx && statsData.jobsByCategory) {
        const labels = Object.keys(statsData.jobsByCategory);
        const data = Object.values(statsData.jobsByCategory);

        new Chart(catCtx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Open Positions',
                    data: data,
                    backgroundColor: '#4f46e5',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 } }
                }
            }
        });
    }
}
