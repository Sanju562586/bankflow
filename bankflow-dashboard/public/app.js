/**
 * Bankflow Client Tier Web Dashboard & Microservice Simulator
 */

const API_BASE = 'http://localhost:8080';

// Global State
const state = {
    accounts: [
        {
            id: 'acc-101',
            accountNumber: '4928172938',
            customerId: 'cust-101',
            customerName: 'Aarav Sharma',
            email: 'aarav.sharma@example.com',
            phoneNumber: '+919876543210',
            currency: 'INR',
            balance: 145000.00,
            status: 'ACTIVE'
        },
        {
            id: 'acc-102',
            accountNumber: '9281746201',
            customerId: 'cust-102',
            customerName: 'Priya Patel',
            email: 'priya.patel@example.com',
            phoneNumber: '+919811223344',
            currency: 'INR',
            balance: 62400.00,
            status: 'ACTIVE'
        },
        {
            id: 'acc-103',
            accountNumber: '3819482019',
            customerId: 'cust-103',
            customerName: 'Rohit Verma',
            email: 'rohit.verma@example.com',
            phoneNumber: '+919700112233',
            currency: 'INR',
            balance: 280000.00,
            status: 'ACTIVE'
        }
    ],
    ledger: [
        { txnRef: 'txn-init-101', type: 'CREDIT', amount: 145000, description: 'Initial Deposit', balanceAfter: 145000, timestamp: 'Today, 10:15 AM' },
        { txnRef: 'txn-init-102', type: 'CREDIT', amount: 62400, description: 'Initial Deposit', balanceAfter: 62400, timestamp: 'Today, 10:20 AM' },
        { txnRef: 'txn-init-103', type: 'CREDIT', amount: 280000, description: 'Salary Credit', balanceAfter: 280000, timestamp: 'Today, 10:30 AM' }
    ],
    fraudAlerts: [],
    notifications: [],
    kafkaEvents: [],
    tokens: {
        ROLE_CUSTOMER: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhYXJhdnNoYXJtYSIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiYmFua2Zsb3ctZ2F0ZXdheSIsImN1c3RvbWVySWQiOiJjdXN0LTEwMSJ9.signature',
        ROLE_ADMIN: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iLCJJPTEVfT1BFUkFUT1IiXSwiaXNzIjoiYmFua2Zsb3ctZ2F0ZXdheSJ9.signature',
        ROLE_OPERATOR: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcGVyYXRvciIsInJvbGVzIjpbIlJPTEVfT1BFUkFUT1IiXSwiaXNzIjoiYmFua2Zsb3ctZ2F0ZXdheSJ9.signature'
    },
    activeRole: 'ROLE_CUSTOMER'
};

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initAccounts();
    initPaymentTerminal();
    initPresets();
    initTokens();
    initModal();
    logKafka('SYSTEM', 'Kafka Event Bus online. Ready to stream txn-events, fraud-alerts, audit-log, notification-queue');
});

// Navigation Handling
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const crumbTitle = document.getElementById('active-crumb-title');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            const tab = item.getAttribute('data-tab');
            document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));
            const activePanel = document.getElementById(`view-panel-${tab}`) || document.getElementById(`view-${tab}`);
            if (activePanel) activePanel.classList.add('active');

            const title = item.querySelector('span').textContent;
            crumbTitle.textContent = title;
        });
    });
}

// Token & Role handling
function initTokens() {
    const roleSelector = document.getElementById('role-selector');
    const rolePill = document.getElementById('current-user-role');
    const jwtDisplay = document.getElementById('jwt-display');
    const claimsBox = document.getElementById('token-claims-box');

    function updateToken() {
        state.activeRole = roleSelector.value;
        rolePill.textContent = state.activeRole;
        const token = state.tokens[state.activeRole];
        if (jwtDisplay) jwtDisplay.value = token;
        if (claimsBox) {
            claimsBox.innerHTML = `<strong>Role:</strong> <code>${state.activeRole}</code> | <strong>Issued By:</strong> <code>bankflow-gateway</code> | <strong>Algorithm:</strong> <code>HMAC-SHA256</code>`;
        }
    }

    roleSelector.addEventListener('change', updateToken);
    updateToken();
}

