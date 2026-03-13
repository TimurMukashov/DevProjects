class ProjectSearch {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.searchForm = document.getElementById('searchForm');
        this.searchResults = document.getElementById('searchResults');
        this.regularProjects = document.getElementById('regularProjects');
        this.searchResultsContainer = document.getElementById('searchResultsContainer');
        this.searchNoResults = document.getElementById('searchNoResults');
        this.searchQuerySpan = document.getElementById('searchQuery');
        this.resultsCountSpan = document.getElementById('resultsCount');
        this.clearSearchBtn = document.getElementById('clearSearchBtn');

        this.projectCardTemplate = document.getElementById('projectCardTemplate');
        this.roleItemTemplate = document.getElementById('roleItemTemplate');
        this.skillItemTemplate = document.getElementById('skillItemTemplate');

        this.searchTimeout = null;
        this.currentQuery = '';
        this.debounceDelay = 400;
        this.minQueryLength = 2;

        this.init();
    }

    init() {
        if (!this.searchInput) return;

        this.searchInput.addEventListener('input', (e) => this.handleInput(e));

        if (this.searchForm)
            this.searchForm.addEventListener('submit', (e) => this.handleSubmit(e));

        if (this.clearSearchBtn)
            this.clearSearchBtn.addEventListener('click', () => this.clearSearch());
    }

    handleInput(e) {
        const query = e.target.value.trim();

        clearTimeout(this.searchTimeout);

        if (query.length < this.minQueryLength) {
            this.showRegularProjects();
            this.currentQuery = '';
            return;
        }

        this.searchTimeout = setTimeout(() => {
            this.performSearch(query);
        }, this.debounceDelay);
    }

    handleSubmit(e) {
        e.preventDefault();

        const query = this.searchInput.value.trim();

        if (query.length >= this.minQueryLength) {
            clearTimeout(this.searchTimeout);
            this.performSearch(query);
        }
    }

    async performSearch(query) {
        this.currentQuery = query;

        try {
            const response = await fetch(
                `/api/search/live?query=${encodeURIComponent(query)}`
            );

            if (!response.ok)
                throw new Error(`Search error: ${response.status}`);

            const projects = await response.json();

            if (query !== this.currentQuery) return;

            this.displayResults(projects, query);

        } catch (error) {
            console.error('Search failed:', error);
            if (query === this.currentQuery)
                this.showError();
        }
    }

    displayResults(projects, query) {
        if (this.searchQuerySpan)
            this.searchQuerySpan.textContent = `"${query}"`;

        if (!projects || projects.length === 0) {
            if (this.resultsCountSpan)
                this.resultsCountSpan.textContent = 'найдено: 0';

            this.searchResults.style.display = 'block';
            this.searchNoResults.style.display = 'block';
            this.searchResultsContainer.innerHTML = '';
            this.regularProjects.style.display = 'none';
            return;
        }

        if (this.resultsCountSpan)
            this.resultsCountSpan.textContent = `найдено: ${projects.length}`;

        this.searchResultsContainer.innerHTML = '';

        projects.forEach(project => {
            const card = this.createProjectCard(project);
            this.searchResultsContainer.appendChild(card);
        });

        this.searchResults.style.display = 'block';
        this.searchNoResults.style.display = 'none';
        this.regularProjects.style.display = 'none';
    }

    createProjectCard(project) {
        const template = this.projectCardTemplate.content.cloneNode(true);
        const card = template.querySelector('.project-card');

        if (card)
            card.setAttribute('onclick', `window.location.href='/projects/${project.id}'`);

        const statusBadge = template.querySelector('.status-badge');
        if (statusBadge)
            statusBadge.textContent = project.statusText || 'Открыт';

        const titleLink = template.querySelector('.project-title a');
        if (titleLink) {
            titleLink.textContent = this.escapeHtml(project.title || '');
            titleLink.href = `/projects/${project.id}`;
            titleLink.setAttribute('onclick', 'event.stopPropagation();');
        }

        const description = template.querySelector('.project-description');
        if (description) {
            description.textContent = this.escapeHtml(
                project.shortDescription || project.description || ''
            );
        }

        const authorName = template.querySelector('.author-name');
        if (authorName)
            authorName.textContent = this.escapeHtml(project.authorName || 'Автор');

        const viewsCount = template.querySelector('.views-count');
        if (viewsCount)
            viewsCount.textContent = project.viewsCount || 0;

        if (project.daysLeft && project.daysLeft > 0) {
            const daysInfo = template.querySelector('.days-info');
            const daysLeft = template.querySelector('.days-left');
            if (daysInfo && daysLeft) {
                daysLeft.textContent = `${project.daysLeft}д`;
                daysInfo.style.display = 'inline';
            }
        }

        const rolesContainer = template.querySelector('.roles-container');
        if (rolesContainer && project.roles && project.roles.length > 0) {
            project.roles.forEach(role => {
                const roleTemplate = this.roleItemTemplate.content.cloneNode(true);

                const roleName = roleTemplate.querySelector('.role-name');
                if (roleName) {
                    roleName.textContent = this.escapeHtml(
                        role.specialization || role.title || 'Роль'
                    );
                }

                const roleCount = roleTemplate.querySelector('.role-count');
                if (roleCount) {
                    roleCount.textContent = `${role.filled || 0}/${role.vacancies || 1}`;
                }

                rolesContainer.appendChild(roleTemplate);
            });
        }

        const skillsContainer = template.querySelector('.skills-container');
        if (skillsContainer && project.skills && project.skills.length > 0) {
            project.skills.forEach(skill => {
                const skillElement = document.createElement('span');
                skillElement.className = `skill-badge ${skill.required ? 'required' : 'optional'}`;
                skillElement.textContent = this.escapeHtml(skill.name || '');
                skillsContainer.appendChild(skillElement);
            });
        }

        return template;
    }

    showRegularProjects() {
        this.searchResults.style.display = 'none';
        this.searchNoResults.style.display = 'none';
        this.regularProjects.style.display = 'block';
    }

    clearSearch() {
        if (this.searchInput)
            this.searchInput.value = '';

        this.currentQuery = '';
        clearTimeout(this.searchTimeout);
        this.showRegularProjects();
    }

    showError() {
        this.searchNoResults.style.display = 'block';
        this.searchResultsContainer.innerHTML = '';
        if (this.resultsCountSpan)
            this.resultsCountSpan.textContent = 'ошибка поиска';
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new ProjectSearch();
    });
} else {
    new ProjectSearch();
}