document.addEventListener('DOMContentLoaded', function() {
    const avatarInput = document.getElementById('avatarInput');
    const avatarPreview = document.getElementById('avatarPreview');
    const placeholder = document.getElementById('avatarPlaceholder');

    if (avatarInput) {
        avatarInput.onchange = function (evt) {
            const [file] = this.files;
            if (file) {
                let prw = avatarPreview;
                if (!prw && placeholder) {
                    prw = document.createElement('img');
                    prw.id = 'avatarPreview';
                    prw.className = 'rounded-circle shadow-sm';
                    prw.style = 'width: 100px; height: 100px; object-fit: cover;';
                    placeholder.parentNode.replaceChild(prw, placeholder);
                }

                if (prw) {
                    prw.src = URL.createObjectURL(file);
                }
            }
        };
    }
});