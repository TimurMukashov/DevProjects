let specializations = [];
let skills = [];
let proficiencyLevels = [];
// let avatarFile = null;

document.addEventListener('DOMContentLoaded', function() {
    loadReferenceData();

    document.getElementById('addSpecialization').addEventListener('click', addSpecialization);
    document.getElementById('addSkill').addEventListener('click', addSkill);
    // document.getElementById('addAvatar').addEventListener('click', addAvatar);
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

/*
function addAvatar() {
    const oldAvatar = document.querySelector('.avatar-item');
    if (oldAvatar) {
        oldAvatar.remove();
    }

    const template = document.getElementById('avatarTemplate');
    const clone = template.content.cloneNode(true);
    document.getElementById('avatar-container').appendChild(clone);

    const newItem = document.querySelector('.avatar-item:last-child');

    newItem.querySelector('.avatar-file').addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            // Проверяем размер файла (10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert('Файл слишком большой. Максимальный размер 10MB');
                e.target.value = '';
                return;
            }
            // Проверяем тип файла
            if (!file.type.startsWith('image/')) {
                alert('Можно загружать только изображения');
                e.target.value = '';
                return;
            }
            avatarFile = file;
        }
    });

    newItem.querySelector('.remove-item').addEventListener('click', function() {
        newItem.remove();
        avatarFile = null;
    });
}
*/

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

    document.querySelectorAll('.specialization-item').forEach((item, index) => {
        const select = item.querySelector('.specialization-select');
        const experience = item.querySelector('.specialization-experience');
        const isPrimary = item.querySelector('.specialization-primary');

        if (select.value && select.value !== '') {
            addHiddenField(`specializations[${index}].specializationId`, select.value);
            addHiddenField(`specializations[${index}].yearsOfExperience`, experience.value || '0');
            addHiddenField(`specializations[${index}].primary`, isPrimary.checked);
            console.log(`Добавлена специализация ID: ${select.value}`);
        }
    });

    document.querySelectorAll('.skill-item').forEach((item, index) => {
        const skillSelect = item.querySelector('.skill-select');
        const levelSelect = item.querySelector('.skill-level');
        const experience = item.querySelector('.skill-experience');

        if (skillSelect.value && skillSelect.value !== '' &&
            levelSelect.value && levelSelect.value !== '') {
            addHiddenField(`skills[${index}].skillId`, skillSelect.value);
            addHiddenField(`skills[${index}].proficiencyLevelId`, levelSelect.value);
            addHiddenField(`skills[${index}].yearsOfExperience`, experience.value || '0');
            console.log(`Добавлен навык ID: ${skillSelect.value}, уровень: ${levelSelect.value}`);
        }
    });

    /*
    if (avatarFile) {
        console.log('Добавляем аватар:', avatarFile.name);
        const formData = new FormData(e.target);

        document.querySelectorAll('input[type="hidden"]').forEach(input => {
            formData.append(input.name, input.value);
        });

        formData.append('avatar', avatarFile);

        fetch('/register', {
            method: 'POST',
            body: formData
        }).then(response => {
            if (response.redirected) {
                window.location.href = response.url;
            } else {
                response.text().then(text => {
                    console.error('Ошибка регистрации:', text);
                    alert('Ошибка при регистрации.');
                });
            }
        }).catch(error => {
            console.error('Ошибка отправки:', error);
            alert('Ошибка при отправке формы');
        });
    } else {
        console.log('Отправка без аватара');
        e.target.submit();
    }
    */

    console.log('Отправка без аватара');
    e.target.submit();
}

function addHiddenField(name, value) {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    document.getElementById('registrationForm').appendChild(input);
}