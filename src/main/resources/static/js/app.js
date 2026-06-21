// App State
let currentUser = null; // Holds { username, email }
let activeRoomCode = null;
let roomState = null; // Last received room update
let stompClient = null;
let socket = null;

// --- NUEVA LÓGICA DE NAVEGACIÓN Y TABS ---
let activeAuthMode = 'login'; // 'login' o 'register'

function navigateToScreen(screenId) {
    // Escondemos todas las pantallas
    document.getElementById('screen-auth').style.display = 'none';
    document.getElementById('screen-lobby').style.display = 'none';
    document.getElementById('screen-game').style.display = 'none';
    
    // Mostramos la pantalla objetivo
    if (screenId === 'auth') {
        document.getElementById('screen-auth').style.display = 'flex';
    } else if (screenId === 'lobby') {
        document.getElementById('screen-lobby').style.display = 'block';
    } else if (screenId === 'game') {
        document.getElementById('screen-game').style.display = 'flex';
    }
}

function switchAuthTab(mode) {
    activeAuthMode = mode;
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const emailInputGroup = document.getElementById('emailInputGroup');
    const authSubmitBtn = document.getElementById('authSubmitBtn');
    
    if (mode === 'login') {
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
        emailInputGroup.style.display = 'none';
        authSubmitBtn.innerText = 'Entrar';
    } else {
        tabRegister.classList.add('active');
        tabLogin.classList.remove('active');
        emailInputGroup.style.display = 'flex';
        authSubmitBtn.innerText = 'Registrarse';
    }
}

function handleAuthSubmit() {
    if (activeAuthMode === 'login') {
        handleLogin();
    } else {
        handleRegister();
    }
}

// Position nodes radially around the 700x380 oval table
// Coordinate mapping based on seat number (1 to 8)
const seatCoords = {
    1: { top: '85%', left: '50%' },   // Bottom Center (Normal player focus)
    2: { top: '75%', left: '20%' },   // Bottom Left
    3: { top: '50%', left: '10%' },   // Middle Left
    4: { top: '25%', left: '20%' },   // Top Left
    5: { top: '15%', left: '50%' },   // Top Center
    6: { top: '25%', left: '80%' },   // Top Right
    7: { top: '50%', left: '90%' },   // Middle Right
    8: { top: '75%', left: '80%' }    // Bottom Right
};

const mobileSeatCoords = {
    1: { top: '88%', left: '50%' },   // Abajo Centro
    2: { top: '74%', left: '18%' },   // Abajo Izquierda
    3: { top: '50%', left: '14%' },   // Medio Izquierda
    4: { top: '26%', left: '18%' },   // Arriba Izquierda
    5: { top: '12%', left: '50%' },   // Arriba Centro
    6: { top: '26%', left: '82%' },   // Arriba Derecha
    7: { top: '50%', left: '86%' },   // Medio Derecha
    8: { top: '74%', left: '82%' }    // Abajo Derecha
};

const landscapeSeatCoords = {
    1: { top: '86%', left: '50%' },   // Abajo Centro
    2: { top: '78%', left: '24%' },   // Abajo Izquierda
    3: { top: '50%', left: '9%' },    // Medio Izquierda
    4: { top: '22%', left: '24%' },   // Arriba Izquierda
    5: { top: '14%', left: '50%' },   // Arriba Centro
    6: { top: '22%', left: '76%' },   // Arriba Derecha
    7: { top: '50%', left: '91%' },   // Medio Derecha
    8: { top: '78%', left: '76%' }    // Abajo Derecha
};

// Raise Slider Sync Helpers
function syncRaiseAmountFromSlider(val) {
    document.getElementById('raiseAmountInput').value = val;
}

function syncRaiseSliderFromInput(val) {
    const slider = document.getElementById('raiseSlider');
    if (slider) {
        slider.value = val;
    }
}

