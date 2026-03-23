let specializations = [];
let skills = [];
let proficiencyLevels = []; // НОВОЕ: переменная для уровней

document.addEventListener('DOMContentLoaded', function() {
    loadReferenceData();

    document.getElementById('addRole').addEventListener('click', addRole);
    document.getElementById('addSkill').addEventListener('click', addSkill);
    document.getElementById('projectForm').addEventListener('submit', prepareFormSubmit);
});

function loadReferenceData() {
    Promise.all([
        fetch('/api/profile-data/specializations').then(res => res.json()),
        fetch('/api/profile-data/skills').then(res => res.json()),
        fetch('/api/profile-data/proficiency-levels').then(res => res.json()) // НОВОЕ: загрузка уровней
    ]).then(([specs, skls, levels]) => {
        specializations = specs;
        skills = skls;
        proficiencyLevels = levels; // Сохраняем уровни

        // Добавляем по одной пустой строке сразу для удобства
        addRole();
        addSkill();
    }).catch(error => console.error('Ошибка загрузки данных:', error));
}

function addRole() {
    const template = document.getElementById('roleTemplate');
    const clone = template.content.cloneNode(true);
    document.getElementById('roles-container').appendChild(clone);

    const newItem = document.querySelector('.role-item:last-child');

    // Заполняем специализации
    fillSelect(newItem.querySelector('.role-specialization'), specializations);

    // НОВОЕ: Заполняем уровни владения
    fillSelect(newItem.querySelector('.role-proficiency'), proficiencyLevels);

    newItem.querySelector('.remove-item').addEventListener('click', () => newItem.remove());
}

function addSkill() {
    const template = document.getElementById('skillTemplate');
    const clone = template.content.cloneNode(true);
    document.getElementById('skills-container').appendChild(clone);

    const newItem = document.querySelector('.skill-item:last-child');
    fillSkillSelect(newItem.querySelector('.skill-select'));

    newItem.querySelector('.remove-item').addEventListener('click', () => newItem.remove());
}

// Универсальная функция заполнения простых списков
function fillSelect(select, data) {
    // Добавляем пустой вариант по умолчанию
    const defaultOption = new Option("Выберите...", "");
    defaultOption.disabled = true;
    defaultOption.selected = true;
    select.add(defaultOption);

    data.forEach(item => {
        // Используем item.displayName (для уровней) или item.name (для специализаций)
        const label = item.displayName || item.name;
        select.add(new Option(label, item.id));
    });
}

function fillSkillSelect(select) {
    const grouped = skills.reduce((acc, skill) => {
        if (!acc[skill.category]) acc[skill.category] = [];
        acc[skill.category].push(skill);
        return acc;
    }, {});

    Object.keys(grouped).sort().forEach(category => {
        const group = document.createElement('optgroup');
        group.label = category;
        grouped[category].forEach(s => group.appendChild(new Option(s.name, s.id)));
        select.appendChild(group);
    });
}

function prepareFormSubmit(e) {
    e.preventDefault();

    // Очистка старых скрытых полей
    document.querySelectorAll('input[name^="roles"], input[name^="skills"]').forEach(el => el.remove());

    const seenSkills = new Set();
    let hasDuplicates = false;

    // Сбор ролей
    document.querySelectorAll('.role-item').forEach((item, index) => {
        const specId = item.querySelector('.role-specialization').value;
        const proficiencyId = item.querySelector('.role-proficiency').value; // ИЗМЕНЕНО: получаем ID уровня
        const vacancies = item.querySelector('.role-vacancies').value;
        const desc = item.querySelector('.role-description').value;

        if (specId && proficiencyId) {
            addHiddenField(`roles[${index}].specializationId`, specId);
            addHiddenField(`roles[${index}].proficiencyLevelId`, proficiencyId); // ИЗМЕНЕНО: передаем proficiencyLevelId
            addHiddenField(`roles[${index}].vacanciesCount`, vacancies);
            addHiddenField(`roles[${index}].description`, desc);
        }
    });

    // Сбор навыков
    document.querySelectorAll('.skill-item').forEach((item, index) => {
        const skillId = item.querySelector('.skill-select').value;
        const isRequired = item.querySelector('.skill-required').checked;

        if (skillId) {
            if (seenSkills.has(skillId)) {
                hasDuplicates = true;
                item.querySelector('.skill-select').classList.add('is-invalid');
            } else {
                seenSkills.add(skillId);
                addHiddenField(`skills[${index}].skillId`, skillId);
                addHiddenField(`skills[${index}].required`, isRequired);
            }
        }
    });

    if (hasDuplicates) {
        alert('Пожалуйста, удалите дублирующиеся навыки.');
        return;
    }

    e.target.submit();
}

function addHiddenField(name, value) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    document.getElementById('projectForm').appendChild(input);
}