// Accounts Hub
function initAccounts() {
    renderAccounts();
    populateAccountSelects();

    const ledgerSelect = document.getElementById('ledger-account-select');
    if (ledgerSelect) {
        ledgerSelect.addEventListener('change', (e) => {
            renderLedger(e.target.value);
        });
    }
}

function renderAccounts() {
    const container = document.getElementById('accounts-cards-container');
    if (!container) return;

    container.innerHTML = state.accounts.map(acc => `
        <div class="account-card" data-acc-id="${acc.id}">
            <div class="account-card-top">
                <div>
                    <h4>${acc.customerName}</h4>
                    <span class="text-muted" style="font-size: 0.75rem;">ID: ${acc.id}</span>
                </div>
                <span class="account-num">${acc.accountNumber}</span>
            </div>
            <div class="account-balance-label">AVAILABLE BALANCE</div>
            <div class="account-balance-val">₹${acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
            <div class="account-card-footer">
                <span><i class="fa-solid fa-phone"></i> ${acc.phoneNumber}</span>
                <span class="badge" style="color: var(--accent-green); background: rgba(16,185,129,0.1);">${acc.status}</span>
            </div>
        </div>
    `).join('');
}

function populateAccountSelects() {
    const fromSelect = document.getElementById('pay-from-account');
    const toSelect = document.getElementById('pay-to-account');
    const ledgerSelect = document.getElementById('ledger-account-select');

    const optionsFrom = state.accounts.map((acc, idx) => 
        `<option value="${acc.id}" ${idx === 0 ? 'selected' : ''}>${acc.customerName} (${acc.accountNumber}) - Bal: ₹${acc.balance.toLocaleString('en-IN')}</option>`
    ).join('');

    const optionsTo = state.accounts.map((acc, idx) => 
        `<option value="${acc.id}" ${idx === 1 ? 'selected' : ''}>${acc.customerName} (${acc.accountNumber})</option>`
    ).join('');

    if (fromSelect) fromSelect.innerHTML = optionsFrom;
    if (toSelect) toSelect.innerHTML = optionsTo;
    if (ledgerSelect) {
        ledgerSelect.innerHTML = state.accounts.map(acc => `<option value="${acc.id}">${acc.customerName} (${acc.accountNumber})</option>`).join('');
        renderLedger(state.accounts[0].id);
    }
}

function renderLedger(accountId) {
    const tbody = document.getElementById('ledger-body');
    if (!tbody) return;

    const filtered = state.ledger;
    tbody.innerHTML = filtered.map(row => `
        <tr>
            <td><code>${row.txnRef}</code></td>
            <td><span class="badge" style="color: ${row.type === 'CREDIT' ? 'var(--accent-green)' : 'var(--accent-red)'}">${row.type}</span></td>
            <td><strong>₹${row.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></td>
            <td>${row.description}</td>
            <td>₹${row.balanceAfter.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
            <td class="text-muted">${row.timestamp}</td>
        </tr>
    `).join('');
}

