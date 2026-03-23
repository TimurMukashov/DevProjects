let specializations = [];
let skills = [];
let proficiencyLevels = [];

document.addEventListener('DOMContentLoaded', async function() {
    await loadReferenceData();

    document.getElementById('addRole').onclick = () => {
        const row = addRole();
        fillSelects(row);
    };

    document.getElementById('addSkill').onclick = () => {
        const row = addSkill();
        fillSkillSelect(row.querySelector('.skill-select'));
    };

    document.getElementById('projectForm').onsubmit = prepareFormSubmit;

    const data = window.initialProjectData;
    if (data && (data.roles?.length || data.skills?.length)) {
        fillExistingData(data);
    }
});

async function loadReferenceData() {
    try {
        const [specs, skls, levels] = await Promise.all([
            fetch('/api/profile-data/specializations').then(res => res.json()),
            fetch('/api/profile-data/skills').then(res => res.json()),
            fetch('/api/profile-data/proficiency-levels').then(res => res.json())
        ]);
        specializations = specs;
        skills = skls;
        proficiencyLevels = levels;
    } catch (e) {
        console.error("Ошибка загрузки справочников:", e);
    }
}

function fillExistingData(data) {
    // Отрисовка ролей
    data.roles?.forEach(role => {
        const row = addRole();
        fillSelects(row); // Заполняем dropdown-ы перед установкой значений
        row.querySelector('.role-id').value = role.id;
        row.querySelector('.role-specialization').value = role.specializationId;
        row.querySelector('.role-proficiency').value = role.proficiencyLevelId || '';
        row.querySelector('.role-vacancies').value = role.vacanciesCount || 1;
        row.querySelector('.role-description').value = role.description || '';
    });

    // Отрисовка навыков
    data.skills?.forEach(skill => {
        const row = addSkill();
        fillSkillSelect(row.querySelector('.skill-select'));
        row.querySelector('.skill-id').value = skill.id;
        row.querySelector('.skill-select').value = skill.skillId;
        row.querySelector('.skill-required').checked = skill.required;
    });
}

function addRole() {
    return createFromTemplate('roleTemplate', 'roles-container');
}

function addSkill() {
    return createFromTemplate('skillTemplate', 'skills-container');
}

function fillSelects(row) {
    // Специализации
    const specSelect = row.querySelector('.role-specialization');
    if (specSelect.options.length === 0) {
        specSelect.add(new Option('Выберите специализацию...', ''));
        specializations.forEach(s => specSelect.add(new Option(s.name, s.id)));
    }

    // Уровни владения
    const profSelect = row.querySelector('.role-proficiency');
    if (profSelect.options.length === 0) {
        profSelect.add(new Option('Выберите уровень...', ''));
        proficiencyLevels.forEach(l => profSelect.add(new Option(l.displayName || l.name, l.id)));
    }
}

function fillSkillSelect(select) {
    if (select.options.length > 0) return;

    select.add(new Option('Выберите технологию...', ''));
    const grouped = skills.reduce((acc, s) => {
        acc[s.category] = acc[s.category] || [];
        acc[s.category].push(s);
        return acc;
    }, {});

    Object.keys(grouped).sort().forEach(cat => {
        const group = document.createElement('optgroup');
        group.label = cat;
        grouped[cat].forEach(s => group.appendChild(new Option(s.name, s.id)));
        select.appendChild(group);
    });
}

function createFromTemplate(templateId, containerId) {
    const template = document.getElementById(templateId);
    const container = document.getElementById(containerId);
    const clone = template.content.cloneNode(true);
    const newItem = clone.firstElementChild; // Получаем сам добавленный элемент

    container.appendChild(clone);

    // Привязываем кнопку удаления к новому элементу
    newItem.querySelector('.remove-item').onclick = function() {
        this.closest('.item-row').remove();
    };

    return newItem;
}

function prepareFormSubmit(e) {
    e.preventDefault();

    // Чистим старые скрытые поля
    const form = document.getElementById('projectForm');
    form.querySelectorAll('input[type="hidden"][name^="roles["]').forEach(el => el.remove());
    form.querySelectorAll('input[type="hidden"][name^="skills["]').forEach(el => el.remove());

    // Сбор РОЛЕЙ
    let roleIndex = 0;
    document.querySelectorAll('.role-item').forEach((item) => {
        const id = item.querySelector('.role-id').value;
        const specId = item.querySelector('.role-specialization').value;
        const proficiencyId = item.querySelector('.role-proficiency').value;
        const vacancies = item.querySelector('.role-vacancies').value;
        const description = item.querySelector('.role-description').value;

        if (specId && proficiencyId) {
            if (id) addHiddenField(`roles[${roleIndex}].id`, id, form);
            addHiddenField(`roles[${roleIndex}].specializationId`, specId, form);
            addHiddenField(`roles[${roleIndex}].proficiencyLevelId`, proficiencyId, form);
            addHiddenField(`roles[${roleIndex}].vacanciesCount`, vacancies || 1, form);
            addHiddenField(`roles[${roleIndex}].description`, description || '', form);
            roleIndex++;
        }
    });

    // Сбор НАВЫКОВ
    let skillIndex = 0;
    document.querySelectorAll('.skill-item').forEach((item) => {
        const id = item.querySelector('.skill-id').value;
        const skillId = item.querySelector('.skill-select').value;
        const isRequired = item.querySelector('.skill-required').checked;

        if (skillId) {
            if (id) addHiddenField(`skills[${skillIndex}].id`, id, form);
            addHiddenField(`skills[${skillIndex}].skillId`, skillId, form);
            addHiddenField(`skills[${skillIndex}].required`, isRequired, form);
            skillIndex++;
        }
    });

    form.submit();
}

function addHiddenField(name, value, form) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
}