function handleRaiseClick() {
    const isMobile = window.innerWidth <= 950;
    const isMobilePortrait = isMobile && window.innerHeight > window.innerWidth;
    const sliderContainer = document.getElementById('raiseSliderContainer');
    const raiseBtn = document.getElementById('actionRaiseBtn');
    const raiseText = document.getElementById('actionRaiseText');
    const raiseIcon = document.getElementById('actionRaiseIcon');
    
    if (isMobilePortrait && (sliderContainer.style.display === 'none' || !sliderContainer.style.display)) {
        // Solo en móvil vertical se realiza la expansión del slider al hacer click
        sliderContainer.style.display = 'block';
        if (raiseText) raiseText.innerText = 'Confirmar';
        if (raiseIcon) raiseIcon.innerText = 'check';
        raiseBtn.style.background = '#10b981'; // Color verde de éxito para confirmar
        raiseBtn.style.color = 'white';
    } else {
        // En desktop y landscape, o si ya está desplegado en portrait, se envía la acción de Raise
        sendPlayerAction('RAISE');
    }
}

// Opciones avanzadas de la sala
function toggleAdvancedConfig() {
    const configPanel = document.getElementById('advancedRoomConfig');
    const toggleIcon = document.getElementById('advancedToggleIcon');
    if (configPanel.style.display === 'none' || !configPanel.style.display) {
        configPanel.style.display = 'flex';
        toggleIcon.innerText = 'expand_less';
    } else {
        configPanel.style.display = 'none';
        toggleIcon.innerText = 'expand_more';
    }
}

function toggleBlindsHandsConfig() {
    const isChecked = document.getElementById('blindsIncreaseInput').checked;
    const wrapper = document.getElementById('handsToIncreaseWrapper');
    wrapper.style.display = isChecked ? 'flex' : 'none';
}

function onGameModeChange() {
    const mode = document.getElementById('gameModeInput').value;
    const maxRebuysInput = document.getElementById('maxRebuysInput');
    const blindsIncreaseInput = document.getElementById('blindsIncreaseInput');
    
    if (mode === 'CASH') {
        maxRebuysInput.value = '';
        blindsIncreaseInput.checked = false;
    } else { // TOURNAMENT
        maxRebuysInput.value = '1';
        blindsIncreaseInput.checked = true;
    }
    toggleBlindsHandsConfig();
}

function closeOrientationModal() {
    document.getElementById('orientationModal').style.display = 'none';
}

// Initialize Console Logger
function log(message, type = 'info') {
    const consoleLog = document.getElementById('logConsole');
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    
    let typeColor = '';
    if (type === 'error') typeColor = 'color: var(--danger);';
    else if (type === 'success') typeColor = 'color: var(--primary);';
    else if (type === 'action') typeColor = 'color: var(--accent);';

    entry.innerHTML = `<span class="log-timestamp">[${time}]</span> <span style="${typeColor}">${message}</span>`;
    consoleLog.appendChild(entry);
    consoleLog.scrollTop = consoleLog.scrollHeight;
}

// Rest API Call Helper
async function apiCall(url, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const config = { method, headers };
    if (body) {
        config.body = JSON.stringify(body);
    }
    const response = await fetch(url, config);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
    }
    if (response.status === 204) return null;
    return await response.json().catch(() => null);
}

// Authentication Handlers
async function handleRegister() {
    const username = document.getElementById('usernameInput').value.trim();
    const email = document.getElementById('emailInput').value.trim();
    const password = document.getElementById('passwordInput').value;
    const banner = document.getElementById('authStatusBanner');

    if (!username || !email || !password) {
        showAuthBanner('Completa todos los campos', 'error');
        return;
    }

    try {
        const data = await apiCall('/api/auth/register', 'POST', { username, email, password });
        log(`Usuario registrado: ${data.username}`, 'success');
        loginUserSuccess(data);
    } catch (err) {
        log(`Error de registro: ${err.message}`, 'error');
        showAuthBanner(err.message, 'error');
    }
}

async function handleLogin() {
    const username = document.getElementById('usernameInput').value.trim();
    const password = document.getElementById('passwordInput').value;
    const banner = document.getElementById('authStatusBanner');

    if (!username || !password) {
        showAuthBanner('Completa usuario y contraseña', 'error');
        return;
    }

    try {
        const data = await apiCall('/api/auth/login', 'POST', { identifier: username, password });
        log(`Inicio de sesión exitoso: ${data.username}`, 'success');
        loginUserSuccess(data);
    } catch (err) {
        log(`Error de login: ${err.message}`, 'error');
        showAuthBanner(err.message, 'error');
    }
}

