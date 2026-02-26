const form = document.getElementById('searchForm');
const plan = document.getElementById('plan');
const info = document.getElementById('algorithmInfo');
const adminToggle = document.getElementById('adminMode');
const saveLayoutBtn = document.getElementById('saveLayout');
const mealSuggestion = document.getElementById('mealSuggestion');

let adminMode = false;
let latestTables = [];

const now = new Date();
now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
document.getElementById('dateTime').value = now.toISOString().slice(0, 16);

function toApiDateTime(localValue) {
    return new Date(localValue).toISOString().slice(0, 19);
}

function loadAvailability(e) {
    if (e) e.preventDefault();

    const params = new URLSearchParams({
        dateTime: toApiDateTime(document.getElementById('dateTime').value),
        partySize: document.getElementById('partySize').value,
        privacy: document.getElementById('privacy').checked,
        window: document.getElementById('window').checked,
        accessibility: document.getElementById('accessibility').checked,
        kidsArea: document.getElementById('kidsArea').checked
    });

    const zone = document.getElementById('zone').value;
    if (zone) params.set('zone', zone);

    fetch(`/api/availability?${params.toString()}`)
        .then(r => r.json())
        .then(renderPlan)
        .catch(() => {
            info.textContent = 'Andmete laadimine ebaõnnestus.';
            info.className = 'alert alert-danger py-2 mb-3';
        });
}

function renderPlan(data) {
    latestTables = data.tables;
    plan.innerHTML = `
        <div class="legend">
            <span class="l-available">Vaba</span>
            <span class="l-occupied">Hõivatud</span>
            <span class="l-recommended">Soovitatud</span>
            <span class="l-merged">Liidetud laud</span>
        </div>`;
    info.textContent = data.algorithmInfo;
    info.className = 'alert alert-secondary py-2 mb-3';

    renderMealSuggestion(data.mealSuggestion);

    data.tables.forEach(item => {
        const card = document.createElement('div');
        const cssState = item.occupied ? 'occupied' : item.recommended ? 'recommended' : 'available';
        card.className = `table ${cssState}`;
        card.style.left = `${item.table.x}px`;
        card.style.top = `${item.table.y}px`;
        card.dataset.id = item.table.id;
        card.innerHTML = `
            <div class="table-title">${item.table.id}</div>
            <div>${item.table.seats} kohta</div>
            <div>${zoneLabel(item.table.zone)}</div>
            <div class="badges">${featureBadges(item.table)} ${item.merged ? '<span class="table-badge merge">🔗 Liitlaud</span>' : ''}</div>
            <small class="reason">${item.reason}</small>`;
        if (adminMode && !item.merged) {
            makeDraggable(card);
        }
        plan.appendChild(card);
    });
}

function renderMealSuggestion(meal) {
    if (!meal) {
        mealSuggestion.innerHTML = '';
        return;
    }

    mealSuggestion.innerHTML = `
        <div class="meal-card ${meal.fallback ? 'fallback' : ''}">
            ${meal.thumbnail ? `<img src="${meal.thumbnail}" alt="${meal.name}" />` : ''}
            <div>
                <div class="meal-title">🍲 Päevapraadi soovitus: ${meal.name}</div>
                <div class="meal-meta">Kategooria: ${meal.category}${meal.fallback ? ' (varusoovitus)' : ''}</div>
                <a href="${meal.sourceUrl}" target="_blank" rel="noopener noreferrer">Vaata retsepti</a>
            </div>
        </div>`;
}

function makeDraggable(card) {
    card.classList.add('draggable');
    let dragging = false;
    let offsetX = 0;
    let offsetY = 0;

    card.onmousedown = (e) => {
        dragging = true;
        offsetX = e.clientX - card.offsetLeft;
        offsetY = e.clientY - card.offsetTop;
        card.classList.add('dragging');
    };

    document.onmousemove = (e) => {
        if (!dragging) return;
        card.style.left = `${Math.max(0, e.clientX - plan.getBoundingClientRect().left - offsetX)}px`;
        card.style.top = `${Math.max(30, e.clientY - plan.getBoundingClientRect().top - offsetY)}px`;
    };

    document.onmouseup = () => {
        dragging = false;
        card.classList.remove('dragging');
    };
}

function saveLayout() {
    const updates = Array.from(plan.querySelectorAll('.table'))
        .filter(card => !card.dataset.id.includes('+'))
        .map(card => ({
            id: card.dataset.id,
            x: parseInt(card.style.left, 10),
            y: parseInt(card.style.top, 10)
        }));

    fetch('/api/admin/layout', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates)
    })
        .then(() => loadAvailability())
        .catch(() => alert('Laua paigutuse salvestamine ebaõnnestus'));
}

function featureBadges(table) {
    const items = [
        table.window ? '<span class="table-badge">🪟 Aken</span>' : '',
        table.privacy ? '<span class="table-badge">🤫 Vaikne</span>' : '',
        table.accessibility ? '<span class="table-badge">♿ Ligip.</span>' : '',
        table.kidsArea ? '<span class="table-badge">🧸 Lasteala</span>' : ''
    ];

    return items.filter(Boolean).join(' ');
}

function zoneLabel(zone) {
    switch (zone) {
        case 'INDOOR': return 'Sisesaal';
        case 'TERRACE': return 'Terrass';
        case 'PRIVATE_ROOM': return 'Privaatruum';
        default: return zone;
    }
}

adminToggle.addEventListener('change', (e) => {
    adminMode = e.target.checked;
    saveLayoutBtn.disabled = !adminMode;
    loadAvailability();
});

saveLayoutBtn.addEventListener('click', saveLayout);

form.addEventListener('submit', loadAvailability);
loadAvailability();
