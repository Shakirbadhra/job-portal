/**
 * JobConnect - Job Search & Catalog Filter Engine
 */

let currentPage = 0;
const pageSize = 9;

document.addEventListener('DOMContentLoaded', () => {
    initSearchEngine();
});

function initSearchEngine() {
    // Populate form from URL if present
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('keyword')) document.getElementById('search-keyword').value = urlParams.get('keyword');
    if (urlParams.has('location')) document.getElementById('search-location').value = urlParams.get('location');
    if (urlParams.has('category')) document.getElementById('filter-category').value = urlParams.get('category');
    if (urlParams.has('employmentType')) document.getElementById('filter-employment').value = urlParams.get('employmentType');
    if (urlParams.has('workplaceType')) document.getElementById('filter-workplace').value = urlParams.get('workplaceType');
    if (urlParams.has('experienceLevel')) document.getElementById('filter-experience').value = urlParams.get('experienceLevel');
    if (urlParams.has('sort')) document.getElementById('sort-by').value = urlParams.get('sort');

    loadJobs(0);

    // Event listeners
    const searchForm = document.getElementById('job-search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            loadJobs(0);
        });
    }

    const sortSelect = document.getElementById('sort-by');
    if (sortSelect) {
        sortSelect.addEventListener('change', () => loadJobs(0));
    }

    // Filter change listeners
    ['filter-category', 'filter-employment', 'filter-workplace', 'filter-experience'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('change', () => loadJobs(0));
    });

    const resetBtn = document.getElementById('btn-reset-filters');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            document.getElementById('search-keyword').value = '';
            document.getElementById('search-location').value = '';
            document.getElementById('filter-category').value = '';
            document.getElementById('filter-employment').value = '';
            document.getElementById('filter-workplace').value = '';
            document.getElementById('filter-experience').value = '';
            document.getElementById('filter-min-salary').value = '';
            document.getElementById('filter-max-salary').value = '';
            document.getElementById('sort-by').value = 'createdAt,desc';
            loadJobs(0);
        });
    }
}

async function loadJobs(page = 0) {
    currentPage = page;
    const container = document.getElementById('jobs-results-container');
    const countEl = document.getElementById('results-count');
    const paginationEl = document.getElementById('jobs-pagination');

    if (!container) return;

    container.innerHTML = `
        <div class="col-12 text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading jobs...</span>
            </div>
            <p class="text-muted mt-2">Finding matching opportunities...</p>
        </div>
    `;

    const keyword = document.getElementById('search-keyword')?.value || '';
    const location = document.getElementById('search-location')?.value || '';
    const category = document.getElementById('filter-category')?.value || '';
    const employmentType = document.getElementById('filter-employment')?.value || '';
    const workplaceType = document.getElementById('filter-workplace')?.value || '';
    const experienceLevel = document.getElementById('filter-experience')?.value || '';
    const minSalary = document.getElementById('filter-min-salary')?.value || '';
    const maxSalary = document.getElementById('filter-max-salary')?.value || '';
    const sort = document.getElementById('sort-by')?.value || 'createdAt,desc';

    const params = new URLSearchParams({
        page: page,
        size: pageSize,
        sort: sort
    });

    if (keyword.trim()) params.append('keyword', keyword.trim());
    if (location.trim()) params.append('location', location.trim());
    if (category.trim()) params.append('category', category.trim());
    if (employmentType.trim()) params.append('employmentType', employmentType.trim());
    if (workplaceType.trim()) params.append('workplaceType', workplaceType.trim());
    if (experienceLevel.trim()) params.append('experienceLevel', experienceLevel.trim());
    if (minSalary.trim()) params.append('minSalary', minSalary.trim());
    if (maxSalary.trim()) params.append('maxSalary', maxSalary.trim());

    try {
        const response = await fetch(`/api/jobs?${params.toString()}`);
        const res = await response.json();

        if (res.success && res.data) {
            const pageData = res.data;
            const jobs = pageData.content || [];
            const totalElements = pageData.totalElements;

            if (countEl) {
                countEl.innerHTML = `Showing <strong>${jobs.length}</strong> of <strong>${totalElements}</strong> jobs`;
            }

            if (jobs.length === 0) {
                container.innerHTML = `
                    <div class="col-12 text-center py-5">
                        <i class="bi bi-search text-muted" style="font-size: 3rem;"></i>
                        <h4 class="mt-3 fw-bold">No jobs matched your criteria</h4>
                        <p class="text-muted">Try adjusting your filters, location, or keywords.</p>
                        <button class="btn btn-outline-custom mt-2" onclick="document.getElementById('btn-reset-filters').click()">
                            Reset All Filters
                        </button>
                    </div>
                `;
                if (paginationEl) paginationEl.innerHTML = '';
                return;
            }

            container.innerHTML = jobs.map(job => renderJobCard(job)).join('');
            renderPagination(pageData, paginationEl);
        } else {
            container.innerHTML = `<div class="col-12 alert alert-danger">${res.message || 'Error loading jobs'}</div>`;
        }
    } catch (err) {
        console.error('Error fetching jobs:', err);
        container.innerHTML = `<div class="col-12 alert alert-danger">Network error loading jobs. Please try again.</div>`;
    }
}