function loginUserSuccess(user) {
    currentUser = user;
    document.getElementById('loggedInUserLabel').innerText = user.username;
    navigateToScreen('lobby');
    log(`Hola ${user.username}, ya puedes unirte o crear salas.`);
}

function handleLogout() {
    disconnectWebSocket();
    currentUser = null;
    activeRoomCode = null;
    roomState = null;

    navigateToScreen('auth');
    
    // Clean table representation
    document.getElementById('playerSeatsContainer').innerHTML = '';
    document.getElementById('potDisplay').innerText = 'Pozo: $0';
    document.getElementById('phaseDisplay').innerText = 'Esperando inicio...';
    document.getElementById('blindsDisplay').style.display = 'none';
    document.getElementById('adminActionsPanel').style.display = 'none';
    document.getElementById('playerActionsBar').style.visibility = 'hidden';

    log('Sesión cerrada.');
}

function showAuthBanner(msg, type) {
    const banner = document.getElementById('authStatusBanner');
    banner.innerText = msg;
    banner.className = `status-banner ${type}`;
    banner.style.display = 'block';
    setTimeout(() => banner.style.display = 'none', 4000);
}

// Room management Rest APIs
async function createRoom() {
    const initialChips = parseInt(document.getElementById('initialChipsInput').value) || 1000;
    const gameMode = document.getElementById('gameModeInput').value;
    const maxRebuysVal = document.getElementById('maxRebuysInput').value.trim();
    const maxRebuys = maxRebuysVal === "" ? null : parseInt(maxRebuysVal);
    const blindsIncrease = document.getElementById('blindsIncreaseInput').checked;
    const handsToIncreaseVal = document.getElementById('handsToIncreaseInput').value.trim();
    const handsToIncrease = handsToIncreaseVal === "" ? null : parseInt(handsToIncreaseVal);

    try {
        const data = await apiCall('/api/rooms', 'POST', { 
            initialChips,
            gameMode,
            maxRebuys,
            blindsIncrease,
            handsToIncrease
        });
        log(`Sala creada con código: ${data.code}`, 'success');
        document.getElementById('roomCodeInput').value = data.code;
        await joinRoom(data.code);
    } catch (err) {
        log(`Error al crear sala: ${err.message}`, 'error');
    }
}

async function joinRoom(code = null) {
    const roomCode = (code || document.getElementById('roomCodeInput').value).trim().toUpperCase();
    if (!roomCode) {
        log('Por favor ingresa un código de sala', 'error');
        return;
    }

    try {
        log(`Uniéndose a la sala ${roomCode}...`);
        const player = await apiCall(`/api/rooms/${roomCode}/join`, 'POST', { username: currentUser.username });
        log(`Te uniste a la sala ${roomCode} en el asiento #${player.seatNumber}`, 'success');
        
        activeRoomCode = roomCode;
        document.getElementById('roomCodeBadge').innerText = roomCode;
        document.getElementById('adminActionsPanel').style.display = 'flex';

        // Connect WebSocket
        connectWebSocket(roomCode);

        // Cambiar a pantalla de juego
        navigateToScreen('game');

        // Mostrar modal de recomendación de orientación en móviles
        if (window.innerWidth <= 950) {
            document.getElementById('orientationModal').style.display = 'flex';
        }
    } catch (err) {
        log(`Error al unirse: ${err.message}`, 'error');
    }
}

// Host Rest APIs
async function startGame() {
    try {
        await apiCall(`/api/rooms/${activeRoomCode}/start`, 'POST');
        log('Comando de inicio de partida enviado', 'success');
    } catch (err) {
        log(`Error al iniciar: ${err.message}`, 'error');
    }
}

async function startNextHand() {
    try {
        await apiCall(`/api/rooms/${activeRoomCode}/next-hand`, 'POST');
        log('Comando de siguiente mano enviado', 'success');
    } catch (err) {
        log(`Error al iniciar siguiente mano: ${err.message}`, 'error');
    }
}

