/**
 * JobConnect - Global Application JavaScript
 */

document.addEventListener('DOMContentLoaded', () => {
    initActiveNav();
});

// Toast notification system
function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;

    let icon = 'bi-check-circle-fill';
    if (type === 'error') icon = 'bi-exclamation-triangle-fill';
    if (type === 'info') icon = 'bi-info-circle-fill';

    toast.innerHTML = `
        <i class="bi ${icon} fs-5"></i>
        <div class="flex-grow-1">${message}</div>
        <button type="button" class="btn-close btn-close-white" style="font-size: 0.75rem;" onclick="this.parentElement.remove()"></button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 400);
    }, 4000);
}

// Bookmark / Save Job Handler
async function toggleSaveJob(jobId, btnElement) {
    try {
        const response = await fetch(`/api/saved-jobs/${jobId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = '/login?error=Please log in as a Job Seeker to bookmark jobs';
            return;
        }

        const res = await response.json();
        if (res.success) {
            const isSaved = res.data;
            if (isSaved) {
                btnElement.classList.add('saved');
                btnElement.innerHTML = '<i class="bi bi-bookmark-fill text-danger"></i>';
                showToast('Job saved to your bookmarks!', 'success');
            } else {
                btnElement.classList.remove('saved');
                btnElement.innerHTML = '<i class="bi bi-bookmark"></i>';
                showToast('Job removed from bookmarks.', 'info');
            }
        } else {
            showToast(res.message || 'Failed to update saved job', 'error');
        }
    } catch (err) {
        console.error('Error toggling save job:', err);
        showToast('Network error while saving job', 'error');
    }
}

// Active Nav highlight
function initActiveNav() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.navbar-nav .nav-link, .dashboard-sidebar .dash-nav-item').forEach(link => {
        const href = link.getAttribute('href');
        if (href && (href === currentPath || (href !== '/' && currentPath.startsWith(href)))) {
            link.classList.add('active');
        }
    });
}

// Format salary display helper
function formatSalary(min, max) {
    if (!min && !max) return 'Negotiable';
    if (min && !max) return `$${Number(min).toLocaleString()}+`;
    if (!min && max) return `Up to $${Number(max).toLocaleString()}`;
    return `$${Number(min).toLocaleString()} - $${Number(max).toLocaleString()}`;
}

// Fill demo credentials on login page
function fillLogin(email, password) {
    const emailInput = document.getElementById('email');
    const passInput = document.getElementById('password');
    if (emailInput && passInput) {
        emailInput.value = email;
        passInput.value = password;
        showToast(`Filled credentials for ${email}`, 'info');
    }
}
