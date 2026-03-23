document.addEventListener('DOMContentLoaded', function () {
    var applyModal = document.getElementById('applyModal');
    var applyForm = document.getElementById('applyForm');
    var filesInput = document.getElementById('filesInput');
    var fileError = document.getElementById('fileError');
    var submitBtn = document.getElementById('submitBtn');

    if (applyModal) {
        applyModal.addEventListener('show.bs.modal', function (event) {
            var button = event.relatedTarget;
            var roleId = button.getAttribute('data-role-id');
            var roleTitle = button.getAttribute('data-role-title');

            var modalRoleIdInput = applyModal.querySelector('#modalRoleId');
            var modalRoleTitleSpan = applyModal.querySelector('#modalRoleTitle');

            modalRoleIdInput.value = roleId;
            modalRoleTitleSpan.textContent = roleTitle;

            if (fileError) fileError.style.display = 'none';
            if (filesInput) filesInput.classList.remove('is-invalid');
        });
    }

    if (applyForm) {
        applyForm.addEventListener('submit', function (e) {
            var files = filesInput.files;
            var maxCount = 10;
            var maxSize = 5 * 1024 * 1024;
            var error = "";

            if (files.length > maxCount) {
                error = "Нельзя прикрепить более 10 файлов.";
            } else {
                for (var i = 0; i < files.length; i++) {
                    if (files[i].size > maxSize) {
                        error = "Файл '" + files[i].name + "' слишком большой (макс. 5 МБ).";
                        break;
                    }
                }
            }

            if (error) {
                e.preventDefault();
                fileError.textContent = error;
                fileError.style.display = 'block';
                filesInput.classList.add('is-invalid');
            } else {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Отправка...';
            }
        });
    }
});