// End Hand Dialog Management
function openEndHandModal() {
    if (!roomState || !roomState.players) return;
    const list = document.getElementById('winnersCheckboxList');
    list.innerHTML = '';
    
    // Only players in hand are eligible to win
    roomState.players.filter(p => p.inHand).forEach(p => {
        const label = document.createElement('label');
        label.className = 'winner-checkbox-item';
        label.innerHTML = `
            <input type="checkbox" name="winnerCheckbox" value="${p.username}">
            <span>${p.username} (Asiento ${p.seatNumber})</span>
        `;
        list.appendChild(label);
    });

    document.getElementById('winnersModal').style.display = 'flex';
}

function closeEndHandModal() {
    document.getElementById('winnersModal').style.display = 'none';
}

async function submitEndHand() {
    const checkedBoxes = document.querySelectorAll('input[name="winnerCheckbox"]:checked');
    const winners = Array.from(checkedBoxes).map(cb => cb.value);
    
    if (winners.length === 0) {
        alert('Debes seleccionar al menos un ganador.');
        return;
    }

    try {
        await apiCall(`/api/rooms/${activeRoomCode}/end-hand`, 'POST', { winnerUsernames: winners });
        log(`Mano terminada. Ganadores declarados: ${winners.join(', ')}`, 'success');
        closeEndHandModal();
    } catch (err) {
        log(`Error al declarar ganadores: ${err.message}`, 'error');
    }
}

// Player actions Rest API
async function sendPlayerAction(actionType) {
    const amountInput = document.getElementById('raiseAmountInput');
    let amount = null;

    if (actionType === 'RAISE') {
        amount = parseInt(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            log('Por favor ingresa un monto válido para subir (Raise)', 'error');
            return;
        }
    }

    try {
        await apiCall(`/api/rooms/${activeRoomCode}/action/${currentUser.username}`, 'POST', {
            action: actionType,
            amount: amount
        });
        log(`Acción enviada: ${actionType} ${amount ? '$' + amount : ''}`, 'action');
    } catch (err) {
        log(`Error al enviar acción: ${err.message}`, 'error');
    }
}

// WebSockets STOMP Integration
function connectWebSocket(roomCode) {
    disconnectWebSocket(); // Clean up existing
    
    log('Estableciendo conexión en tiempo real...');
    socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    // Disable debug logging to keep log clean
    stompClient.debug = null;

    stompClient.connect({}, function(frame) {
        document.getElementById('wsConnStatus').className = 'connection-status online';
        log('✅ Conexión establecida con el servidor de eventos');
        
        // Suscribirse al canal de actualización de la sala pasándole el username
        stompClient.subscribe('/topic/room/' + roomCode, function(message) {
            const data = JSON.parse(message.body);
            handleRoomUpdate(data);
        }, { username: currentUser.username });
        
        log(`📝 Suscrito a las actualizaciones de la sala ${roomCode}`);
    }, function(error) {
        document.getElementById('wsConnStatus').className = 'connection-status offline';
        log(`❌ Error de WebSocket: ${error}`, 'error');
    });
}

