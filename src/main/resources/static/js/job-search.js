/**
 * JobConnect — job search page: filters, URL state, results and pagination.
 */
(function () {
    'use strict';

    const PAGE_SIZE = 10;
    const $ = id => document.getElementById(id);
    let view = 'list';
    let currentPage = 0;

    const FILTER_LABELS = {
        employmentType: { FULL_TIME: 'Full-time', PART_TIME: 'Part-time', CONTRACT: 'Contract', INTERNSHIP: 'Internship' },
        workplaceType: { REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site' },
        experienceLevel: { FRESHER: 'Entry level', JUNIOR: 'Junior', MID_LEVEL: 'Mid level', EXPERIENCED: 'Experienced', SENIOR: 'Senior' }
    };

    function radioValue(name) {
        const el = document.querySelector('input[name="' + name + '"]:checked');
        return el ? el.value : '';
    }

    function setRadio(name, value) {
        const el = document.querySelector('input[name="' + name + '"][value="' + (value || '') + '"]');
        if (el) el.checked = true;
    }

    function readFilters() {
        return {
            keyword: $('search-keyword').value.trim(),
            location: $('search-location').value.trim(),
            category: $('filter-category').value,
            employmentType: radioValue('employmentType'),
            workplaceType: radioValue('workplaceType'),
            experienceLevel: $('filter-experience').value,
            minSalary: $('filter-min-salary').value,
            companyId: $('filter-company').value,
            sort: $('sort-by').value
        };
    }

    function applyFromUrl() {
        const p = new URLSearchParams(window.location.search);
        const init = window.JOB_SEARCH_INIT || {};
        if (p.has('keyword')) $('search-keyword').value = p.get('keyword');
        if (p.has('location')) $('search-location').value = p.get('location');
        $('filter-category').value = p.get('category') || init.category || '';
        setRadio('employmentType', p.get('employmentType') || init.employmentType || '');
        setRadio('workplaceType', p.get('workplaceType') || init.workplaceType || '');
        $('filter-experience').value = p.get('experienceLevel') || init.experienceLevel || '';
        $('filter-min-salary').value = p.get('minSalary') || '';
        $('filter-company').value = p.get('companyId') || '';
        if (p.get('sort')) $('sort-by').value = p.get('sort');
        if (p.get('page')) currentPage = Math.max(0, parseInt(p.get('page'), 10) || 0);
        try { view = localStorage.getItem('jc-job-view') || 'list'; } catch (e) { view = 'list'; }
        syncViewButtons();
    }

    function syncUrl(filters, page) {
        const p = new URLSearchParams();
        Object.keys(filters).forEach(k => {
            if (filters[k] && !(k === 'sort' && filters[k] === 'createdAt,desc')) p.set(k, filters[k]);
        });
        if (page > 0) p.set('page', page);
        const qs = p.toString();
        history.replaceState(null, '', window.location.pathname + (qs ? '?' + qs : ''));
    }

    function renderActiveFilters(f) {
        const chips = [];
        const add = (key, label) => chips.push('<span class="tag">' + JC.esc(label) +
            '<button type="button" data-clear="' + key + '" aria-label="Remove filter ' + JC.esc(label) + '"><i class="bi bi-x"></i></button></span>');
        if (f.keyword) add('keyword', '“' + f.keyword + '”');
        if (f.location) add('location', f.location);
        if (f.category) add('category', f.category);
        if (f.employmentType) add('employmentType', FILTER_LABELS.employmentType[f.employmentType]);
        if (f.workplaceType) add('workplaceType', FILTER_LABELS.workplaceType[f.workplaceType]);
        if (f.experienceLevel) add('experienceLevel', FILTER_LABELS.experienceLevel[f.experienceLevel]);
        if (f.minSalary) add('minSalary', JC.money(f.minSalary) + '+');
        if (f.companyId) {
            const opt = $('filter-company').selectedOptions[0];
            add('companyId', opt ? opt.textContent : 'Company');
        }
        const box = $('active-filters');
        box.innerHTML = chips.join('');
        box.querySelectorAll('[data-clear]').forEach(btn => btn.addEventListener('click', () => {
            clearFilter(btn.getAttribute('data-clear'));
            load(0);
        }));
    }

    function clearFilter(key) {
        switch (key) {
            case 'keyword': $('search-keyword').value = ''; break;
            case 'location': $('search-location').value = ''; break;
            case 'category': $('filter-category').value = ''; break;
            case 'employmentType': setRadio('employmentType', ''); break;
            case 'workplaceType': setRadio('workplaceType', ''); break;
            case 'experienceLevel': $('filter-experience').value = ''; break;
            case 'minSalary': $('filter-min-salary').value = ''; break;
            case 'companyId': $('filter-company').value = ''; break;
        }
    }

    async function load(page) {
        currentPage = page || 0;
        const container = $('jobs-results-container');
        const filters = readFilters();
        syncUrl(filters, currentPage);
        renderActiveFilters(filters);
        container.innerHTML = JC.skeletonCards(4);

        const params = new URLSearchParams({ page: currentPage, size: PAGE_SIZE, sort: filters.sort });
        ['keyword', 'location', 'category', 'employmentType', 'workplaceType', 'experienceLevel', 'minSalary', 'companyId']
            .forEach(k => { if (filters[k]) params.append(k, filters[k]); });

        try {
            const res = await JC.api('/api/jobs?' + params.toString());
            const page = res.data;
            const jobs = page.content || [];
            const total = page.totalElements;
            const from = total === 0 ? 0 : page.number * page.size + 1;
            const to = page.number * page.size + jobs.length;
            $('results-count').innerHTML = total === 0 ? 'No matching jobs' :
                '<span class="text-muted fw-normal">Showing</span> ' + from + '–' + to + ' <span class="text-muted fw-normal">of</span> ' + total + ' <span class="text-muted fw-normal">jobs</span>';

            if (!jobs.length) {
                container.innerHTML = '<div class="card-x">' + JC.emptyState('bi-search', 'No jobs match those filters',
                    'Try removing a filter, widening the location, or searching for a related skill.',
                    '<button type="button" class="btn btn-outline" id="empty-reset">Clear all filters</button>') + '</div>';
                $('empty-reset').addEventListener('click', resetAll);
                $('jobs-pagination').innerHTML = '';
                return;
            }

            if (view === 'grid') {
                container.innerHTML = '<div class="row g-3">' + jobs.map(j => '<div class="col-md-6">' + JC.jobCard(j, { grid: true }) + '</div>').join('') + '</div>';
            } else {
                container.innerHTML = '<div class="d-flex flex-column gap-3">' + jobs.map(j => JC.jobCard(j)).join('') + '</div>';
            }
            JC.pagination($('jobs-pagination'), page, p => {
                load(p);
                window.scrollTo({ top: container.getBoundingClientRect().top + window.scrollY - 100, behavior: 'smooth' });
            });
        } catch (e) {
            container.innerHTML = '<div class="alert-x error"><i class="bi bi-exclamation-octagon"></i><div>' + JC.esc(e.message) + '</div></div>';
        }
    }

    function resetAll() {
        ['search-keyword', 'search-location'].forEach(id => { $(id).value = ''; });
        ['filter-category', 'filter-experience', 'filter-min-salary', 'filter-company'].forEach(id => { $(id).value = ''; });
        setRadio('employmentType', '');
        setRadio('workplaceType', '');
        $('sort-by').value = 'createdAt,desc';
        load(0);
    }

    function syncViewButtons() {
        document.querySelectorAll('[data-view]').forEach(b => b.classList.toggle('active', b.getAttribute('data-view') === view));
    }

    document.addEventListener('DOMContentLoaded', () => {
        applyFromUrl();

        $('job-search-form').addEventListener('submit', e => { e.preventDefault(); load(0); });
        ['filter-category', 'filter-experience', 'filter-min-salary', 'filter-company', 'sort-by']
            .forEach(id => $(id).addEventListener('change', () => load(0)));
        document.querySelectorAll('.chip-check input').forEach(el => el.addEventListener('change', () => load(0)));
        $('btn-reset-filters').addEventListener('click', resetAll);
        document.querySelectorAll('[data-view]').forEach(btn => btn.addEventListener('click', () => {
            view = btn.getAttribute('data-view');
            try { localStorage.setItem('jc-job-view', view); } catch (e) { /* ignore */ }
            syncViewButtons();
            load(currentPage);
        }));

        load(currentPage);
    });
})();
