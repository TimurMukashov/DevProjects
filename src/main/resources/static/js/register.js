let specializations = [];
let skills = [];
let proficiencyLevels = [];

document.addEventListener('DOMContentLoaded', function() {
    loadReferenceData();

    // Логика превью аватара
    const avatarInput = document.getElementById('avatarInput');
    const avatarPreview = document.getElementById('avatarPreview');
    if (avatarInput && avatarPreview) {
        avatarInput.addEventListener('change', function() {
            const [file] = this.files;
            if (file) {
                avatarPreview.src = URL.createObjectURL(file);
            }
        });
    }

    document.getElementById('addSpecialization').addEventListener('click', addSpecialization);
    document.getElementById('addSkill').addEventListener('click', addSkill);
    document.getElementById('registrationForm').addEventListener('submit', prepareFormSubmit);
});

function loadReferenceData() {
    Promise.all([
        fetch('/api/profile-data/specializations').then(res => res.json()),
        fetch('/api/profile-data/skills').then(res => res.json()),
        fetch('/api/profile-data/proficiency-levels').then(res => res.json())
    ]).then(([specs, skls, levels]) => {
        specializations = specs;
        skills = skls;
        proficiencyLevels = levels;

        addSpecialization();
        addSkill();
    }).catch(error => {
        console.error('Ошибка загрузки данных:', error);
    });
}

function addSpecialization() {
    const template = document.getElementById('specializationTemplate');
    const clone = template.content.cloneNode(true);
    document.getElementById('specializations-container').appendChild(clone);

    const newItem = document.querySelector('.specialization-item:last-child');
    fillSpecializationSelect(newItem.querySelector('.specialization-select'));

    newItem.querySelector('.remove-item').addEventListener('click', function() {
        newItem.remove();
    });
}

function addSkill() {
    const template = document.getElementById('skillTemplate');
    const clone = template.content.cloneNode(true);
    document.getElementById('skills-container').appendChild(clone);

    const newItem = document.querySelector('.skill-item:last-child');
    fillSkillSelect(newItem.querySelector('.skill-select'));
    fillLevelSelect(newItem.querySelector('.skill-level'));

    newItem.querySelector('.remove-item').addEventListener('click', function() {
        newItem.remove();
    });
}

function fillSpecializationSelect(select) {
    while (select.options.length > 1) {
        select.remove(1);
    }

    specializations.forEach(spec => {
        const option = new Option(spec.name, spec.id);
        select.add(option);
    });
}

function fillSkillSelect(select) {
    while (select.options.length > 1) {
        select.remove(1);
    }

    const grouped = skills.reduce((acc, skill) => {
        if (!acc[skill.category]) acc[skill.category] = [];
        acc[skill.category].push(skill);
        return acc;
    }, {});

    Object.keys(grouped).sort().forEach(category => {
        const optgroup = document.createElement('optgroup');
        optgroup.label = category;
        grouped[category].sort((a, b) => a.name.localeCompare(b.name)).forEach(skill => {
            const option = new Option(skill.name, skill.id);
            optgroup.appendChild(option);
        });
        select.appendChild(optgroup);
    });
}

function fillLevelSelect(select) {
    if (!select) return; // Проверка на случай отсутствия элемента
    while (select.options.length > 1)
        select.remove(1);

    proficiencyLevels.sort((a, b) => a.id - b.id).forEach(level => {
        const option = new Option(level.displayName, level.id);
        select.add(option);
    });
}

function prepareFormSubmit(e) {
    e.preventDefault();

    console.log('Подготовка формы к отправке...');

    document.querySelectorAll('input[name^="specializations"]').forEach(el => el.remove());
    document.querySelectorAll('input[name^="skills"]').forEach(el => el.remove());

    const seenSpecs = new Set();
    const seenSkills = new Set();
    let hasDuplicates = false;

    // Сбор и проверка специализаций
    document.querySelectorAll('.specialization-item').forEach((item, index) => {
        const select = item.querySelector('.specialization-select');
        const experience = item.querySelector('.specialization-experience');
        const isPrimary = item.querySelector('.specialization-primary');

        if (select.value && select.value !== '') {
            const specId = select.value;
            if (seenSpecs.has(specId)) {
                hasDuplicates = true;
                select.classList.add('is-invalid');
            } else {
                seenSpecs.add(specId);
                select.classList.remove('is-invalid');
                addHiddenField(`specializations[${index}].specializationId`, select.value);
                addHiddenField(`specializations[${index}].yearsOfExperience`, experience.value || '0');
                addHiddenField(`specializations[${index}].primary`, isPrimary.checked);
            }
        }
    });

    // Сбор и проверка навыков
    document.querySelectorAll('.skill-item').forEach((item, index) => {
        const skillSelect = item.querySelector('.skill-select');
        const levelSelect = item.querySelector('.skill-level');
        const experience = item.querySelector('.skill-experience');

        if (skillSelect.value && skillSelect.value !== '') {
            const skillId = skillSelect.value;
            if (seenSkills.has(skillId)) {
                hasDuplicates = true;
                skillSelect.classList.add('is-invalid');
            } else {
                seenSkills.add(skillId);
                skillSelect.classList.remove('is-invalid');
                addHiddenField(`skills[${index}].skillId`, skillSelect.value);
                addHiddenField(`skills[${index}].proficiencyLevelId`, levelSelect.value);
                addHiddenField(`skills[${index}].yearsOfExperience`, experience.value || '0');
            }
        }
    });

    if (hasDuplicates) {
        alert('Пожалуйста, удалите дублирующиеся специализации или навыки.');
        return;
    }

    console.log('Отправка формы...');
    e.target.submit();
}

function addHiddenField(name, value) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    document.getElementById('registrationForm').appendChild(input);
}