// Handle Realtime updates
function handleRoomUpdate(update) {
    roomState = update;
    log(`Evento: ${update.lastAction || 'Actualización de mesa'}`, 'action');

    // Render Pot and Phase
    document.getElementById('potDisplay').innerText = `Pozo: $${update.mainPot ? update.mainPot.amount : 0}`;
    document.getElementById('phaseDisplay').innerText = update.phase ? update.phase : update.status;

    // Render blinds if active
    const blindsDisplay = document.getElementById('blindsDisplay');
    if (update.smallBlindAmount) {
        blindsDisplay.style.display = 'block';
        const modeLabel = update.gameMode === 'TOURNAMENT' ? 'Torneo' : 'Cash';
        blindsDisplay.innerText = `${modeLabel} - Ciegas: $${update.smallBlindAmount}/$${update.smallBlindAmount * 2}`;
    } else {
        blindsDisplay.style.display = 'none';
    }

    // Render Player seats
    renderPlayers(update.players, update.currentPlayerUsername);

    // Update Rebuy visibility
    updateRebuyButton(update);

    // Synchronize Actions Bar
    syncActionPanel(update);

    // Control Admin (Host / Dealer) Panel Visibility
    const me = update.players.find(p => currentUser && p.username === currentUser.username);
    const adminPanel = document.getElementById('adminActionsPanel');
    const startGameBtn = document.getElementById('startGameBtn');
    const endHandBtn = document.getElementById('endHandBtn');
    const nextHandBtn = document.getElementById('nextHandBtn');

    const isLobby = !update.dealerSeat || update.dealerSeat === 0;
    
    let hasAdminPrivilege = false;
    if (me) {
        if (isLobby) {
            hasAdminPrivilege = me.seatNumber === 1;
        } else {
            hasAdminPrivilege = me.seatNumber === update.dealerSeat;
        }
    }

    if (hasAdminPrivilege) {
        adminPanel.style.display = 'flex';
        if (isLobby) {
            // Lobby: solo iniciar partida
            startGameBtn.style.display = 'block';
            endHandBtn.style.display = 'none';
            nextHandBtn.style.display = 'none';
        } else if (update.status === 'PLAYING') {
            // Partida activa: solo terminar mano
            startGameBtn.style.display = 'none';
            endHandBtn.style.display = 'block';
            nextHandBtn.style.display = 'none';
        } else if (update.status === 'WAITING') {
            // Mano finalizada: solo siguiente mano
            startGameBtn.style.display = 'none';
            endHandBtn.style.display = 'none';
            nextHandBtn.style.display = 'block';
        }
    } else {
        adminPanel.style.display = 'none';
    }
}

function renderPlayers(players, currentTurnUsername) {
    const container = document.getElementById('playerSeatsContainer');
    container.innerHTML = '';

    // Solo mostrar indicador de turno si estamos jugando y no es SHOWDOWN
    const isBettingActive = roomState && roomState.status === 'PLAYING' && roomState.phase !== 'SHOWDOWN';

    players.forEach(p => {
        const node = document.createElement('div');
        const isMyTurn = isBettingActive && (p.username === currentTurnUsername);
        
        node.className = `player-node ${isMyTurn ? 'active-turn' : ''} ${!p.inHand ? 'folded' : ''}`;
        
        // Apply radial coordinates mapping (1-8 seats)
        // Usar mobileSeatCoords solo si está en móvil y en orientación portrait (vertical)
        const isMobile = window.innerWidth <= 950;
        const isMobilePortrait = isMobile && window.innerHeight > window.innerWidth;
        const isMobileLandscape = isMobile && window.innerWidth > window.innerHeight;
        const coordsSource = isMobilePortrait ? mobileSeatCoords : (isMobileLandscape ? landscapeSeatCoords : seatCoords);
        const coord = coordsSource[p.seatNumber] || { top: '50%', left: '50%' };
        node.style.top = coord.top;
        node.style.left = coord.left;

        // Connection status class
        const connClass = p.isConnected ? 'online' : 'offline';
        const connTitle = p.isConnected ? 'Conectado' : 'Desconectado';

        // status label (All in)
        let statusLabel = '';
        if (p.isAllIn) {
            statusLabel = `<span class="player-status-badge status-all-in">ALL-IN</span>`;
        } else if (!p.inHand && roomState.status === 'PLAYING') {
            statusLabel = `<span class="player-status-badge" style="background:#4b5563;color:white;">FOLDED</span>`;
        }

        // Is current logged in user highlight
        const isMe = (currentUser && p.username === currentUser.username);
        const nameStyle = isMe ? 'color: var(--accent); font-weight: 700;' : '';

        // Mostrar número de recompras
        const rebuyIndicator = p.rebuyCount > 0 ? ` <span style="font-size:10px;color:var(--text-secondary);font-weight:normal;">[R:${p.rebuyCount}]</span>` : '';

        // If this player is dealer, add dealer chip.
        const isDealer = (roomState && roomState.dealerSeat && p.seatNumber === roomState.dealerSeat);
        const dealerBtnHtml = isDealer ? '<div class="dealer-button" title="Dealer Button">D</div>' : '';

        const betLabel = isMobile ? `$${p.currentBet}` : `Apuesta: $${p.currentBet}`;
        const betBadgeHtml = p.currentBet > 0 ? `<div class="player-bet-badge">${betLabel}</div>` : '';

        node.innerHTML = `
            <div class="player-node-header">
                <span class="player-name" style="${nameStyle}" title="${p.username}">${p.username}${rebuyIndicator}</span>
                <span class="connection-status ${connClass}" title="${connTitle}"></span>
            </div>
            <div class="player-chips">$${p.chips}</div>
            ${betBadgeHtml}
            <div>${statusLabel}</div>
            ${isMyTurn ? '<div style="font-size:10px;color:var(--accent);margin-top:4px;font-weight:600;">PENSANDO...</div>' : ''}
            ${dealerBtnHtml}
        `;

        container.appendChild(node);
    });
}

