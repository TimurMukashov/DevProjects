let specializations = [];
let skills = [];

document.addEventListener('DOMContentLoaded', async function() {
    // 1. Параллельно загружаем справочники
    await loadReferenceData();

    // 2. Инициализируем основные обработчики
    document.getElementById('addRole').onclick = () => addRole();
    document.getElementById('addSkill').onclick = () => addSkill();
    document.getElementById('projectForm').onsubmit = prepareFormSubmit;

    // 3. Заполняем форму данными (они уже гарантированно есть в window.initialProjectData)
    const data = window.initialProjectData;
    if (data && (data.roles?.length || data.skills?.length)) {
        fillExistingData(data);
    }
});

async function loadReferenceData() {
    try {
        const [specs, skls] = await Promise.all([
            fetch('/api/profile-data/specializations').then(res => res.json()),
            fetch('/api/profile-data/skills').then(res => res.json())
        ]);
        specializations = specs;
        skills = skls;
    } catch (e) {
        console.error("Ошибка загрузки справочников:", e);
    }
}

function fillExistingData(data) {
    // Отрисовка ролей
    data.roles?.forEach(role => {
        const row = addRole();
        row.querySelector('.role-id').value = role.id;
        row.querySelector('.role-specialization').value = role.specializationId;
        row.querySelector('.role-title').value = role.title || '';
        row.querySelector('.role-vacancies').value = role.vacanciesCount || 1;
        row.querySelector('.role-description').value = role.description || '';
    });

    // Отрисовка навыков
    data.skills?.forEach(skill => {
        const row = addSkill();
        row.querySelector('.skill-id').value = skill.id;
        row.querySelector('.skill-select').value = skill.skillId;
        row.querySelector('.skill-required').checked = skill.required;
    });
}

function addRole() {
    const row = createFromTemplate('roleTemplate', 'roles-container');
    const select = row.querySelector('.role-specialization');

    select.add(new Option('Выберите специализацию...', ''));
    specializations.forEach(s => select.add(new Option(s.name, s.id)));

    row.querySelector('.remove-item').onclick = () => row.remove();
    return row;
}

function addSkill() {
    const row = createFromTemplate('skillTemplate', 'skills-container');
    const select = row.querySelector('.skill-select');

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

    row.querySelector('.remove-item').onclick = () => row.remove();
    return row;
}

// Вспомогательная функция для создания элементов из шаблона
function createFromTemplate(templateId, containerId) {
    const template = document.getElementById(templateId);
    const container = document.getElementById(containerId);
    const clone = template.content.cloneNode(true);
    container.appendChild(clone);
    return container.lastElementChild;
}

function prepareFormSubmit(e) {
    e.preventDefault();

    // 1. Чистим старые скрытые поля (чтобы не дублировать при повторном клике)
    document.querySelectorAll('#projectForm input[type="hidden"][name*="["]').forEach(el => el.remove());

    // 2. Сбор РОЛЕЙ
    document.querySelectorAll('.role-item').forEach((item, index) => {
        const id = item.querySelector('.role-id').value;
        const specId = item.querySelector('.role-specialization').value;
        const title = item.querySelector('.role-title').value;
        const vacancies = item.querySelector('.role-vacancies').value;
        const description = item.querySelector('.role-description').value;

        if (specId) {
            // Имена должны быть roles[0].id, roles[0].specializationId и т.д.
            if (id) addHiddenField(`roles[${index}].id`, id);
            addHiddenField(`roles[${index}].specializationId`, specId);
            addHiddenField(`roles[${index}].title`, title || '');
            addHiddenField(`roles[${index}].vacanciesCount`, vacancies || 1);
            addHiddenField(`roles[${index}].description`, description || '');
        }
    });

    // 3. Сбор НАВЫКОВ
    document.querySelectorAll('.skill-item').forEach((item, index) => {
        const id = item.querySelector('.skill-id').value;
        const skillId = item.querySelector('.skill-select').value;
        const isRequired = item.querySelector('.skill-required').checked;

        if (skillId) {
            // Имена должны быть skills[0].id, skills[0].skillId, skills[0].required
            if (id) addHiddenField(`skills[${index}].id`, id);
            addHiddenField(`skills[${index}].skillId`, skillId);
            addHiddenField(`skills[${index}].required`, isRequired);
        }
    });

    console.log("Форма подготовлена, отправляю на сервер...");
    e.target.submit();
}

function addHiddenField(name, value) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    document.getElementById('projectForm').appendChild(input);
}