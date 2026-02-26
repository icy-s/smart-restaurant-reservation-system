const form = document.getElementById('searchForm');
const plan = document.getElementById('plan');
const info = document.getElementById('algorithmInfo');
const adminToggle = document.getElementById('adminMode');
const saveLayoutBtn = document.getElementById('saveLayout');
const mealSuggestion = document.getElementById('mealSuggestion');

let adminMode = false;
let latestTables = [];
const TABLE_WIDTH = 132;
const TABLE_HEIGHT = 96;
const TABLE_GAP = 16;
const PLAN_PADDING = 12;

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

    const positions = resolveInitialOverlaps(data.tables);

    data.tables.forEach(item => {
        const card = document.createElement('div');
        const cssState = item.occupied ? 'occupied' : item.recommended ? 'recommended' : 'available';
        card.className = `table ${cssState}`;
        const position = positions.get(item.table.id) ?? { x: item.table.x, y: item.table.y };
        card.style.left = `${position.x}px`;
        card.style.top = `${position.y}px`;
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
    let activePointerId = null;

    const pointerMoveHandler = (e) => {
        if (!dragging || e.pointerId !== activePointerId) return;
        const bounds = plan.getBoundingClientRect();
        const maxX = Math.max(0, bounds.width - card.offsetWidth - PLAN_PADDING);
        const maxY = Math.max(30, bounds.height - card.offsetHeight - PLAN_PADDING);
        const nextX = Math.max(0, Math.min(maxX, e.clientX - bounds.left - offsetX));
        const nextY = Math.max(30, Math.min(maxY, e.clientY - bounds.top - offsetY));
        card.style.left = `${nextX}px`;
        card.style.top = `${nextY}px`;
    };

    const stopDragging = (e) => {
        if (activePointerId !== null && e.pointerId !== activePointerId) return;
        dragging = false;
        activePointerId = null;
        card.classList.remove('dragging');
        card.releasePointerCapture?.(e.pointerId);
        card.removeEventListener('pointermove', pointerMoveHandler);
        card.removeEventListener('pointerup', stopDragging);
        card.removeEventListener('pointercancel', stopDragging);
    };

    card.onpointerdown = (e) => {
        if (e.button !== 0) return;
        dragging = true;
        activePointerId = e.pointerId;
        const cardRect = card.getBoundingClientRect();
        offsetX = e.clientX - cardRect.left;
        offsetY = e.clientY - cardRect.top;
        card.classList.add('dragging');
        card.setPointerCapture?.(e.pointerId);
        card.addEventListener('pointermove', pointerMoveHandler);
        card.addEventListener('pointerup', stopDragging);
        card.addEventListener('pointercancel', stopDragging);
    };
}

function resolveInitialOverlaps(tables) {
    const positioned = [...tables]
        .sort((a, b) => (a.table.y - b.table.y) || (a.table.x - b.table.x));
    const occupied = [];
    const map = new Map();

    const planWidth = Math.max(plan.clientWidth || 0, TABLE_WIDTH + PLAN_PADDING * 2);
    const maxX = Math.max(0, planWidth - TABLE_WIDTH - PLAN_PADDING);

    positioned.forEach((item) => {
        let x = Math.max(0, Math.min(maxX, item.table.x));
        let y = Math.max(40, item.table.y);
        let attempts = 0;

        while (hasCollision(x, y, occupied) && attempts < 200) {
            x += TABLE_GAP;
            if (x > maxX) {
                x = Math.max(0, Math.min(maxX, item.table.x));
                y += TABLE_GAP;
            }
            attempts += 1;
        }

        occupied.push({ x, y });
        map.set(item.table.id, { x, y });
    });

    return map;
}

function hasCollision(x, y, occupied) {
    return occupied.some((pos) => Math.abs(pos.x - x) < (TABLE_WIDTH + TABLE_GAP)
        && Math.abs(pos.y - y) < (TABLE_HEIGHT + TABLE_GAP));
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