function syncActionPanel(update) {
    const bar = document.getElementById('playerActionsBar');
    
    // Habilitar la barra de acciones solo si el juego está en curso y es mi turno
    // Y no es Showdown o finalizado
    const isMyTurn = (update.status === 'PLAYING' && 
                      update.phase !== 'SHOWDOWN' && 
                      update.phase !== null && 
                      update.currentPlayerUsername === currentUser.username);
    
    if (isMyTurn) {
        bar.style.visibility = 'visible';
        
        // Adjust Check / Call buttons based on highest bet
        const myBet = update.players.find(p => p.username === currentUser.username).currentBet || 0;
        
        // Fetch highest bet in room.
        const maxBet = Math.max(...update.players.map(p => p.currentBet || 0), 0);
        
        const actionCheckBtn = document.getElementById('actionCheckBtn');
        const actionCallBtn = document.getElementById('actionCallBtn');
        
        const callTextSpan = actionCallBtn.querySelector('.btn-text');
        if (maxBet > myBet) {
            // Hay una apuesta mayor, no se puede dar "Check", se debe pagar (Call) o Fold/Raise
            actionCheckBtn.disabled = true;
            actionCallBtn.disabled = false;
            if (callTextSpan) {
                callTextSpan.innerText = `Call ($${maxBet - myBet})`;
            } else {
                actionCallBtn.innerText = `Call ($${maxBet - myBet})`;
            }
        } else {
            // No hay apuestas previas en esta ronda, se puede dar "Check" sin costo
            actionCheckBtn.disabled = false;
            actionCallBtn.disabled = true;
            if (callTextSpan) {
                callTextSpan.innerText = `Call ($0)`;
            } else {
                actionCallBtn.innerText = `Call ($0)`;
            }
        }

        // Minimum raise amount must be higher than current max bet
        const minRaise = maxBet + 10; // Simple blind increment increment rule
        document.getElementById('raiseAmountInput').value = minRaise;
        document.getElementById('raiseAmountInput').min = minRaise;

        // Configurar Slider de Apuestas (Raise)
        const mePlayer = update.players.find(p => p.username === currentUser.username);
        const myChips = mePlayer.chips || 0;
        const totalChipsForRaise = myChips + myBet; // Fichas totales de las que dispone para subir la apuesta
        
        const raiseSlider = document.getElementById('raiseSlider');
        const raiseSliderContainer = document.getElementById('raiseSliderContainer');
        const raiseBtn = document.getElementById('actionRaiseBtn');
        
        // Restablecer el botón de Raise a su estado inicial
        const raiseText = document.getElementById('actionRaiseText');
        const raiseIcon = document.getElementById('actionRaiseIcon');
        if (raiseText) raiseText.innerText = 'Raise';
        if (raiseIcon) raiseIcon.innerText = 'trending_up';
        raiseBtn.style.background = ''; // Restablecer al color por defecto del CSS (btn-accent)
        raiseBtn.style.color = '';

        if (totalChipsForRaise >= minRaise) {
            const isMobile = window.innerWidth <= 950;
            const isMobilePortrait = isMobile && window.innerHeight > window.innerWidth;
            if (isMobilePortrait) {
                // Solo en móvil vertical (Portrait) se oculta por defecto
                raiseSliderContainer.style.display = 'none';
            } else {
                // En desktop y móvil horizontal (Landscape) se muestra de inmediato
                raiseSliderContainer.style.display = 'block';
            }
            raiseSlider.min = minRaise;
            raiseSlider.max = totalChipsForRaise;
            raiseSlider.value = minRaise;
            document.getElementById('sliderMinLabel').innerText = minRaise;
            document.getElementById('sliderMaxLabel').innerText = totalChipsForRaise;
        } else {
            // Si no tiene saldo suficiente para hacer raise, ocultar el slider
            raiseSliderContainer.style.display = 'none';
        }
    } else {
        bar.style.visibility = 'hidden';
        document.getElementById('raiseSliderContainer').style.display = 'none';
    }
}