// Payment Terminal & Saga Visualizer
function initPaymentTerminal() {
    const form = document.getElementById('payment-form');
    const refreshIdempBtn = document.getElementById('btn-refresh-idemp');
    const idempInput = document.getElementById('pay-idempotency-key');
    const randomizeBtn = document.getElementById('btn-random-payment');

    function refreshIdempotency() {
        const key = 'idemp-' + Math.floor(100000 + Math.random() * 900000) + '-' + Date.now().toString().slice(-4);
        idempInput.value = key;
    }

    refreshIdempotency();
    refreshIdempBtn.addEventListener('click', refreshIdempotency);

    if (randomizeBtn) {
        randomizeBtn.addEventListener('click', () => {
            const randomAmount = Math.floor(1000 + Math.random() * 45000);
            document.getElementById('pay-amount').value = randomAmount;
            refreshIdempotency();
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fromAccount = document.getElementById('pay-from-account').value;
        const toAccount = document.getElementById('pay-to-account').value;
        const amount = parseFloat(document.getElementById('pay-amount').value);
        const mode = document.getElementById('pay-mode').value;
        const idempotencyKey = idempInput.value;
        const remarks = document.getElementById('pay-remarks').value;

        if (fromAccount === toAccount) {
            alert('Source and destination accounts cannot be identical.');
            return;
        }

        await runSagaExecution({
            fromAccountId: fromAccount,
            toAccountId: toAccount,
            amount: amount,
            mode: mode,
            idempotencyKey: idempotencyKey,
            remarks: remarks
        });

        refreshIdempotency();
    });
}

// Interactive Saga Execution
async function runSagaExecution(paymentData) {
    const stepper = document.getElementById('saga-stepper');
    const badge = document.getElementById('saga-status-badge');
    const resultBox = document.getElementById('saga-result-box');
    const resultTitle = document.getElementById('saga-result-title');
    const resultDesc = document.getElementById('saga-result-desc');
    const resultIcon = document.getElementById('saga-result-icon');
    const resultTxnId = document.getElementById('saga-result-txnid');

    const steps = [
        document.getElementById('step-1'),
        document.getElementById('step-2'),
        document.getElementById('step-3'),
        document.getElementById('step-4'),
        document.getElementById('step-5'),
        document.getElementById('step-6')
    ];

    // Reset steps
    steps.forEach(s => {
        s.className = 'step-node';
    });
    resultBox.style.display = 'none';
    resultBox.className = 'saga-result-card';
    badge.textContent = 'RUNNING SAGA';
    badge.className = 'badge badge-warning';

    const txnId = 'txn-' + Math.random().toString(36).substring(2, 10);
    const timeNow = new Date().toLocaleTimeString();

    // Step 1: Initiated & Idempotency Key validation
    steps[0].classList.add('active');
    logKafka('txn-events', `[TOPIC txn-events] TransactionInitiated { txnId: "${txnId}", amount: ${paymentData.amount}, mode: "${paymentData.mode}", idempotencyKey: "${paymentData.idempotencyKey}" }`);
    await sleep(600);
    steps[0].classList.remove('active');
    steps[0].classList.add('done');

    // Step 2: ML Fraud Scoring (XGBoost)
    steps[1].classList.add('active');
    // Calculate synthetic feature ratio & score
    const sender = state.accounts.find(a => a.id === paymentData.fromAccountId);
    const avg30Day = 15000;
    const ratio = paymentData.amount / avg30Day;
    let score = Math.min(0.98, Math.max(0.04, 0.08 + (ratio > 2 ? ratio * 0.16 : 0.04) + (paymentData.mode === 'UPI' && paymentData.amount > 80000 ? 0.35 : 0)));
    const isAmbiguous = (score >= 0.35 && score <= 0.75);

    updateFraudGauge(score);
    await sleep(700);
    steps[1].classList.remove('active');
    steps[1].classList.add('done');

    let llmReasoning = null;
    let isFraud = score > 0.75;

    // Step 3: Groq LLM Borderline Case Reasoning
    if (isAmbiguous || isFraud) {
        steps[2].classList.add('active');
        llmReasoning = `Transaction amount of ₹${paymentData.amount} via ${paymentData.mode} is ${ratio.toFixed(1)}x account 30-day average. ${ratio >= 3.0 ? 'Exceeds baseline threshold significantly. Flagged as suspected unauthorized takeover.' : 'Borderline deviation evaluated. Velocity profile is normal; approved under monitoring.'}`;
        
        displayGroqReasoning(llmReasoning, score);
        logKafka('fraud-alerts', `[GROQ LLM INFERENCE] Ambiguity band triggered (score=${score.toFixed(2)}). Reasoning: "${llmReasoning}"`);
        await sleep(900);
        steps[2].classList.remove('active');
        steps[2].classList.add('done');

        if (ratio >= 3.0) {
            isFraud = true;
        }
    } else {
        steps[2].classList.add('done');
        displayGroqReasoning(`Score ${score.toFixed(2)} is well within legitimate threshold (< 0.35). Auto-approved.`, score);
    }

    if (isFraud) {
        // FRAUD DETECTED -> REJECT / SAGA ABORT
        badge.textContent = 'BLOCKED (FRAUD)';
        badge.className = 'badge';
        badge.style.background = 'rgba(239, 68, 68, 0.2)';
        badge.style.color = 'var(--accent-red)';

        logKafka('fraud-alerts', `[TOPIC fraud-alerts] FraudAlertEvent { txnId: "${txnId}", score: ${score.toFixed(2)}, riskLevel: "HIGH", reason: "${llmReasoning}" }`);

        // Record alert
        addFraudAlert({
            alertId: 'alt-' + Math.floor(1000 + Math.random() * 9000),
            fromAccount: paymentData.fromAccountId,
            amount: paymentData.amount,
            score: score.toFixed(2),
            risk: 'HIGH',
            rule: ratio >= 3 ? '>3x 30-Day Avg' : 'Unusual Velocity'
        });

        // Trigger SMS Notification
        triggerNotification('SMS', '+919876543210', paymentData.fromAccountId, 
            'SECURITY ALERT', `Bankflow Security: Transfer of ₹${paymentData.amount} blocked due to high fraud risk (Score: ${score.toFixed(2)}). ${llmReasoning}`);

        resultBox.style.display = 'flex';
        resultBox.classList.add('rejected');
        resultIcon.innerHTML = '<i class="fa-solid fa-triangle-exclamation" style="font-size: 1.8rem; color: var(--accent-red);"></i>';
        resultTitle.textContent = 'Payment Blocked by AI Fraud Engine';
        resultDesc.innerHTML = `Transaction <code>${txnId}</code> rejected. Score: <strong>${score.toFixed(2)}</strong>. Reason: ${llmReasoning}`;
        return;
    }

    // Step 4: Account Service - Debit Source
    steps[3].classList.add('active');
    if (sender) {
        sender.balance -= paymentData.amount;
        state.ledger.unshift({
            txnRef: txnId,
            type: 'DEBIT',
            amount: paymentData.amount,
            description: `Transfer to ${paymentData.toAccountId} (${paymentData.mode})`,
            balanceAfter: sender.balance,
            timestamp: timeNow
        });
    }
    logKafka('balance-updates', `[TOPIC balance-updates] BalanceUpdated { accountId: "${paymentData.fromAccountId}", op: "DEBIT", delta: ${paymentData.amount}, newBal: ${sender ? sender.balance : 0} }`);
    await sleep(600);
    steps[3].classList.remove('active');
    steps[3].classList.add('done');

    // Step 5: Account Service - Credit Destination
    steps[4].classList.add('active');
    const recipient = state.accounts.find(a => a.id === paymentData.toAccountId);
    if (recipient) {
        recipient.balance += paymentData.amount;
        state.ledger.unshift({
            txnRef: txnId,
            type: 'CREDIT',
            amount: paymentData.amount,
            description: `Received from ${paymentData.fromAccountId} (${paymentData.mode})`,
            balanceAfter: recipient.balance,
            timestamp: timeNow
        });
    }
    logKafka('balance-updates', `[TOPIC balance-updates] BalanceUpdated { accountId: "${paymentData.toAccountId}", op: "CREDIT", delta: ${paymentData.amount}, newBal: ${recipient ? recipient.balance : 0} }`);
    logKafka('txn-approved', `[TOPIC txn-approved] TransactionApproved { txnId: "${txnId}", score: ${score.toFixed(2)}, status: "COMPLETED" }`);
    await sleep(600);
    steps[4].classList.remove('active');
    steps[4].classList.add('done');

    // Step 6: Multi-Channel Notification Dispatch
    steps[5].classList.add('active');
    triggerNotification('SMS', sender ? sender.phoneNumber : '+919876543210', paymentData.fromAccountId,
        'Payment Sent', `Bankflow Alert: ₹${paymentData.amount} sent successfully to ${paymentData.toAccountId} via ${paymentData.mode}. Txn ID: ${txnId}`);
    
    triggerNotification('PUSH', 'device', paymentData.fromAccountId,
        'Transfer Successful', `₹${paymentData.amount} debited.`);
    await sleep(500);
    steps[5].classList.remove('active');
    steps[5].classList.add('done');

    // Update accounts view
    renderAccounts();
    populateAccountSelects();

    // Final result
    badge.textContent = 'SAGA COMPLETED';
    badge.className = 'badge';
    badge.style.background = 'rgba(16, 185, 129, 0.2)';
    badge.style.color = 'var(--accent-green)';

    resultBox.style.display = 'flex';
    resultIcon.innerHTML = '<i class="fa-solid fa-circle-check" style="font-size: 1.8rem; color: var(--accent-green);"></i>';
    resultTitle.textContent = 'Saga Execution Succeeded';
    resultTxnId.textContent = txnId;
    resultDesc.innerHTML = `Transaction completed and committed atomically. Multi-channel receipts dispatched.`;
}

// Fraud Radar & Groq Display
function updateFraudGauge(score) {
    const fillBar = document.getElementById('gauge-fill-bar');
    const scoreVal = document.getElementById('gauge-score-val');
    const riskBadge = document.getElementById('gauge-risk-badge');

    scoreVal.textContent = score.toFixed(2);

    let angle = score * 180;
    fillBar.style.transform = `rotate(${angle - 180}deg)`;

    if (score >= 0.75) {
        riskBadge.textContent = 'CRITICAL FRAUD RISK';
        riskBadge.style.color = 'var(--accent-red)';
        riskBadge.style.background = 'rgba(239, 68, 68, 0.2)';
    } else if (score >= 0.35) {
        riskBadge.textContent = 'AMBIGUOUS (GROQ LLM)';
        riskBadge.style.color = 'var(--accent-amber)';
        riskBadge.style.background = 'rgba(245, 158, 11, 0.2)';
    } else {
        riskBadge.textContent = 'LOW RISK (APPROVED)';
        riskBadge.style.color = 'var(--accent-green)';
        riskBadge.style.background = 'rgba(16, 185, 129, 0.2)';
    }
}

function displayGroqReasoning(text, score) {
    const card = document.getElementById('groq-reasoning-card');
    const textField = document.getElementById('groq-reasoning-text');
    textField.textContent = `"${text}"`;
}

function addFraudAlert(alert) {
    state.fraudAlerts.unshift(alert);
    const badge = document.getElementById('alert-counter-badge');
    if (badge) badge.textContent = `${state.fraudAlerts.length} Alerts`;

    const tbody = document.getElementById('fraud-alerts-body');
    if (tbody) {
        tbody.innerHTML = state.fraudAlerts.map(a => `
            <tr>
                <td><code>${a.alertId}</code></td>
                <td>${a.fromAccount}</td>
                <td>₹${parseFloat(a.amount).toLocaleString('en-IN')}</td>
                <td><strong style="color: var(--accent-red);">${a.score}</strong></td>
                <td><span class="badge" style="color: var(--accent-red); background: rgba(239,68,68,0.15);">${a.risk}</span></td>
                <td>${a.rule}</td>
            </tr>
        `).join('');
    }
}

// Notifications & Phone Screen
function triggerNotification(channel, recipient, customerId, subject, body, simulatedFailure = false) {
    const smsFeed = document.getElementById('sms-feed-container');
    const notifFeedList = document.getElementById('notif-feed-list');
    const notifCountBadge = document.getElementById('notif-count');

    const notif = {
        id: 'notif-' + Math.floor(1000 + Math.random() * 9000),
        channel: channel,
        recipient: recipient,
        customerId: customerId,
        subject: subject,
        body: body,
        status: simulatedFailure ? 'RETRYING (Attempt 1/3)' : 'DELIVERED',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    state.notifications.unshift(notif);
    if (notifCountBadge) notifCountBadge.textContent = state.notifications.length;

    // Render SMS bubble on phone frame
    if (channel === 'SMS' && smsFeed) {
        const isAlert = subject.includes('ALERT') || subject.includes('Security');
        const bubble = document.createElement('div');
        bubble.className = `sms-bubble in ${isAlert ? 'alert' : ''}`;
        bubble.innerHTML = `
            <strong>${subject}</strong>
            <p>${body}</p>
            <span class="sms-time">${notif.time}</span>
        `;
        smsFeed.appendChild(bubble);
        smsFeed.scrollTop = smsFeed.scrollHeight;
    }

    // Render in notification center list
    if (notifFeedList) {
        const item = document.createElement('div');
        item.className = 'notif-item';
        item.innerHTML = `
            <div class="notif-channel-badge ${channel.toLowerCase()}">
                <i class="fa-solid ${channel === 'SMS' ? 'fa-comment-sms' : (channel === 'EMAIL' ? 'fa-envelope' : 'fa-bell')}"></i>
            </div>
            <div class="notif-body-box">
                <h5>${subject}</h5>
                <p>${body}</p>
                <div class="notif-meta">
                    <span><i class="fa-solid fa-user"></i> ${recipient}</span>
                    <span><i class="fa-solid fa-clock"></i> ${notif.time}</span>
                    <span style="color: ${simulatedFailure ? 'var(--accent-amber)' : 'var(--accent-green)'}">
                        <i class="fa-solid ${simulatedFailure ? 'fa-arrows-rotate' : 'fa-circle-check'}"></i> ${notif.status}
                    </span>
                </div>
            </div>
        `;
        notifFeedList.insertBefore(item, notifFeedList.firstChild);
    }
}

// Kafka Terminal Logger
function logKafka(topic, message) {
    const terminal = document.getElementById('kafka-terminal');
    if (!terminal) return;

    const time = new Date().toISOString().split('T')[1].slice(0, 8);
    let cssClass = 'event';
    if (topic === 'fraud-alerts') cssClass = 'alert';
    if (topic === 'GROQ' || message.includes('GROQ')) cssClass = 'groq';
    if (topic === 'SYSTEM') cssClass = 'info';

    const line = document.createElement('div');
    line.className = `term-line ${cssClass}`;
    line.innerHTML = `[${time}] ${message}`;
    terminal.appendChild(line);
    terminal.scrollTop = terminal.scrollHeight;

    // Update topic counter pills
    if (topic === 'txn-events') incrementCounter('cnt-txn-events');
    if (topic === 'fraud-alerts') incrementCounter('cnt-fraud-alerts');
    if (topic === 'txn-approved') incrementCounter('cnt-txn-approved');
}

function incrementCounter(id) {
    const el = document.getElementById(id);
    if (!el) return;
    const current = parseInt(el.textContent) || 0;
    el.textContent = `${current + 1} msgs`;
}

// Quick Presets
function initPresets() {
    const seedBtn = document.getElementById('btn-seed-data');
    const quickTransferBtn = document.getElementById('btn-quick-transfer');
    const fraudSimBtn = document.getElementById('btn-simulate-fraud');
    const testFailedSmsBtn = document.getElementById('btn-trigger-failed-sms');
    const clearKafkaBtn = document.getElementById('btn-clear-kafka');

    if (seedBtn) {
        seedBtn.addEventListener('click', () => {
            logKafka('SYSTEM', 'Seeded 3 active customer accounts with historical 30-day baselines');
            alert('Demo data refreshed: 3 accounts seeded with balance and transaction ledger.');
        });
    }

    if (quickTransferBtn) {
        quickTransferBtn.addEventListener('click', () => {
            document.querySelector('[data-tab="payments"]').click();
            document.getElementById('pay-amount').value = 4500;
            document.getElementById('pay-mode').value = 'UPI';
            document.getElementById('pay-remarks').value = 'Grocery & utilities transfer';
            document.getElementById('payment-form').dispatchEvent(new Event('submit'));
        });
    }

    if (fraudSimBtn) {
        fraudSimBtn.addEventListener('click', () => {
            document.querySelector('[data-tab="payments"]').click();
            document.getElementById('pay-amount').value = 92000;
            document.getElementById('pay-mode').value = 'UPI';
            document.getElementById('pay-remarks').value = 'Nocturnal sudden burst transfer';
            document.getElementById('payment-form').dispatchEvent(new Event('submit'));
        });
    }

    if (testFailedSmsBtn) {
        testFailedSmsBtn.addEventListener('click', () => {
            triggerNotification('SMS', '+919999999999', 'cust-test', 'Twilio Carrier Glitch', 'Simulated failure: carrier timeout.', true);
            setTimeout(() => {
                logKafka('notification-queue', '[SPRING RETRY] Exponential backoff triggered: attempt 2 of 3 (delay=2000ms)...');
            }, 1000);
            setTimeout(() => {
                logKafka('notification-queue', '[SPRING RETRY] Final attempt succeeded via secondary gateway! Status: DELIVERED.');
            }, 3000);
        });
    }

    if (clearKafkaBtn) {
        clearKafkaBtn.addEventListener('click', () => {
            const terminal = document.getElementById('kafka-terminal');
            if (terminal) terminal.innerHTML = '<div class="term-line info">[KAFKA EVENT BUS] Stream cleared. Connected to broker.</div>';
        });
    }
}

// Modal handling
function initModal() {
    const modal = document.getElementById('create-account-modal');
    const openBtn = document.getElementById('btn-open-create-account');
    const closeBtn = document.getElementById('modal-close-btn');
    const cancelBtn = document.getElementById('modal-cancel-btn');
    const form = document.getElementById('create-account-form');

    if (openBtn) openBtn.addEventListener('click', () => modal.classList.add('open'));
    if (closeBtn) closeBtn.addEventListener('click', () => modal.classList.remove('open'));
    if (cancelBtn) cancelBtn.addEventListener('click', () => modal.classList.remove('open'));

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('new-cust-name').value;
            const email = document.getElementById('new-cust-email').value;
            const phone = document.getElementById('new-cust-phone').value;
            const currency = document.getElementById('new-cust-currency').value;
            const deposit = parseFloat(document.getElementById('new-cust-deposit').value);

            const newAcc = {
                id: 'acc-' + Math.floor(100 + Math.random() * 900),
                accountNumber: Math.floor(1000000000 + Math.random() * 9000000000).toString(),
                customerId: 'cust-' + Math.floor(100 + Math.random() * 900),
                customerName: name,
                email: email,
                phoneNumber: phone,
                currency: currency,
                balance: deposit,
                status: 'ACTIVE'
            };

            state.accounts.push(newAcc);
            state.ledger.unshift({
                txnRef: 'txn-init-' + newAcc.id,
                type: 'CREDIT',
                amount: deposit,
                description: 'Account Opening Deposit',
                balanceAfter: deposit,
                timestamp: new Date().toLocaleTimeString()
            });

            logKafka('account-events', `[TOPIC account-events] AccountCreatedEvent { accountId: "${newAcc.id}", customer: "${name}", initialDeposit: ${deposit} }`);

            renderAccounts();
            populateAccountSelects();
            modal.classList.remove('open');
            form.reset();
            alert(`Account created successfully for ${name}! Account Number: ${newAcc.accountNumber}`);
        });
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
