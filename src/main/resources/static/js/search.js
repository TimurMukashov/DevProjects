class ProjectSearch {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.searchResults = document.getElementById('searchResults');
        this.regularProjects = document.getElementById('regularProjects');
        this.searchResultsContainer = document.getElementById('searchResultsContainer');
        this.searchNoResults = document.getElementById('searchNoResults');
        this.searchQuerySpan = document.getElementById('searchQuery');
        this.resultsCountSpan = document.getElementById('resultsCount');
        this.projectCardTemplate = document.getElementById('projectCardTemplate');
        this.roleItemTemplate = document.getElementById('roleItemTemplate');
        this.searchTimeout = null;
        this.init();
    }

    init() {
        if (!this.searchInput) return;
        this.searchInput.addEventListener('input', (e) => {
            const query = e.target.value.trim();
            clearTimeout(this.searchTimeout);
            if (query.length < 2) {
                this.showRegular();
                return;
            }
            this.searchTimeout = setTimeout(() => this.performSearch(query), 300);
        });
    }

    async performSearch(query) {
        try {
            const response = await fetch(`/api/search/live?query=${encodeURIComponent(query)}`);
            const projects = await response.json();
            this.displayResults(projects, query);
        } catch (e) {
            console.error(e);
        }
    }

    displayResults(projects, query) {
        this.searchResultsContainer.innerHTML = '';
        this.searchQuerySpan.textContent = `"${query}"`;
        if (projects.length === 0) {
            this.resultsCountSpan.textContent = 'найдено: 0';
            this.searchNoResults.style.display = 'block';
        } else {
            this.resultsCountSpan.textContent = `найдено: ${projects.length}`;
            this.searchNoResults.style.display = 'none';
            projects.forEach(p => this.searchResultsContainer.appendChild(this.createCard(p)));
        }
        this.regularProjects.style.display = 'none';
        this.searchResults.style.display = 'block';
    }

    createCard(p) {
        const clone = this.projectCardTemplate.content.cloneNode(true);
        const card = clone.querySelector('.project-card');
        const projectUrl = `/projects/${p.id}`;

        card.onclick = () => window.location.href = projectUrl;

        const badge = clone.querySelector('.status-badge');
        badge.textContent = p.statusText;
        badge.className = `status-badge badge bg-${p.statusColor}`;

        const titleLink = clone.querySelector('.project-title a');
        titleLink.textContent = p.title;
        titleLink.href = projectUrl;

        clone.querySelector('.project-description').textContent = p.shortDescription;
        clone.querySelector('.author-name').textContent = p.authorName;
        clone.querySelector('.views-count').textContent = p.viewsCount;

        const rolesCont = clone.querySelector('.roles-container');
        p.roles.forEach(r => {
            const roleEl = this.roleItemTemplate.content.cloneNode(true);
            roleEl.querySelector('.role-name').textContent = r.specialization || r.title;
            roleEl.querySelector('.role-count').textContent = `${r.filled}/${r.vacancies}`;
            rolesCont.appendChild(roleEl);
        });

        const skillsCont = clone.querySelector('.skills-container');
        p.skills.forEach(s => {
            const span = document.createElement('span');
            span.className = `skill-badge ${s.required ? 'required' : 'optional'}`;
            span.textContent = s.name;
            skillsCont.appendChild(span);
        });

        return clone;
    }

    showRegular() {
        this.searchResults.style.display = 'none';
        this.regularProjects.style.display = 'block';
    }
}
document.addEventListener('DOMContentLoaded', () => new ProjectSearch());