async function requestRebuy() {
    if (!activeRoomCode || !currentUser) return;
    try {
        log(`Solicitando recompra de fichas...`);
        await apiCall(`/api/rooms/${activeRoomCode}/rebuy/${currentUser.username}`, 'POST');
        log(`¡Recompra realizada con éxito!`, 'success');
    } catch (err) {
        log(`Error al recomprar: ${err.message}`, 'error');
        alert(`Error al recomprar: ${err.message}`);
    }
}

function updateRebuyButton(update) {
    const rebuyContainer = document.getElementById('rebuyContainer');
    if (!currentUser || !update.players || !update.gameMode) {
        rebuyContainer.style.display = 'none';
        return;
    }
    const me = update.players.find(p => p.username === currentUser.username);
    if (!me) {
        rebuyContainer.style.display = 'none';
        return;
    }

    // No permitir recompras si no ha iniciado el juego (todavía en lobby / dealer no asignado)
    if (!update.dealerSeat || update.dealerSeat === 0) {
        rebuyContainer.style.display = 'none';
        return;
    }

    let isEligible = false;
    const isCash = update.gameMode === 'CASH';

    if (isCash) {
        // En Cash: puede recomprar si no está jugando la mano activa en esta ronda
        const notInActiveHand = !me.inHand || update.status !== 'PLAYING';
        const hasRebuysLeft = update.maxRebuys == null || me.rebuyCount < update.maxRebuys;
        isEligible = notInActiveHand && hasRebuysLeft;
    } else { // TOURNAMENT
        // En Torneo: solo si se quedó sin fichas (chips <= 0) Y la ciega pequeña es <= 40 Y quedan recompras
        const hasNoChips = me.chips <= 0;
        const inEarlyPhase = update.smallBlindAmount != null && update.smallBlindAmount <= 40;
        const hasRebuysLeft = update.maxRebuys == null || me.rebuyCount < update.maxRebuys;
        isEligible = hasNoChips && inEarlyPhase && hasRebuysLeft;
    }

    rebuyContainer.style.display = isEligible ? 'block' : 'none';
}

// Drawer toggle logic for logs
function toggleConsoleDrawer() {
    const drawer = document.getElementById('consoleDrawer');
    const overlay = document.getElementById('consoleDrawerOverlay');
    if (drawer && overlay) {
        drawer.classList.toggle('active');
        overlay.classList.toggle('active');
    }
}

// Exit Room Handler
function leaveRoom() {
    disconnectWebSocket();
    activeRoomCode = null;
    roomState = null;
    
    // Resetear UI de la mesa
    document.getElementById('playerSeatsContainer').innerHTML = '';
    document.getElementById('potDisplay').innerText = 'Pozo: $0';
    document.getElementById('phaseDisplay').innerText = 'Esperando inicio...';
    document.getElementById('blindsDisplay').style.display = 'none';
    document.getElementById('adminActionsPanel').style.display = 'none';
    document.getElementById('playerActionsBar').style.visibility = 'hidden';
    
    log('Saliste de la sala.');
    navigateToScreen('lobby');
}

function disconnectWebSocket() {
    if (stompClient) {
        try {
            stompClient.disconnect();
        } catch(e){}
        stompClient = null;
    }
    if (socket) {
        try {
            socket.close();
        } catch(e){}
        socket = null;
    }
    document.getElementById('wsConnStatus').className = 'connection-status offline';
}

// Registrar Service Worker para PWA
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then(reg => console.log('Service Worker registrado con éxito:', reg.scope))
            .catch(err => console.error('Error al registrar Service Worker:', err));
    });
}

// Re-renderizar jugadores en cambio de tamaño o rotación de pantalla
window.addEventListener('resize', () => {
    if (roomState && roomState.players) {
        renderPlayers(roomState.players, roomState.currentPlayerUsername);
    }
});
