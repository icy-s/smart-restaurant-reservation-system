const form = document.getElementById('searchForm');
const plan = document.getElementById('plan');
const info = document.getElementById('algorithmInfo');

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
    plan.innerHTML = `
        <div class="legend">
            <span class="l-available">Vaba</span>
            <span class="l-occupied">Hõivatud</span>
            <span class="l-recommended">Soovitatud</span>
        </div>`;
    info.textContent = data.algorithmInfo;
    info.className = 'alert alert-secondary py-2 mb-3';

    data.tables.forEach(item => {
        const card = document.createElement('div');
        const cssState = item.occupied ? 'occupied' : item.recommended ? 'recommended' : 'available';
        card.className = `table ${cssState}`;
        card.style.left = `${item.table.x}px`;
        card.style.top = `${item.table.y}px`;
        card.innerHTML = `
            <div class="table-title">${item.table.id}</div>
            <div>${item.table.seats} kohta</div>
            <div>${zoneLabel(item.table.zone)}</div>
            <small class="reason">${item.reason}</small>`;
        plan.appendChild(card);
    });
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

form.addEventListener('submit', loadAvailability);
loadAvailability();
