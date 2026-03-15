async function toggleFavorite(projectId) {
    const btn = document.getElementById('favoriteBtn');
    const icon = document.getElementById('heartIcon');
    const text = document.getElementById('favoriteBtnText');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

    try {
        const response = await fetch(`/api/favorites/toggle/${projectId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            }
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status === 'added') {
                // Состояние: Добавлено
                btn.classList.replace('btn-outline-danger', 'btn-danger');
                icon.className = 'bi bi-heart-fill';
                text.innerText = 'В избранном';
            } else {
                // Состояние: Удалено
                btn.classList.replace('btn-danger', 'btn-outline-danger');
                icon.className = 'bi bi-heart';
                text.innerText = 'Добавить в избранное';
            }
        } else if (response.status === 401) {
            window.location.href = '/login';
        }
    } catch (error) {
        console.error('Ошибка при переключении избранного:', error);
    }
}