function renderJobCard(job) {
    const skillsBadges = (job.skills || []).slice(0, 4).map(skill =>
        `<span class="badge-tag me-1 mb-1">${escapeHtml(skill)}</span>`
    ).join('');

    const salaryFormatted = formatSalary(job.minSalary, job.maxSalary);
    const savedClass = job.saved ? 'saved' : '';
    const bookmarkIcon = job.saved ? 'bi-bookmark-fill text-danger' : 'bi-bookmark';

    const companyLogo = job.companyLogoUrl ?
        `<img src="${escapeHtml(job.companyLogoUrl)}" alt="${escapeHtml(job.companyName)}" onerror="this.onerror=null; this.parentElement.innerHTML='<span class=\\'company-initial\\'>${escapeHtml(job.companyName.charAt(0))}</span>';">` :
        `<span class="company-initial">${escapeHtml(job.companyName ? job.companyName.charAt(0) : 'J')}</span>`;

    const featuredBadge = job.featured ?
        `<span class="badge bg-warning text-dark me-2"><i class="bi bi-star-fill me-1"></i>Featured</span>` : '';

    return `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="job-card ${job.featured ? 'featured-card' : ''}">
                <div class="d-flex align-items-start justify-content-between mb-3">
                    <div class="d-flex align-items-center gap-3">
                        <div class="company-logo-frame">
                            ${companyLogo}
                        </div>
                        <div>
                            <h6 class="mb-0 fw-bold text-muted small">${escapeHtml(job.companyName)}</h6>
                            <small class="text-muted"><i class="bi bi-geo-alt me-1"></i>${escapeHtml(job.location)}</small>
                        </div>
                    </div>
                    <button class="bookmark-btn ${savedClass}" onclick="toggleSaveJob(${job.id}, this)" title="Bookmark job">
                        <i class="bi ${bookmarkIcon}"></i>
                    </button>
                </div>

                <div class="mb-2">
                    ${featuredBadge}
                    <span class="badge-tag badge-primary-soft me-1 mb-1">${escapeHtml(job.employmentType)}</span>
                    <span class="badge-tag badge-info-soft me-1 mb-1">${escapeHtml(job.workplaceType)}</span>
                </div>

                <h5 class="mb-2">
                    <a href="/jobs/${job.id}" class="job-title">${escapeHtml(job.title)}</a>
                </h5>

                <p class="text-primary fw-bold mb-2">
                    <i class="bi bi-cash-stack me-1"></i>${salaryFormatted}
                </p>

                <div class="mb-3 d-flex flex-wrap">
                    ${skillsBadges}
                </div>

                <div class="mt-auto pt-3 border-top d-flex align-items-center justify-content-between">
                    <small class="text-muted"><i class="bi bi-clock me-1"></i>${job.postedTimeAgo}</small>
                    <a href="/jobs/${job.id}" class="btn btn-sm btn-outline-custom">View Details</a>
                </div>
            </div>
        </div>
    `;
}

function renderPagination(pageData, container) {
    if (!container || pageData.totalPages <= 1) {
        if (container) container.innerHTML = '';
        return;
    }

    const total = pageData.totalPages;
    const current = pageData.number;

    let html = '<ul class="pagination justify-content-center">';

    // Previous
    html += `
        <li class="page-item ${current === 0 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadJobs(${current - 1})" aria-label="Previous">
                <span aria-hidden="true">&laquo;</span>
            </a>
        </li>
    `;

    // Pages
    for (let i = 0; i < total; i++) {
        if (i === 0 || i === total - 1 || (i >= current - 2 && i <= current + 2)) {
            html += `
                <li class="page-item ${i === current ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0)" onclick="loadJobs(${i})">${i + 1}</a>
                </li>
            `;
        } else if (i === current - 3 || i === current + 3) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    // Next
    html += `
        <li class="page-item ${current === total - 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadJobs(${current + 1})" aria-label="Next">
                <span aria-hidden="true">&raquo;</span>
            </a>
        </li>
    `;

    html += '</ul>';
    container.innerHTML = html;
}

function escapeHtml(text) {
    if (!text) return '';
    return text.toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
