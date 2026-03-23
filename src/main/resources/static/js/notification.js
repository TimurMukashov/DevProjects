document.addEventListener('DOMContentLoaded', function() {
    const bell = document.getElementById('bellDropdown');
    if (bell) {
        bell.addEventListener('click', function() {
            const badge = this.querySelector('.badge');

            if (!badge || badge.style.display === 'none') return;

            const token = document.querySelector('meta[name="_csrf"]')?.content;
            const header = document.querySelector('meta[name="_csrf_header"]')?.content;

            fetch('/dashboard/notifications/read', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [header]: token
                }
            }).then(response => {
                if (response.ok) {
                    badge.style.display = 'none';
                }
            });
        });
    }
});