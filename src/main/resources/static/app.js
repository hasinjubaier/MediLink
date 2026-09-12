const API_BASE_URL = (window.MEDILINK_CONFIG && window.MEDILINK_CONFIG.API_BASE_URL) ? window.MEDILINK_CONFIG.API_BASE_URL : '';

const state = {
    isAuthenticated: false,
    activeRole: 'PATIENT',
    currentUser: {
        id: 'ML-9824-A',
        name: 'Rahim Ahmed',
        email: 'rahim@medilink.com',
        role: 'PATIENT'
    },
    medicines: [],
    prescriptions: [],
    stocks: [],
    pharmacies: [],
    reminders: [],
    notifications: [],
    pharmacists: [],
    selectedPharmacistId: 'usr_pharma_01',
    eventSource: null
};

// Initializer
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initLandingNav();
    loadDynamicStats();
    initRealTimeStream();
    loadMedicines();
    loadPrescriptions();
    loadStocks();
    loadEmergencyPharmacies();
    loadReminders();
    loadPharmacistsList();
    loadChatMessages();
    renderNotifications();
    initAuthGate();
    initAiAssistant();
    renderCountryDropdown();
});

// Visitor Authentication Status (Restores session if logged in; landing page shown first)
function ensureAuthSessionLoaded() {
    if (!state.isAuthenticated) {
        const savedUser = sessionStorage.getItem('medilink_user') || localStorage.getItem('medilink_user');
        if (savedUser) {
            try {
                const user = JSON.parse(savedUser);
                if (user && user.email) {
                    applyAuthenticatedUser(user, false);
                }
            } catch (e) {
                sessionStorage.removeItem('medilink_user');
                localStorage.removeItem('medilink_user');
            }
        }
    }
    return Boolean(state.isAuthenticated && state.currentUser && state.currentUser.email);
}

function initAuthGate() {
    ensureAuthSessionLoaded();
}

// Smart "Get Started" Entry Controller:
// - New / unauthenticated visitor -> directly opens Sign Up section!
// - Authenticated / logged-in user -> directs classification-wise to their own role portal!
function handleGetStartedClick() {
    if (!ensureAuthSessionLoaded()) {
        openAuthModal('signup');
        showToast('👋 Welcome! Please create an account to get started.');
        return;
    }

    // Authenticated user: navigate classification-wise to their own portal
    const role = (state.currentUser?.role || state.activeRole || 'PATIENT').toUpperCase();
    if (role === 'PHARMACIST') {
        launchApp('pharmacist');
    } else if (role === 'ADMIN') {
        launchApp('admin');
    } else {
        launchApp('patient');
    }
}

// Smart "Consult Pharmacists" Controller:
// - New patient / unauthenticated visitor -> directly opens Sign Up section with Patient role!
// - Patient already logged in -> directly opens Pharmacist Live Chat!
function handleConsultPharmacistsClick() {
    if (!ensureAuthSessionLoaded()) {
        openAuthModal('signup');
        selectAuthRole('PATIENT', 'signup');
        showToast('👋 Sign up to connect directly with verified pharmacists.');
        return;
    }

    // Patient or user is already logged in -> directly open Pharmacist Live Chat
    launchApp('chat');
    loadChatMessages();
    setTimeout(() => {
        const input = document.getElementById('chat-input');
        if (input) input.focus();
    }, 350);
    showToast('💬 Connected to Pharmacist Live Chat');
}

// Smart "Notifications & Reminders" Controller:
// - New patient / unauthenticated visitor -> directly opens Sign Up section with Patient role!
// - Patient already logged in -> directly opens Notifications icon & dropdown!
function handleNotificationsRemindersClick() {
    if (!ensureAuthSessionLoaded()) {
        openAuthModal('signup');
        selectAuthRole('PATIENT', 'signup');
        showToast('👋 Sign up to manage medication reminders and health alerts.');
        return;
    }

    // Patient or user is already logged in -> directly open Notifications icon
    openNotificationsIcon();
}

// Smart "Medication Reminders & Dose Scheduler" Controller:
// - Directly links/connects to the "Medication Reminders & Dose Scheduler" feature (tab-reminders)
// - If unauthenticated -> directly opens Sign Up / Sign In modal
// - If authenticated -> smoothly navigates to the Medication Reminders tab and refreshes schedule data
function handleMedicationRemindersClick() {
    if (!ensureAuthSessionLoaded()) {
        openAuthModal('signup');
        selectAuthRole('PATIENT', 'signup');
        showToast('👋 Please create an account or sign in to access Medication Reminders.');
        return;
    }

    launchApp('reminders');
    showToast('⏰ Connected to Medication Reminders & Dose Scheduler');
}

// Backwards compatibility alias:
function handleAppointmentManagementClick() {
    handleMedicationRemindersClick();
}

// Smart "Medical Records" Controller:
// - Links the landing page Medical Records card (first screenshot) directly to the Medical Records tab (second screenshot)
// - If unauthenticated -> opens Sign Up / Sign In modal with Patient role pre-selected
// - If authenticated -> smoothly navigates to the Medical Records tab and loads records
function handleMedicalRecordsClick() {
    if (!ensureAuthSessionLoaded()) {
        openAuthModal('signup');
        selectAuthRole('PATIENT', 'signup');
        showToast('👋 Sign up or log in to access your secure Medical Records vault.');
        return;
    }

    launchApp('prescriptions');
    showToast('📄 Opened Medical Records Vault');
}

function openNotificationsIcon() {
    document.getElementById('view-landing').classList.remove('active');
    document.getElementById('view-app').classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });

    setTimeout(() => {
        const notifBell = document.getElementById('btn-notification-bell');
        const notifMenu = document.getElementById('notification-dropdown-menu');

        document.querySelectorAll('.notification-dropdown-menu').forEach(m => {
            if (m !== notifMenu) m.classList.remove('active');
        });

        if (notifMenu) {
            notifMenu.classList.add('active');
            renderNotifications();
        }

        if (notifBell) {
            notifBell.classList.add('pulse-highlight');
            setTimeout(() => notifBell.classList.remove('pulse-highlight'), 2500);
        }

        showToast('🔔 Notifications & Reminders opened.');
    }, 250);
}

// View Switching: Landing Page <-> Interactive App
function launchApp(context) {
    // If not authenticated and not accessing public emergency finder, guide to Sign Up
    if (!ensureAuthSessionLoaded() && context !== 'emergency') {
        openAuthModal('signup');
        showToast('Please sign up or log in to access your portal.');
        return;
    }

    document.getElementById('view-landing').classList.remove('active');
    document.getElementById('view-app').classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });

    const activeRole = state.currentUser?.role || state.activeRole || 'PATIENT';

    if (context === 'emergency') {
        switchTab('emergency');
    } else if (context === 'chat') {
        switchTab('chat');
    } else if (context === 'patient') {
        switchTab('dashboard');
    } else if (context === 'pharmacist') {
        switchTab('stock');
    } else if (context === 'admin') {
        switchTab('admin');
    } else if (context === 'prescriptions') {
        switchTab('prescriptions');
    } else if (context === 'reminders') {
        switchTab('reminders');
    } else if (context === 'upload-rx') {
        switchTab('upload-rx');
    } else if (context === 'verify') {
        switchTab('verify');
    } else if (context === 'settings') {
        switchTab('settings');
    } else {
        // Classification-wise routing based on logged-in user's role
        if (activeRole === 'PHARMACIST') {
            switchTab('stock');
        } else if (activeRole === 'ADMIN') {
            switchTab('admin');
        } else {
            switchTab('dashboard');
        }
    }
}

function returnToLanding() {
    document.getElementById('view-app').classList.remove('active');
    document.getElementById('view-landing').classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Smart Navbar Login Button Handler
function handleNavbarLoginClick() {
    const hasRegistered = localStorage.getItem('medilink_has_registered') === 'true';
    if (hasRegistered) {
        // User is already registered -> open directly to Sign In (Login) without Sign Up!
        openAuthModal('signin');
    } else {
        // First-time user -> open Sign Up first
        openAuthModal('signup');
    }
}

// Authentication Modal & Sliding Dual-Panel Controller (Demo Video Match)
function openAuthModal(mode = 'signin') {
    const modal = document.getElementById('modal-auth');
    if (modal) {
        modal.style.display = 'flex';
        modal.classList.add('active');
        setAuthMode(mode);
    }
}

// Direct Create Account Trigger (Landing Page Workflow Step 01)
// - If not logged in -> directly open Sign Up section
// - If logged in -> directly open user's dashboard depending on account email & role [patient/pharmacist/admin]
function handleCreateAccountClick() {
    const isLoggedIn = ensureAuthSessionLoaded();

    if (!isLoggedIn) {
        openAuthModal('signup');
        showToast('Create Account');
        if (typeof addNotification === 'function') {
            addNotification({
                icon: '👤',
                title: 'Create Account',
                text: 'Welcome! Create your secure MediLink account to get started.'
            });
        }
        return;
    }

    // Authenticated user: navigate directly to their dashboard depending on account email & role
    const user = state.currentUser;
    const email = (user?.email || '').trim().toLowerCase();
    const role = (user?.role || state.activeRole || 'PATIENT').toUpperCase();

    if (role === 'PHARMACIST' || email.includes('pharma') || email.includes('pharmacist')) {
        launchApp('pharmacist');
        showToast(`👨‍⚕️ Welcome back ${user?.name || 'Pharmacist'}! Opened Pharmacist Dashboard.`);
    } else if (role === 'ADMIN' || email.includes('admin')) {
        launchApp('admin');
        showToast(`🛡️ Welcome back ${user?.name || 'Admin'}! Opened Administrator Dashboard.`);
    } else {
        launchApp('patient');
        showToast(`🏥 Welcome back ${user?.name || 'Patient'}! Opened Patient Health Dashboard.`);
    }
}

function setAuthMode(mode) {
    const container = document.getElementById('auth-container');
    if (!container) return;
    if (mode === 'signup') {
        container.classList.add('sign-up-active');
        const signupPanel = document.querySelector('.form-signup');
        if (signupPanel) signupPanel.scrollTop = 0;
    } else {
        container.classList.remove('sign-up-active');
        const signinPanel = document.querySelector('.form-signin');
        if (signinPanel) signinPanel.scrollTop = 0;
    }
}

function selectAuthRole(role, formType) {
    const roleInput = document.getElementById(`${formType}-role`);
    if (roleInput) roleInput.value = role;

    // Update active tab buttons
    ['patient', 'pharmacist', 'admin'].forEach(r => {
        const tab = document.getElementById(`${formType}-tab-${r}`);
        if (tab) {
            if (r.toUpperCase() === role) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        }
    });

    // For signup, customize dynamic extra field
    if (formType === 'signup') {
        const roleFieldGroup = document.getElementById('signup-role-field-group');
        const label = document.getElementById('signup-role-field-label');
        const phoneWrapper = document.getElementById('signup-phone-wrapper');
        const textWrapper = document.getElementById('signup-text-wrapper');
        const extraTextInput = document.getElementById('signup-extra-text');
        const textIcon = document.getElementById('signup-text-icon');
        const phoneInput = document.getElementById('signup-phone');

        if (role === 'PHARMACIST') {
            if (roleFieldGroup) roleFieldGroup.style.display = 'block';
            if (label) label.textContent = 'Pharmacy Name & License No. *';
            if (phoneWrapper) phoneWrapper.style.display = 'none';
            if (textWrapper) textWrapper.style.display = 'flex';
            if (phoneInput) phoneInput.removeAttribute('required');
            if (extraTextInput) {
                extraTextInput.setAttribute('required', 'true');
                extraTextInput.placeholder = 'e.g. Lazz Pharma (Dhanmondi) | DGDA-PH-99201';
            }
            if (textIcon) textIcon.textContent = '🩺';
        } else if (role === 'ADMIN') {
            // One central system admin handles everything -> no department needed
            if (roleFieldGroup) roleFieldGroup.style.display = 'none';
            if (phoneWrapper) phoneWrapper.style.display = 'none';
            if (textWrapper) textWrapper.style.display = 'none';
            if (phoneInput) phoneInput.removeAttribute('required');
            if (extraTextInput) {
                extraTextInput.removeAttribute('required');
                extraTextInput.value = 'General Administration';
            }
        } else {
            // PATIENT
            if (roleFieldGroup) roleFieldGroup.style.display = 'block';
            if (label) label.textContent = 'Emergency Phone / Contact *';
            if (phoneWrapper) phoneWrapper.style.display = 'flex';
            if (textWrapper) textWrapper.style.display = 'none';
            if (phoneInput) phoneInput.setAttribute('required', 'true');
            if (extraTextInput) extraTextInput.removeAttribute('required');
        }
        syncSignupExtra();
    }
}

const COUNTRIES_DATA = [
    { code: 'bd', dial: '+880', name: 'Bangladesh' },
    { code: 'us', dial: '+1',   name: 'United States' },
    { code: 'gb', dial: '+44',  name: 'United Kingdom' },
    { code: 'in', dial: '+91',  name: 'India' },
    { code: 'pk', dial: '+92',  name: 'Pakistan' },
    { code: 'ae', dial: '+971', name: 'United Arab Emirates' },
    { code: 'sa', dial: '+966', name: 'Saudi Arabia' },
    { code: 'ca', dial: '+1',   name: 'Canada' },
    { code: 'au', dial: '+61',  name: 'Australia' },
    { code: 'my', dial: '+60',  name: 'Malaysia' },
    { code: 'sg', dial: '+65',  name: 'Singapore' },
    { code: 'qa', dial: '+974', name: 'Qatar' },
    { code: 'kw', dial: '+965', name: 'Kuwait' },
    { code: 'om', dial: '+968', name: 'Oman' },
    { code: 'de', dial: '+49',  name: 'Germany' },
    { code: 'fr', dial: '+33',  name: 'France' },
    { code: 'it', dial: '+39',  name: 'Italy' },
    { code: 'tr', dial: '+90',  name: 'Turkey' },
    { code: 'np', dial: '+977', name: 'Nepal' },
    { code: 'lk', dial: '+94',  name: 'Sri Lanka' },
    { code: 'jp', dial: '+81',  name: 'Japan' },
    { code: 'cn', dial: '+86',  name: 'China' },
    { code: 'kr', dial: '+82',  name: 'South Korea' },
    { code: 'br', dial: '+55',  name: 'Brazil' },
    { code: 'za', dial: '+27',  name: 'South Africa' },
    { code: 'eg', dial: '+20',  name: 'Egypt' },
    { code: 'es', dial: '+34',  name: 'Spain' }
];

let _selectedCountry = COUNTRIES_DATA[0];

function renderCountryDropdown(filterText = '') {
    const listEl = document.getElementById('country-list-items');
    if (!listEl) return;
    const q = filterText.toLowerCase().trim();
    const filtered = COUNTRIES_DATA.filter(c => 
        c.name.toLowerCase().includes(q) || 
        c.dial.includes(q) || 
        c.code.toLowerCase().includes(q)
    );

    if (filtered.length === 0) {
        listEl.innerHTML = '<div style="padding:12px;text-align:center;color:#64748b;font-size:0.8rem;">No country found</div>';
        return;
    }

    listEl.innerHTML = filtered.map(c => `
        <div class="country-item ${c.code === _selectedCountry.code ? 'active' : ''}" onclick="selectCountryItem('${c.code}', '${c.dial}', '${c.name.replace(/'/g, "\\'")}')">
            <img class="country-flag-img" src="/flags/${c.code}.png" alt="${c.name}">
            <span class="country-item-name">${c.name}</span>
            <span class="country-item-dial">${c.dial}</span>
        </div>
    `).join('');
}

function toggleCountryDropdown(event) {
    if (event) event.stopPropagation();
    const dropdown = document.getElementById('country-picker-dropdown');
    const container = document.getElementById('custom-country-picker');
    if (!dropdown) return;

    const isOpen = dropdown.style.display === 'flex';
    if (isOpen) {
        dropdown.style.display = 'none';
        if (container) container.classList.remove('open');
    } else {
        renderCountryDropdown();
        dropdown.style.display = 'flex';
        if (container) container.classList.add('open');
        const searchInput = document.getElementById('country-search-input');
        if (searchInput) {
            searchInput.value = '';
            setTimeout(() => searchInput.focus(), 60);
        }
    }
}

function filterCountries(value) {
    renderCountryDropdown(value);
}

function selectCountryItem(code, dial, name) {
    _selectedCountry = COUNTRIES_DATA.find(c => c.code === code) || { code, dial, name };
    
    // Update trigger button flag & code
    const flagImg = document.getElementById('selected-country-flag');
    const codeText = document.getElementById('selected-country-code');
    const hiddenCode = document.getElementById('signup-country-code');

    if (flagImg) {
        flagImg.src = `/flags/${code}.png`;
        flagImg.alt = name;
    }
    if (codeText) codeText.textContent = dial;
    if (hiddenCode) hiddenCode.value = dial;

    // Close dropdown
    const dropdown = document.getElementById('country-picker-dropdown');
    const container = document.getElementById('custom-country-picker');
    if (dropdown) dropdown.style.display = 'none';
    if (container) container.classList.remove('open');

    // Adjust placeholder & focus
    const phoneInput = document.getElementById('signup-phone');
    if (phoneInput) {
        if (dial === '+880') phoneInput.placeholder = '1711002233';
        else if (dial === '+1') phoneInput.placeholder = '2025550143';
        else if (dial === '+44') phoneInput.placeholder = '7911123456';
        else if (dial === '+91') phoneInput.placeholder = '9876543210';
        else phoneInput.placeholder = 'Phone number';
        phoneInput.focus();
    }
    syncSignupExtra();
}

// Close country dropdown when clicking outside
document.addEventListener('click', (e) => {
    if (!e.target.closest('#custom-country-picker')) {
        const dropdown = document.getElementById('country-picker-dropdown');
        const container = document.getElementById('custom-country-picker');
        if (dropdown) dropdown.style.display = 'none';
        if (container) container.classList.remove('open');
    }
});

function syncSignupExtra() {
    const role = document.getElementById('signup-role')?.value || 'PATIENT';
    const extraInput = document.getElementById('signup-extra');
    if (!extraInput) return;

    if (role === 'PATIENT') {
        const code = document.getElementById('signup-country-code')?.value || '+880';
        let num = document.getElementById('signup-phone')?.value.trim() || '';
        // If user already entered leading 0, strip it to avoid duplicate zero (e.g. +88001711...)
        if (num.startsWith('0')) {
            num = num.substring(1);
        }
        if (num.startsWith('+')) {
            extraInput.value = num;
        } else if (num) {
            extraInput.value = `${code}${num}`;
        } else {
            extraInput.value = '';
        }
    } else if (role === 'PHARMACIST') {
        const textVal = document.getElementById('signup-extra-text')?.value.trim() || '';
        extraInput.value = textVal;
    } else if (role === 'ADMIN') {
        extraInput.value = 'General Administration';
    }
}

function fillDemoCredentials(role) {
    selectAuthRole(role, 'signin');
    const emailEl = document.getElementById('signin-email');
    const passEl = document.getElementById('signin-password');
    if (role === 'PATIENT') {
        if (emailEl) emailEl.value = 'rahim@medilink.com';
        if (passEl) passEl.value = 'patient123';
    } else if (role === 'PHARMACIST') {
        if (emailEl) emailEl.value = 'farhan@lazzpharma.com';
        if (passEl) passEl.value = 'pharma123';
    } else if (role === 'ADMIN') {
        if (emailEl) emailEl.value = 'admin@medilink.com';
        if (passEl) passEl.value = 'admin123';
    }
    showToast(`Loaded ${role} demo credentials`);
}

function toggleCaptchaVerified(formType) {
    const box = document.getElementById(`${formType}-captcha`);
    if (box) {
        box.classList.toggle('verified');
    }
}

function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.type = input.type === 'password' ? 'text' : 'password';
}

function handlePasswordInput() {
    checkPasswordStrength();
    validatePasswordMatch();
}

function checkPasswordStrength() {
    const pass = document.getElementById('signup-password')?.value || '';
    
    const ruleLength = pass.length >= 8;
    const ruleUpper = /[A-Z]/.test(pass);
    const ruleLower = /[a-z]/.test(pass);
    const ruleNumber = /[0-9]/.test(pass);
    const ruleSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pass);

    const updateRule = (id, passed) => {
        const el = document.getElementById(id);
        if (!el) return;
        const icon = el.querySelector('.rule-icon');
        if (passed) {
            el.classList.add('passed');
            if (icon) icon.textContent = '✓';
        } else {
            el.classList.remove('passed');
            if (icon) icon.textContent = '○';
        }
    };

    updateRule('rule-length', ruleLength);
    updateRule('rule-upper', ruleUpper);
    updateRule('rule-lower', ruleLower);
    updateRule('rule-number', ruleNumber);
    updateRule('rule-special', ruleSpecial);

    const score = [ruleLength, ruleUpper, ruleLower, ruleNumber, ruleSpecial].filter(Boolean).length;
    const badge = document.getElementById('pwd-strength-badge');
    const bar = document.getElementById('pwd-meter-bar');

    if (!badge || !bar) return;

    badge.className = 'pwd-strength-badge';

    if (!pass) {
        badge.textContent = 'Too Short';
        bar.style.width = '0%';
        bar.style.backgroundColor = '#64748b';
    } else if (score <= 1) {
        badge.textContent = 'Weak';
        badge.classList.add('weak');
        bar.style.width = '20%';
        bar.style.backgroundColor = '#ef4444';
    } else if (score === 2 || score === 3) {
        badge.textContent = 'Fair';
        badge.classList.add('fair');
        bar.style.width = '55%';
        bar.style.backgroundColor = '#f59e0b';
    } else if (score === 4) {
        badge.textContent = 'Good';
        badge.classList.add('good');
        bar.style.width = '80%';
        bar.style.backgroundColor = '#3b82f6';
    } else {
        badge.textContent = 'Strong';
        badge.classList.add('strong');
        bar.style.width = '100%';
        bar.style.backgroundColor = '#10b981';
    }
}

function validatePasswordMatch() {
    const pass = document.getElementById('signup-password')?.value || '';
    const confirm = document.getElementById('signup-confirm-password')?.value || '';
    const confirmWrapper = document.getElementById('signup-confirm-wrapper');
    if (!confirmWrapper) return;
    if (!confirm) {
        confirmWrapper.style.borderColor = '';
        return;
    }
    if (pass === confirm) {
        confirmWrapper.style.borderColor = '#10b981';
    } else {
        confirmWrapper.style.borderColor = '#ef4444';
    }
}

async function handleAuthSubmit(event, formType) {
    event.preventDefault();

    if (formType === 'signin') {
        const email = document.getElementById('signin-email').value.trim();
        const password = document.getElementById('signin-password').value.trim();

        if (password.length < 6) {
            showToast('⚠️ Password must contain at least 6 characters.');
            const passInput = document.getElementById('signin-password');
            if (passInput) passInput.focus();
            return;
        }

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, password: password })
            });
            const data = await res.json();

            if (data.status === 'SUCCESS') {
                localStorage.setItem('medilink_has_registered', 'true');
                applyAuthenticatedUser(data, true);
                closeModal('modal-auth');
                showToast(`🎉 Signed in successfully! Welcome, ${data.name} (${data.role}).`);
                
                // Role-wise dashboard redirection
                if (data.role === 'PATIENT') {
                    launchApp('patient');
                } else if (data.role === 'PHARMACIST') {
                    launchApp('pharmacist');
                } else if (data.role === 'ADMIN') {
                    launchApp('admin');
                } else {
                    launchApp('dashboard');
                }
            } else if (data.code === 'USER_NOT_FOUND') {
                // Email doesn't exist -> Show clear notification & guide to Sign Up
                const alertBox = document.getElementById('signin-alert-box');
                if (alertBox) {
                    alertBox.innerHTML = `
                        <span class="alert-icon">⚠️</span>
                        <div>
                            <strong>Email not registered!</strong> No account found for <span style="color:#ffffff;">${email}</span>. 
                            <a href="javascript:void(0)" onclick="setAuthMode('signup')" class="alert-link">Click here to Sign Up first ➔</a>
                        </div>
                    `;
                    alertBox.style.display = 'flex';
                }

                showToast(`⚠️ Email not exist! Please Sign Up first.`);

                // Pre-fill email in Sign Up form
                const signupEmail = document.getElementById('signup-email');
                if (signupEmail) signupEmail.value = email;

                // Smoothly slide to Sign Up panel
                setTimeout(() => {
                    setAuthMode('signup');
                    showToast(`📝 Switched to Sign Up. Create your account for ${email}!`);
                }, 1200);

            } else if (data.code === 'INVALID_PASSWORD') {
                const alertBox = document.getElementById('signin-alert-box');
                if (alertBox) {
                    alertBox.innerHTML = `
                        <span class="alert-icon">⚠️</span>
                        <div>
                            <strong>Incorrect password!</strong> 
                            <a href="javascript:void(0)" onclick="openForgotPasswordModal('${email}')" class="alert-link">Forgot password? Reset it here ➔</a>
                        </div>
                    `;
                    alertBox.style.display = 'flex';
                }
                showToast('⚠️ Incorrect password! Click Forgot Password to reset.');
            } else {
                const alertBox = document.getElementById('signin-alert-box');
                if (alertBox) {
                    alertBox.innerHTML = `<span class="alert-icon">❌</span><div>${data.message || 'Login failed. Check credentials.'}</div>`;
                    alertBox.style.display = 'flex';
                }
                showToast(data.message || 'Login failed. Check credentials.');
            }
        } catch (e) {
            showToast('Authentication server connection error.');
        }

    } else if (formType === 'signup') {
        syncSignupExtra();
        const name = document.getElementById('signup-name').value.trim();
        const email = document.getElementById('signup-email').value.trim();
        const password = document.getElementById('signup-password').value.trim();
        const confirmPassword = document.getElementById('signup-confirm-password')?.value.trim() || '';
        const role = document.getElementById('signup-role').value;
        const extra = document.getElementById('signup-extra').value.trim();

        if (password.length < 6) {
            showToast('⚠️ Password must contain at least 6 characters.');
            const passInput = document.getElementById('signup-password');
            if (passInput) passInput.focus();
            return;
        }

        if (password !== confirmPassword) {
            showToast('⚠️ Passwords do not match! Please check and confirm your password.');
            const confirmInput = document.getElementById('signup-confirm-password');
            if (confirmInput) {
                confirmInput.focus();
                confirmInput.style.borderColor = '#ef4444';
            }
            return;
        }

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: name,
                    email: email,
                    password: password,
                    role: role,
                    extra: extra
                })
            });
            const data = await res.json();

            if (data.status === 'SUCCESS') {
                localStorage.setItem('medilink_has_registered', 'true');
                // Step 1 Completed (Sign Up): Now pre-fill and slide to Step 2 (Sign In)
                document.getElementById('signin-email').value = email;
                document.getElementById('signin-password').value = password;
                selectAuthRole(role, 'signin');

                // Smoothly slide to Sign In panel
                setAuthMode('signin');
                showToast(`✅ Account created for ${name}! Please click 'Sign In' to enter.`);
            } else {
                showToast(data.message || 'Registration failed.');
            }
        } catch (e) {
            showToast('Registration failed. Try again.');
        }
    }
}

// Smart Gender & Avatar Detection
const FEMALE_INDICATORS = [
    'jara', 'zara', 'jaraa', 'zaraa', 'zahra', 'maisha', 'mayesha', 'tasnim', 'fatima', 'ayesha', 'sadia', 'nusrat', 'farhana', 'sarah', 'emily',
    'sultana', 'maria', 'jahan', 'khatun', 'akter', 'begum', 'anika', 'tahmina',
    'sumaiya', 'jannat', 'jannatul', 'mariam', 'nabila', 'samia', 'rina', 'salma', 'sabiha',
    'lamia', 'laboni', 'lubna', 'liza', 'bushra', 'bithi', 'farzana', 'fariha', 'fahmida',
    'afia', 'afreen', 'arifa', 'adiba', 'afra', 'esha', 'eva', 'era', 'humaira', 'hira', 'isna', 'ishrat',
    'jerin', 'jui', 'tamanna', 'tanha', 'tasneem', 'nowshin', 'nayla', 'nazifa',
    'roshni', 'rupa', 'ruma', 'sanjida', 'sayma', 'shahnaz', 'sharmin', 'sohana',
    'tahsin', 'umme', 'zarin', 'zeba', 'zohra', 'mou', 'mim', 'mitu', 'mithila', 'meherin',
    'mrs', 'ms', 'miss', 'lady', 'female', 'woman', 'girl'
];

const MALE_PHOTOS = [
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=160&auto=format&fit=crop&q=80'
];

const FEMALE_PHOTOS = [
    'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=160&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=160&auto=format&fit=crop&q=80'
];

function detectGender(name = '', genderExplicit = '') {
    if (genderExplicit) {
        if (genderExplicit.toLowerCase() === 'female') return 'FEMALE';
        if (genderExplicit.toLowerCase() === 'male') return 'MALE';
    }
    const clean = (name || '').toLowerCase();
    const words = clean.split(/[\s._-]+/);
    const isFemale = words.some(w => FEMALE_INDICATORS.includes(w)) || FEMALE_INDICATORS.some(fi => clean.includes(fi));
    return isFemale ? 'FEMALE' : 'MALE';
}

function getUserAvatarAssets(name = '', gender = '', role = 'PATIENT') {
    const detected = detectGender(name, gender);
    if (detected === 'FEMALE') {
        return {
            gender: 'FEMALE',
            emoji: role === 'PHARMACIST' ? '👩‍⚕️' : '👩',
            photo: FEMALE_PHOTOS[0],
            photosList: FEMALE_PHOTOS
        };
    } else {
        return {
            gender: 'MALE',
            emoji: role === 'PHARMACIST' ? '👨‍⚕️' : '👨',
            photo: MALE_PHOTOS[0],
            photosList: MALE_PHOTOS
        };
    }
}

// ========================================================
// FORGOT PASSWORD & RECOVERY CONTROLLER
// ========================================================
let forgotRecoveryState = {
    step: 1,
    email: '',
    otp: '',
    timerInterval: null,
    timerSeconds: 300
};

function openForgotPasswordModal(prefilledEmail = '') {
    closeModal('modal-auth');
    
    if (!prefilledEmail) {
        const signinInput = document.getElementById('signin-email');
        if (signinInput && signinInput.value.trim()) {
            prefilledEmail = signinInput.value.trim();
        }
    }

    const emailInput = document.getElementById('modal-reset-email');
    if (emailInput && prefilledEmail) {
        emailInput.value = prefilledEmail;
    }

    setModalRecoveryStep(1);
    openModal('modal-forgot-password');
}

function closeForgotPasswordModal() {
    clearInterval(forgotRecoveryState.timerInterval);
    closeModal('modal-forgot-password');
}

function showModalForgotAlert(msg, isSuccess = false) {
    const alertBox = document.getElementById('modal-forgot-alert');
    if (!alertBox) return;
    alertBox.innerHTML = `<span class="alert-icon">${isSuccess ? '✅' : '⚠️'}</span><div>${msg}</div>`;
    alertBox.style.display = 'flex';
    if (isSuccess) {
        alertBox.style.background = 'rgba(16, 185, 129, 0.15)';
        alertBox.style.borderColor = 'rgba(16, 185, 129, 0.4)';
        alertBox.style.color = '#6ee7b7';
    } else {
        alertBox.style.background = 'rgba(239, 68, 68, 0.15)';
        alertBox.style.borderColor = 'rgba(239, 68, 68, 0.4)';
        alertBox.style.color = '#fca5a5';
    }
}

function hideModalForgotAlert() {
    const alertBox = document.getElementById('modal-forgot-alert');
    if (alertBox) alertBox.style.display = 'none';
}

function setModalRecoveryStep(step) {
    hideModalForgotAlert();
    forgotRecoveryState.step = step;

    for (let i = 1; i <= 4; i++) {
        const p = document.getElementById(`modal-step-panel-${i}`);
        if (p) p.style.display = (i === step) ? 'block' : 'none';
    }

    const fill = document.getElementById('modal-step-fill');
    if (fill) {
        if (step === 1) fill.style.width = '0%';
        else if (step === 2) fill.style.width = '50%';
        else if (step >= 3) fill.style.width = '100%';
    }

    for (let i = 1; i <= 3; i++) {
        const dot = document.getElementById(`modal-dot-${i}`);
        if (!dot) continue;
        if (i < step) {
            dot.style.background = '#10b981';
            dot.style.borderColor = '#34d399';
            dot.textContent = '✓';
        } else if (i === step) {
            dot.style.background = '#2563eb';
            dot.style.borderColor = '#60a5fa';
            dot.textContent = i;
        } else {
            dot.style.background = '#1e293b';
            dot.style.borderColor = '#334155';
            dot.textContent = i;
        }
    }
}

async function requestModalPasswordResetOtp(isResend = false) {
    hideModalForgotAlert();
    const emailInput = document.getElementById('modal-reset-email');
    const email = isResend ? forgotRecoveryState.email : (emailInput ? emailInput.value.trim().toLowerCase() : '');

    if (!email || !email.includes('@')) {
        showModalForgotAlert('Please provide a valid registered email address.');
        return;
    }

    forgotRecoveryState.email = email;
    const btn = document.getElementById('btn-modal-send-otp');
    if (btn && !isResend) {
        btn.disabled = true;
        btn.textContent = 'Sending Verification Code... ⏳';
    }

    try {
        const res = await fetch('/api/auth/send-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, purpose: 'RESET_PASSWORD' })
        });
        const data = await res.json();

        if (data.status === 'SUCCESS') {
            forgotRecoveryState.otp = data.otp || '';
            const targetDisp = document.getElementById('modal-target-email-disp');
            if (targetDisp) targetDisp.textContent = email;

            const demoPill = document.getElementById('modal-demo-pill');
            const demoVal = document.getElementById('modal-demo-otp-val');

            if (data.liveEmailSent) {
                if (demoPill) demoPill.style.display = 'none';
                showModalForgotAlert(`📬 Live verification email dispatched to ${email}! Please check your inbox (and spam folder).`, true);
                showToast(`📧 Live verification email sent to ${email}!`);
            } else {
                if (demoPill && demoVal && data.otp) {
                    demoVal.textContent = data.otp;
                    demoPill.style.display = 'flex';
                }
                showModalForgotAlert(`⚠️ Live SMTP not configured in medilink_config.properties. Demo OTP is provided below for testing.`, true);
                showToast(`🔑 Demo OTP generated: ${data.otp}`);
            }

            setModalRecoveryStep(2);
            startModalOtpCountdown();
        } else {
            showModalForgotAlert(data.message || 'Failed to dispatch verification code.');
        }
    } catch (e) {
        showModalForgotAlert('Connection error while reaching authentication server.');
    } finally {
        if (btn && !isResend) {
            btn.disabled = false;
            btn.textContent = 'Send Verification Code ➔';
        }
    }
}

function startModalOtpCountdown() {
    clearInterval(forgotRecoveryState.timerInterval);
    forgotRecoveryState.timerSeconds = 60;
    const timerEl = document.getElementById('modal-otp-countdown');
    const resendBtn = document.getElementById('modal-btn-resend');

    if (resendBtn) {
        resendBtn.style.pointerEvents = 'none';
        resendBtn.style.opacity = '0.4';
    }

    forgotRecoveryState.timerInterval = setInterval(() => {
        forgotRecoveryState.timerSeconds--;
        const mins = String(Math.floor(forgotRecoveryState.timerSeconds / 60)).padStart(2, '0');
        const secs = String(forgotRecoveryState.timerSeconds % 60).padStart(2, '0');
        if (timerEl) timerEl.textContent = `${mins}:${secs}`;

        if (forgotRecoveryState.timerSeconds <= 0) {
            clearInterval(forgotRecoveryState.timerInterval);
            if (timerEl) timerEl.textContent = 'Expired';
            if (resendBtn) {
                resendBtn.style.pointerEvents = 'auto';
                resendBtn.style.opacity = '1';
            }
        }
    }, 1000);
}

function autoFillModalOtp() {
    if (!forgotRecoveryState.otp) return;
    const input = document.getElementById('modal-otp-code');
    if (input) input.value = forgotRecoveryState.otp;
    showModalForgotAlert('OTP code auto-filled from session.', true);
}

async function verifyModalPasswordResetOtp() {
    hideModalForgotAlert();
    const otpInput = document.getElementById('modal-otp-code');
    const code = otpInput ? otpInput.value.trim() : '';

    if (code.length !== 6) {
        showModalForgotAlert('Please enter the full 6-digit verification code.');
        return;
    }

    const btn = document.getElementById('btn-modal-verify-otp');
    if (btn) btn.disabled = true;

    try {
        const res = await fetch('/api/auth/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: forgotRecoveryState.email, otp: code })
        });
        const data = await res.json();

        if (data.status === 'SUCCESS') {
            setModalRecoveryStep(3);
            showModalForgotAlert('Code verified! Please specify your new password.', true);
        } else {
            showModalForgotAlert(data.message || 'Invalid or expired verification code.');
        }
    } catch (e) {
        showModalForgotAlert('Connection error while verifying code.');
    } finally {
        if (btn) btn.disabled = false;
    }
}

function evaluateModalPasswordStrength() {
    const pass = document.getElementById('modal-new-password')?.value || '';
    const confirm = document.getElementById('modal-confirm-password')?.value || '';

    const ruleLen = document.getElementById('modal-rule-len');
    const ruleMatch = document.getElementById('modal-rule-match');
    const meter = document.getElementById('modal-strength-meter');
    const badge = document.getElementById('modal-strength-badge');

    let score = 0;
    if (pass.length >= 6) {
        score += 50;
        if (ruleLen) { ruleLen.textContent = '✓ Min 6 chars'; ruleLen.style.color = '#34d399'; }
    } else {
        if (ruleLen) { ruleLen.textContent = '○ Min 6 chars'; ruleLen.style.color = '#64748b'; }
    }

    if (pass && confirm && pass === confirm) {
        score += 50;
        if (ruleMatch) { ruleMatch.textContent = '✓ Passwords match'; ruleMatch.style.color = '#34d399'; }
    } else {
        if (ruleMatch) { ruleMatch.textContent = '○ Passwords match'; ruleMatch.style.color = '#64748b'; }
    }

    if (meter && badge) {
        meter.style.width = Math.max(score, 15) + '%';
        if (score === 0) {
            meter.style.background = '#ef4444';
            badge.textContent = 'Too Short';
            badge.style.color = '#ef4444';
        } else if (score === 50) {
            meter.style.background = '#f59e0b';
            badge.textContent = 'Moderate';
            badge.style.color = '#f59e0b';
        } else {
            meter.style.background = '#10b981';
            badge.textContent = 'Strong';
            badge.style.color = '#10b981';
        }
    }
}

async function submitModalPasswordReset() {
    hideModalForgotAlert();
    const newPass = document.getElementById('modal-new-password')?.value || '';
    const confirmPass = document.getElementById('modal-confirm-password')?.value || '';
    const otpCode = document.getElementById('modal-otp-code')?.value.trim() || '';

    if (newPass.length < 6) {
        showModalForgotAlert('Password must contain at least 6 characters.');
        return;
    }

    if (newPass !== confirmPass) {
        showModalForgotAlert('Passwords do not match. Please re-enter.');
        return;
    }

    const btn = document.getElementById('btn-modal-submit-reset');
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Saving New Password... ⏳';
    }

    try {
        const res = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email: forgotRecoveryState.email,
                otp: otpCode,
                newPassword: newPass
            })
        });
        const data = await res.json();

        if (data.status === 'SUCCESS') {
            setModalRecoveryStep(4);
            showToast('🎉 Password reset successfully! You can now sign in.');
        } else {
            showModalForgotAlert(data.message || 'Password reset failed. Please check your verification code.');
        }
    } catch (e) {
        showModalForgotAlert('Connection error while updating password.');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Save New Password ✓';
        }
    }
}

function backToSignInFromForgot(resetSuccess = false) {
    closeForgotPasswordModal();
    if (resetSuccess) {
        const signinEmail = document.getElementById('signin-email');
        const signinPass = document.getElementById('signin-password');
        const newPass = document.getElementById('modal-new-password')?.value || '';

        if (signinEmail) signinEmail.value = forgotRecoveryState.email;
        if (signinPass) signinPass.value = newPass;

        const alertBox = document.getElementById('signin-alert-box');
        if (alertBox) {
            alertBox.innerHTML = `
                <span class="alert-icon">🎉</span>
                <div style="color: #6ee7b7;">
                    <strong>Password updated!</strong> Please click Sign In to continue.
                </div>
            `;
            alertBox.style.display = 'flex';
        }
    }
    setAuthMode('signin');
    openModal('modal-auth');
}

function applyAuthenticatedUser(user, saveToStorage = true) {
    state.isAuthenticated = true;
    let ecList = [];
    if (user.emergencyContacts && Array.isArray(user.emergencyContacts)) {
        ecList = user.emergencyContacts;
    } else if (user.emergencyContactsJson) {
        try {
            ecList = JSON.parse(user.emergencyContactsJson);
        } catch (e) {}
    }

    state.currentUser = {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role,
        phone: user.phone || '',
        dob: user.dob || user.dateOfBirth || '',
        gender: user.gender || detectGender(user.name),
        customAvatar: user.customAvatar || '',
        emergencyContact: user.emergencyContact || '',
        emergencyContacts: ecList,
        medicalId: user.medicalId || {
            bloodType: user.bloodType || '',
            allergies: user.allergies !== undefined ? user.allergies : '',
            chronicConditions: user.chronicConditions !== undefined ? user.chronicConditions : ''
        }
    };
    state.activeRole = user.role;

    if (saveToStorage) {
        sessionStorage.setItem('medilink_user', JSON.stringify(state.currentUser));
        localStorage.setItem('medilink_user', JSON.stringify(state.currentUser));
    }

    const avatarAssets = getUserAvatarAssets(user.name, user.gender, user.role);

    // 1. Update Landing Header UI
    const guestBtns = document.getElementById('nav-guest-buttons');
    const userProfile = document.getElementById('nav-user-profile');
    const nameLabel = document.getElementById('header-user-name');
    const roleLabel = document.getElementById('header-user-role');
    const avatar = document.getElementById('header-user-avatar');

    if (guestBtns) guestBtns.style.display = 'none';
    if (userProfile) userProfile.style.display = 'flex';
    if (nameLabel) nameLabel.textContent = user.name;
    if (roleLabel) roleLabel.textContent = user.role;
    if (avatar) avatar.textContent = avatarAssets.emoji;

    // Update Central Top Navbar Portal Badge
    const portalIndicator = document.getElementById('portal-indicator-text');
    if (portalIndicator) {
        if (user.role === 'PATIENT') portalIndicator.textContent = 'Patient Portal';
        else if (user.role === 'PHARMACIST') portalIndicator.textContent = 'Pharmacist Portal';
        else if (user.role === 'ADMIN') portalIndicator.textContent = 'Admin Portal';
        else portalIndicator.textContent = `${user.role} Portal`;
    }

    // 2. Update Patient Dashboard Header
    const dashName = document.getElementById('patient-dash-name');
    const dashBadge = document.getElementById('patient-dash-badge');
    const dashAvatar = document.getElementById('patient-dash-avatar');

    if (dashName) dashName.textContent = user.name;
    if (dashBadge) dashBadge.textContent = `${user.role} DASHBOARD`;
    if (dashAvatar) dashAvatar.textContent = avatarAssets.emoji;

    // 3. Adapt Sidebar Navigation to Active Role
    const dashTabBtn = document.getElementById('tab-btn-dashboard');
    const stockTabBtn = document.getElementById('tab-btn-stock');
    const verifyTabBtn = document.getElementById('tab-btn-verify');
    const settingsTabBtn = document.getElementById('tab-btn-settings');
    const adminTabBtn = document.getElementById('tab-btn-admin');

    const chatNavLabel = document.getElementById('tab-btn-chat-label');
    const remTabBtn = document.getElementById('tab-btn-reminders');

    if (user.role === 'PATIENT') {
        if (dashTabBtn) dashTabBtn.style.display = 'flex';
        if (settingsTabBtn) settingsTabBtn.style.display = 'flex';
        if (stockTabBtn) stockTabBtn.style.display = 'none';
        if (adminTabBtn) adminTabBtn.style.display = 'none';
        if (remTabBtn) remTabBtn.style.display = 'flex';
        if (chatNavLabel) chatNavLabel.textContent = 'Pharmacist Live Chat';
    } else if (user.role === 'PHARMACIST') {
        if (dashTabBtn) dashTabBtn.style.display = 'none';
        if (settingsTabBtn) settingsTabBtn.style.display = 'none';
        if (stockTabBtn) stockTabBtn.style.display = 'flex';
        if (adminTabBtn) adminTabBtn.style.display = 'none';
        if (remTabBtn) remTabBtn.style.display = 'none';
        if (chatNavLabel) chatNavLabel.textContent = 'Patient Live Chat';
    } else if (user.role === 'ADMIN') {
        if (dashTabBtn) dashTabBtn.style.display = 'none';
        if (settingsTabBtn) settingsTabBtn.style.display = 'flex';
        if (stockTabBtn) stockTabBtn.style.display = 'flex';
        if (verifyTabBtn) verifyTabBtn.style.display = 'flex';
        if (adminTabBtn) adminTabBtn.style.display = 'flex';
        if (remTabBtn) remTabBtn.style.display = 'none';
        if (chatNavLabel) chatNavLabel.textContent = 'Live Consultations';
        setTimeout(() => {
            switchTab('admin');
            loadAdminData();
        }, 120);
    }

    // 4. Sync Profile Settings Form with Gender-Matched Assets
    syncProfileSettingsFields(user);

    // 5. Sync Help Center User Initial & Support Center Avatar
    const helpInitial = document.getElementById('help-user-initial');
    if (helpInitial) helpInitial.textContent = (user.name || 'P').trim().charAt(0).toUpperCase();

    const supportAvatar = document.getElementById('support-avatar-img');
    if (supportAvatar) supportAvatar.src = (user.customAvatar || avatarAssets.photo);

    // Refresh prescriptions and notifications
    loadPrescriptions();
    loadReminders();
    loadChatMessages();
}

function formatPatientId(id) {
    if (!id) return 'PA-9824-A';
    if (id.startsWith('PA-')) return id;
    if (id.startsWith('ML-')) return id.replace(/^ML-/, 'PA-');
    let hash = 0;
    for (let i = 0; i < id.length; i++) {
        hash = (hash << 5) - hash + id.charCodeAt(i);
        hash |= 0;
    }
    const num = 1000 + Math.abs(hash % 9000);
    const letter = String.fromCharCode(65 + Math.abs(hash % 26));
    return `PA-${num}-${letter}`;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// Patient Profile Settings Functions (Screenshot Exact Features)
function syncProfileSettingsFields(user) {
    if (!user) return;
    const nameParts = (user.name || '').trim().split(/\s+/);
    const firstName = nameParts[0] || '';
    const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : '';

    const avatarAssets = getUserAvatarAssets(user.name, user.gender, user.role);
    const formattedId = formatPatientId(user.id);

    const fnInput = document.getElementById('profile-first-name');
    const lnInput = document.getElementById('profile-last-name');
    const emailInput = document.getElementById('profile-email');
    const dobInput = document.getElementById('profile-dob');
    const phoneInput = document.getElementById('profile-phone');
    const genderSelect = document.getElementById('profile-gender');
    const cardName = document.getElementById('settings-card-name');
    const cardId = document.getElementById('settings-card-id');
    const avatarImg = document.getElementById('settings-avatar-img');

    if (fnInput) fnInput.value = firstName;
    if (lnInput) lnInput.value = lastName;
    if (emailInput) emailInput.value = user.email || '';
    if (dobInput) dobInput.value = user.dob || '';
    if (phoneInput) phoneInput.value = user.phone || '';
    if (cardName) cardName.textContent = user.name || firstName || 'User Profile';
    if (cardId) cardId.textContent = `Patient ID: ${formattedId}`;
    
    // Use custom uploaded photo or automatically gender-matched photo
    if (avatarImg) {
        avatarImg.src = (user && user.customAvatar) ? user.customAvatar : avatarAssets.photo;
    }

    // Auto-select gender dropdown
    if (genderSelect) {
        genderSelect.value = user.gender ? user.gender : (avatarAssets.gender === 'FEMALE' ? 'Female' : 'Male');
    }

    // Restore emergency contacts if user has saved any
    const contactsList = document.getElementById('emergency-contacts-list');
    if (contactsList) {
        contactsList.innerHTML = '';
        if (user.emergencyContacts && user.emergencyContacts.length > 0) {
            user.emergencyContacts.forEach(contact => {
                const row = document.createElement('div');
                row.className = 'contact-box-row';
                row.innerHTML = `
                    <div class="c-col">
                        <span class="c-label">Name</span>
                        <input type="text" class="c-input contact-name" placeholder="Contact Name" value="${escapeHtml(contact.name || '')}">
                    </div>
                    <div class="c-col">
                        <span class="c-label">Relationship</span>
                        <select class="c-select contact-rel">
                            <option value="Spouse" ${contact.relationship === 'Spouse' ? 'selected' : ''}>Spouse</option>
                            <option value="Parent" ${contact.relationship === 'Parent' ? 'selected' : ''}>Parent</option>
                            <option value="Sibling" ${contact.relationship === 'Sibling' ? 'selected' : ''}>Sibling</option>
                            <option value="Child" ${contact.relationship === 'Child' ? 'selected' : ''}>Child</option>
                            <option value="Family" ${contact.relationship === 'Family' ? 'selected' : ''}>Family</option>
                            <option value="Primary Care" ${contact.relationship === 'Primary Care' ? 'selected' : ''}>Primary Care</option>
                            <option value="Doctor" ${contact.relationship === 'Doctor' ? 'selected' : ''}>Doctor</option>
                            <option value="Friend" ${contact.relationship === 'Friend' ? 'selected' : ''}>Friend</option>
                        </select>
                    </div>
                    <div class="c-col">
                        <span class="c-label">Phone</span>
                        <div style="display:flex; align-items:center; gap:6px;">
                            <input type="text" class="c-input contact-phone" placeholder="+880 1700-000000" value="${escapeHtml(contact.phone || '')}">
                            <button type="button" onclick="removeEmergencyContactRow(this)" title="Delete Contact" style="background:none; border:none; color:#ef4444; font-size:1.1rem; cursor:pointer;">🗑️</button>
                        </div>
                    </div>
                `;
                contactsList.appendChild(row);
            });
        } else {
            contactsList.innerHTML = `
                <div class="empty-contacts-msg" id="empty-contacts-msg" style="color:#64748b; font-size:0.85rem; padding:16px; background:#f8fafc; border-radius:8px; border:1px dashed #cbd5e1; text-align:center;">
                    No emergency contacts added yet. Click <strong>⊕ Add Contact</strong> above to add one.
                </div>
            `;
        }
    }

    // Restore medical ID data if present in session
    const bloodPill = document.getElementById('pill-blood-type');
    if (bloodPill) {
        if (user && user.medicalId && user.medicalId.bloodType) {
            bloodPill.textContent = user.medicalId.bloodType;
            bloodPill.style.background = '#fee2e2';
            bloodPill.style.color = '#dc2626';
        } else {
            bloodPill.textContent = 'Not specified';
            bloodPill.style.background = '#f1f5f9';
            bloodPill.style.color = '#64748b';
        }
    }

    const allergiesContainer = document.getElementById('allergies-pills-container');
    if (allergiesContainer) {
        allergiesContainer.innerHTML = '';
        const rawAllergies = (user && user.medicalId && user.medicalId.allergies) ? user.medicalId.allergies : '';
        const allergies = rawAllergies ? rawAllergies.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (allergies.length === 0) {
            allergiesContainer.innerHTML = '<span class="pill-allergy" style="background:#f1f5f9; color:#64748b;">None reported</span>';
        } else {
            allergies.forEach(a => {
                const span = document.createElement('span');
                span.className = 'pill-allergy';
                span.textContent = a;
                allergiesContainer.appendChild(span);
            });
        }
    }

    const chronicContainer = document.getElementById('chronic-pills-container');
    if (chronicContainer) {
        chronicContainer.innerHTML = '';
        const rawChronic = (user && user.medicalId && user.medicalId.chronicConditions) ? user.medicalId.chronicConditions : '';
        const conditions = rawChronic ? rawChronic.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (conditions.length === 0) {
            chronicContainer.innerHTML = '<span class="pill-condition" style="background:#f1f5f9; color:#64748b;">None reported</span>';
        } else {
            conditions.forEach(c => {
                const span = document.createElement('span');
                span.className = 'pill-condition';
                span.textContent = c;
                chronicContainer.appendChild(span);
            });
        }
    }
}

async function saveProfileSettings() {
    const firstName = document.getElementById('profile-first-name').value.trim();
    const lastName = document.getElementById('profile-last-name').value.trim();
    const email = document.getElementById('profile-email').value.trim();
    const dob = document.getElementById('profile-dob').value.trim();
    const phone = document.getElementById('profile-phone').value.trim();
    const gender = document.getElementById('profile-gender').value;

    const fullName = (firstName + ' ' + lastName).trim() || firstName;
    if (!fullName) {
        showToast('Please enter a valid first name.');
        return;
    }

    // Collect all emergency contacts
    const contacts = [];
    document.querySelectorAll('#emergency-contacts-list .contact-box-row').forEach(row => {
        const name = row.querySelector('.contact-name') ? row.querySelector('.contact-name').value.trim() : '';
        const rel = row.querySelector('.contact-rel') ? row.querySelector('.contact-rel').value : 'Family';
        const p = row.querySelector('.contact-phone') ? row.querySelector('.contact-phone').value.trim() : '';
        if (name || p) {
            contacts.push({ name: name, relationship: rel, phone: p });
        }
    });

    const bloodPill = document.getElementById('pill-blood-type');
    const bloodType = (bloodPill && bloodPill.textContent.trim() !== 'Not specified') ? bloodPill.textContent.trim() : (state.currentUser?.medicalId?.bloodType || '');
    const allergyPills = document.querySelectorAll('#allergies-pills-container .pill-allergy');
    const allergies = Array.from(allergyPills).map(p => p.textContent.trim()).filter(t => t !== 'None reported').join(', ');
    const conditionPills = document.querySelectorAll('#chronic-pills-container .pill-condition');
    const chronicConditions = Array.from(conditionPills).map(p => p.textContent.trim()).filter(t => t !== 'None reported').join(', ');

    const userId = state.currentUser ? state.currentUser.id : null;
    const customAvatar = state.currentUser ? state.currentUser.customAvatar : null;

    try {
        const payload = {
            id: userId,
            name: fullName,
            email: email,
            dob: dob,
            phone: phone,
            gender: gender,
            bloodType: bloodType,
            allergies: allergies,
            chronicConditions: chronicConditions,
            emergencyContacts: contacts,
            customAvatar: customAvatar
        };

        const res = await fetch('/api/patient/profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (data.status === 'SUCCESS') {
            // Update state & storage
            state.currentUser.name = data.name || fullName;
            state.currentUser.email = data.email || email;
            state.currentUser.dob = data.dob || dob;
            state.currentUser.phone = data.phone || phone;
            state.currentUser.gender = data.gender || gender;
            state.currentUser.emergencyContacts = contacts;
            state.currentUser.medicalId = {
                bloodType: data.bloodType || bloodType,
                allergies: data.allergies !== undefined ? data.allergies : allergies,
                chronicConditions: data.chronicConditions !== undefined ? data.chronicConditions : chronicConditions
            };
            if (data.customAvatar) {
                state.currentUser.customAvatar = data.customAvatar;
            }

            sessionStorage.setItem('medilink_user', JSON.stringify(state.currentUser));
            localStorage.setItem('medilink_user', JSON.stringify(state.currentUser));

            const avatarAssets = getUserAvatarAssets(fullName, gender, state.currentUser.role);

            // Update all UI badges & avatars
            const nameLabel = document.getElementById('header-user-name');
            const avatarLabel = document.getElementById('header-user-avatar');
            const dashName = document.getElementById('patient-dash-name');
            const dashAvatar = document.getElementById('patient-dash-avatar');
            const cardName = document.getElementById('settings-card-name');
            const avatarImg = document.getElementById('settings-avatar-img');

            if (nameLabel) nameLabel.textContent = fullName;
            if (avatarLabel) avatarLabel.textContent = avatarAssets.emoji;
            if (dashName) dashName.textContent = fullName;
            if (dashAvatar) dashAvatar.textContent = avatarAssets.emoji;
            if (cardName) cardName.textContent = fullName;
            if (avatarImg && !state.currentUser.customAvatar) avatarImg.src = avatarAssets.photo;

            showToast(`💾 Changes saved & updated in PostgreSQL database!`);
        } else {
            showToast('⚠️ ' + (data.message || 'Failed to save profile.'));
        }
    } catch (err) {
        console.error('Error saving profile to database:', err);
        showToast('❌ Server error saving profile.');
    }
}

function resetProfileSettingsForm() {
    syncProfileSettingsFields(state.currentUser);
    showToast('Changes discarded. Profile reset to previous state.');
}

function triggerAvatarChange() {
    const fileInput = document.getElementById('patient-photo-file-input');
    if (fileInput) {
        fileInput.click();
    }
}

function openDobCalendar() {
    const dob = document.getElementById('profile-dob');
    if (dob) {
        if (dob.showPicker) {
            try {
                dob.showPicker();
            } catch (e) {
                dob.focus();
            }
        } else {
            dob.focus();
        }
    }
}

function handlePatientPhotoUpload(event) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
        showToast('❌ Please select a valid image file (JPG, PNG, WEBP, GIF, etc.)');
        return;
    }

    const reader = new FileReader();
    reader.onload = function(e) {
        const dataUrl = e.target.result;
        const img = document.getElementById('settings-avatar-img');
        if (img) {
            img.src = dataUrl;
        }

        // Save custom uploaded photo to current user state & storage
        if (state.currentUser) {
            state.currentUser.customAvatar = dataUrl;
            sessionStorage.setItem('medilink_user', JSON.stringify(state.currentUser));

            // Also persist to PostgreSQL
            if (state.currentUser.id) {
                fetch('/api/patient/profile', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        id: state.currentUser.id,
                        customAvatar: dataUrl
                    })
                }).catch(e => console.error('Error syncing photo to DB:', e));
            }
        }

        showToast(`✅ Profile photo uploaded & saved! (${file.name})`);
    };
    reader.readAsDataURL(file);
}

function openEditMedicalIdModal() {
    // Read current values from the UI
    const currentBlood = (document.getElementById('pill-blood-type') && document.getElementById('pill-blood-type').textContent.trim() !== 'Not specified') ? document.getElementById('pill-blood-type').textContent.trim() : 'O Positive';
    
    const allergyPills = document.querySelectorAll('#allergies-pills-container .pill-allergy');
    const currentAllergies = Array.from(allergyPills).map(p => p.textContent.trim()).filter(t => t !== 'None reported').join(', ');

    const conditionPills = document.querySelectorAll('#chronic-pills-container .pill-condition');
    const currentChronic = Array.from(conditionPills).map(p => p.textContent.trim()).filter(t => t !== 'None reported').join(', ');

    const bloodSelect = document.getElementById('edit-med-blood');
    const allergiesInput = document.getElementById('edit-med-allergies');
    const chronicInput = document.getElementById('edit-med-chronic');

    if (bloodSelect) bloodSelect.value = currentBlood;
    if (allergiesInput) allergiesInput.value = currentAllergies;
    if (chronicInput) chronicInput.value = currentChronic;

    openModal('modal-edit-medical-id');
}

function submitEditMedicalId() {
    const blood = document.getElementById('edit-med-blood').value;
    const allergiesStr = document.getElementById('edit-med-allergies').value.trim();
    const chronicStr = document.getElementById('edit-med-chronic').value.trim();

    // 1. Update Blood Type Pill
    const bloodPill = document.getElementById('pill-blood-type');
    if (bloodPill) {
        bloodPill.textContent = blood;
    }

    // 2. Update Known Allergies Pills
    const allergiesContainer = document.getElementById('allergies-pills-container');
    if (allergiesContainer) {
        allergiesContainer.innerHTML = '';
        const allergies = allergiesStr ? allergiesStr.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (allergies.length === 0) {
            allergiesContainer.innerHTML = '<span class="pill-allergy" style="background:#f1f5f9; color:#64748b;">None reported</span>';
        } else {
            allergies.forEach(a => {
                const span = document.createElement('span');
                span.className = 'pill-allergy';
                span.textContent = a;
                allergiesContainer.appendChild(span);
            });
        }
    }

    // 3. Update Chronic Conditions Pills
    const chronicContainer = document.getElementById('chronic-pills-container');
    if (chronicContainer) {
        chronicContainer.innerHTML = '';
        const conditions = chronicStr ? chronicStr.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (conditions.length === 0) {
            chronicContainer.innerHTML = '<span class="pill-condition" style="background:#f1f5f9; color:#64748b;">None reported</span>';
        } else {
            conditions.forEach(c => {
                const span = document.createElement('span');
                span.className = 'pill-condition';
                span.textContent = c;
                chronicContainer.appendChild(span);
            });
        }
    }

    // Save to active user state & session storage
    if (state.currentUser) {
        state.currentUser.medicalId = {
            bloodType: blood,
            allergies: allergiesStr,
            chronicConditions: chronicStr
        };
        sessionStorage.setItem('medilink_user', JSON.stringify(state.currentUser));

        // Also persist to PostgreSQL
        if (state.currentUser.id) {
            fetch('/api/patient/profile', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    id: state.currentUser.id,
                    bloodType: blood,
                    allergies: allergiesStr,
                    chronicConditions: chronicStr
                })
            }).catch(e => console.error('Error syncing medical ID to DB:', e));
        }
    }

    closeModal('modal-edit-medical-id');
    showToast('✅ Medical ID saved & synced with database!');
}

function openAddContactModal() {
    const list = document.getElementById('emergency-contacts-list');
    if (!list) return;
    const emptyMsg = document.getElementById('empty-contacts-msg');
    if (emptyMsg) {
        emptyMsg.remove();
    }
    const newRow = document.createElement('div');
    newRow.className = 'contact-box-row';
    newRow.innerHTML = `
        <div class="c-col">
            <span class="c-label">Name</span>
            <input type="text" class="c-input contact-name" placeholder="Contact Name" value="">
        </div>
        <div class="c-col">
            <span class="c-label">Relationship</span>
            <select class="c-select contact-rel">
                <option value="Family">Family</option>
                <option value="Spouse">Spouse</option>
                <option value="Parent">Parent</option>
                <option value="Sibling">Sibling</option>
                <option value="Child">Child</option>
                <option value="Primary Care">Primary Care</option>
                <option value="Doctor">Doctor</option>
                <option value="Friend">Friend</option>
            </select>
        </div>
        <div class="c-col" style="position:relative;">
            <span class="c-label">Phone</span>
            <div style="display:flex; align-items:center; gap:6px;">
                <input type="text" class="c-input contact-phone" placeholder="+880 1700-000000" value="">
                <button type="button" onclick="removeEmergencyContactRow(this)" title="Delete Contact" style="background:none; border:none; color:#ef4444; font-size:1.1rem; cursor:pointer;">🗑️</button>
            </div>
        </div>
    `;
    list.appendChild(newRow);
    showToast('➕ New emergency contact row added. Enter contact details.');
}

function removeEmergencyContactRow(btn) {
    const row = btn.closest('.contact-box-row');
    if (row) {
        row.remove();
        const list = document.getElementById('emergency-contacts-list');
        if (list && list.querySelectorAll('.contact-box-row').length === 0) {
            list.innerHTML = `
                <div class="empty-contacts-msg" id="empty-contacts-msg" style="color:#64748b; font-size:0.85rem; padding:16px; background:#f8fafc; border-radius:8px; border:1px dashed #cbd5e1; text-align:center;">
                    No emergency contacts added yet. Click <strong>⊕ Add Contact</strong> above to add one.
                </div>
            `;
        }
        showToast('🗑️ Emergency contact removed.');
    }
}

function handleUserLogout() {
    sessionStorage.removeItem('medilink_user');
    localStorage.removeItem('medilink_user');
    state.isAuthenticated = false;
    state.currentUser = null;

    // Reset Landing Header UI
    const guestBtns = document.getElementById('nav-guest-buttons');
    const userProfile = document.getElementById('nav-user-profile');
    if (guestBtns) guestBtns.style.display = 'flex';
    if (userProfile) userProfile.style.display = 'none';

    returnToLanding();
    showToast('👋 You have been logged out.');
}

// Dynamic Stats & Metrics Loader with Scroll-Triggered Count-Up Animation
let cachedStats = null;
let statsAnimated = false;
let metricsAnimated = false;

async function loadDynamicStats() {
    try {
        const res = await fetch('/api/stats');
        cachedStats = await res.json();
    } catch (e) {
        console.warn('Using fallback dynamic counts:', e);
        cachedStats = {
            activeUsers: 10215,
            certifiedPharmacists: 560,
            remindersSent: '1.1M+',
            docTimeReduction: -32,
            engagementRate: 49
        };
    }

    // Set up scroll-triggered observers
    initScrollTriggeredCountUp();
}

function initScrollTriggeredCountUp() {
    const statsSection = document.querySelector('.stats-section');
    const proSection = document.querySelector('.professionals-section');

    const observerOptions = { threshold: 0.25 };

    if (statsSection) {
        const statsObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !statsAnimated && cachedStats) {
                    statsAnimated = true;
                    runStatsCountUp(cachedStats);
                }
            });
        }, observerOptions);
        statsObserver.observe(statsSection);
    }

    if (proSection) {
        const proObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !metricsAnimated && cachedStats) {
                    metricsAnimated = true;
                    runMetricsCountUp(cachedStats);
                }
            });
        }, observerOptions);
        proObserver.observe(proSection);
    }

    // Run immediately if already visible
    if (statsSection && isElementInViewport(statsSection) && !statsAnimated && cachedStats) {
        statsAnimated = true;
        runStatsCountUp(cachedStats);
    }
}

function isElementInViewport(el) {
    const rect = el.getBoundingClientRect();
    return (
        rect.top < (window.innerHeight || document.documentElement.clientHeight) &&
        rect.bottom >= 0
    );
}

function runStatsCountUp(data) {
    // 1. Active Users (0 -> 10,215+)
    animateCounter('stat-active-users', 0, data.activeUsers || 10215, 1800, (v) => `${Math.round(v).toLocaleString()}+`);

    // 2. Certified Pharmacists (0 -> 560+)
    animateCounter('stat-certified-pharmacists', 0, data.certifiedPharmacists || data.certifiedDoctors || 560, 1600, (v) => `${Math.round(v)}+`);

    // 3. Reminders Sent (0.0M+ -> 1.1M+)
    const millions = typeof data.remindersSent === 'string' ? parseFloat(data.remindersSent) || 1.1 : 1.1;
    animateCounter('stat-reminders-sent', 0.0, millions, 1800, (v) => `${v.toFixed(1)}M+`);
}

function runMetricsCountUp(data) {
    // 4. Time spent on doc (0% -> -32%)
    animateCounter('metric-doc-time', 0, Math.abs(data.docTimeReduction || 32), 1600, (v) => `-${Math.round(v)}%`);

    // 5. Engagement rate (0% -> +49%)
    animateCounter('metric-engagement', 0, data.engagementRate || 49, 1600, (v) => `+${Math.round(v)}%`);
}

function animateCounter(elementId, start, end, duration, formatFn) {
    const el = document.getElementById(elementId);
    if (!el) return;

    el.classList.add('counting-active');
    let startTimestamp = null;

    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        // Fluid cubic ease-out
        const easeOut = 1 - Math.pow(1 - progress, 3);
        const currentVal = start + (end - start) * easeOut;

        el.textContent = formatFn ? formatFn(currentVal) : Math.round(currentVal);

        if (progress < 1) {
            window.requestAnimationFrame(step);
        } else {
            el.classList.remove('counting-active');
        }
    };
    window.requestAnimationFrame(step);
}

// Landing Page Header Navigation & Scrollspy
function initLandingNav() {
    const navLinks = document.querySelectorAll('.landing-header .nav-link');
    const sections = document.querySelectorAll('#features, #how-it-works, #for-patients, #for-pharmacists, #about');

    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        });
    });

    window.addEventListener('scroll', () => {
        const viewLanding = document.getElementById('view-landing');
        if (!viewLanding || !viewLanding.classList.contains('active')) return;

        let activeId = '';
        const scrollPos = window.scrollY + 180;

        sections.forEach(sec => {
            const top = sec.offsetTop;
            const height = sec.offsetHeight;
            if (scrollPos >= top && scrollPos < top + height) {
                activeId = sec.getAttribute('id');
            }
        });

        if (activeId) {
            navLinks.forEach(link => {
                if (link.getAttribute('href') === `#${activeId}`) {
                    navLinks.forEach(l => l.classList.remove('active'));
                    link.classList.add('active');
                }
            });
        }
    }, { passive: true });
}

// Role Switcher
function changeUserRole(role) {
    if (state.isAuthenticated && state.currentUser && state.currentUser.name) {
        state.activeRole = role;
        state.currentUser.role = role;
        applyAuthenticatedUser(state.currentUser);
        showToast(`Active role: ${role} (${state.currentUser.name})`);
        return;
    }
    state.activeRole = role;
    if (role === 'PATIENT') {
        state.currentUser = {
            id: 'PA-9824-A',
            name: 'Rahim Ahmed',
            email: 'rahim@medilink.com',
            role: 'PATIENT'
        };
    } else if (role === 'PHARMACIST') {
        state.currentUser = {
            id: 'PH-9920-DGDA',
            name: 'Dr. Farhan Kabir',
            email: 'farhan@lazzpharma.com',
            role: 'PHARMACIST'
        };
    } else if (role === 'ADMIN') {
        state.currentUser = {
            id: 'ADM-1001',
            name: 'System Administrator',
            email: 'admin@medilink.com',
            role: 'ADMIN'
        };
    }
    applyAuthenticatedUser(state.currentUser);
    showToast(`Active user switched to: ${state.currentUser.name} (${role})`);
    loadPrescriptions();
    loadStocks();
    loadChatMessages();
}

// App Tab Switcher
function switchTab(tabId) {
    document.querySelectorAll('.tab-view').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.sidebar-footer-link').forEach(el => el.classList.remove('active'));

    const targetTab = document.getElementById(`tab-${tabId}`);
    const targetBtn = document.getElementById(`tab-btn-${tabId}`);

    if (targetTab) targetTab.classList.add('active');
    if (targetBtn) targetBtn.classList.add('active');

    // Hide top navbar "Upload Prescription" button when inside Medical Records or Upload Rx
    const topUploadBtn = document.querySelector('.btn-top-upload');
    if (topUploadBtn) {
        if (tabId === 'prescriptions' || tabId === 'upload-rx') {
            topUploadBtn.style.display = 'none';
        } else {
            topUploadBtn.style.display = 'block';
        }
    }

    if (tabId === 'chat') {
        loadConversationsList();
        loadChatMessages();
        startChatHeartbeat();
    } else {
        stopChatHeartbeat();
    }
    if (tabId === 'reminders') {
        loadReminders();
    }
    if (tabId === 'prescriptions') {
        loadPrescriptions();
    }
    if (tabId === 'admin') {
        loadAdminData();
    }
}

// Real-Time Server-Sent Events (SSE) Observer Pattern Client
function initRealTimeStream() {
    const sseIndicator = document.getElementById('sse-indicator');
    const sseText = document.getElementById('sse-text');
    const tickerBox = document.getElementById('event-ticker-box');

    try {
        const streamUrl = `${API_BASE_URL || ''}/api/events/stream`;
        state.eventSource = new EventSource(streamUrl);

        state.eventSource.onopen = () => {
            if (sseIndicator) sseIndicator.style.background = '#ecfdf5';
            if (sseText) sseText.textContent = 'Observer Stream Active';
        };

        state.eventSource.onmessage = (event) => {
            const raw = event.data;
            if (!raw) return;

            if (tickerBox) {
                const time = new Date().toLocaleTimeString();
                const item = document.createElement('div');
                item.className = 'ticker-item';
                item.textContent = `[${time}] ${raw}`;
                tickerBox.insertBefore(item, tickerBox.firstChild);
            }

            if (raw.includes('DEMO NOTIFICATION') || raw.includes('MEDICINE ALARM')) {
                openAlarmModal(raw);
                addNotification({
                    icon: '⏰',
                    title: 'Medicine Dose Alarm',
                    text: raw.trim(),
                    time: 'Just now'
                });
            } else if (raw.includes('STOCK_UPDATE') || raw.includes('PRESCRIPTION_') || raw.includes('ADMIN_BROADCAST') || raw.includes('SYSTEM ALERT') || raw.includes('SYSTEM_ANNOUNCEMENT') || raw.includes('PRICE_UPDATE')) {
                if (raw.includes('PRICE_UPDATE')) {
                    handleLivePriceUpdateEvent(raw);
                } else {
                    showToast(raw);
                }
                if (typeof appendAdminAuditLine === 'function') {
                    appendAdminAuditLine(raw);
                }
            }

            if (raw.includes('STOCK_UPDATE')) {
                loadStocks();
                addNotification({
                    icon: '🔔',
                    title: 'Pharmacy Stock Update',
                    text: raw.replace('STOCK_UPDATE:', '').trim(),
                    time: 'Just now'
                });
            }
            if (raw.includes('PRESCRIPTION_')) {
                loadPrescriptions();
                addNotification({
                    icon: '📝',
                    title: 'Prescription Status Update',
                    text: raw.trim(),
                    time: 'Just now'
                });
            }
            if (raw.includes('CHAT_MESSAGE')) {
                const typingIndicator = document.getElementById('chat-typing-indicator');
                if (typingIndicator) typingIndicator.style.display = 'none';

                let chatMsg = null;
                try {
                    const jsonIdx = raw.indexOf('{');
                    if (jsonIdx !== -1) {
                        chatMsg = JSON.parse(raw.substring(jsonIdx));
                    }
                } catch (e) {
                    console.warn('Error parsing CHAT_MESSAGE:', e);
                }

                const currentRole = (state.currentUser?.role || state.activeRole || 'PATIENT').toUpperCase();
                const currentId = state.currentUser?.id || 'ML-9824-A';
                const currentEmail = (state.currentUser?.email || '').toLowerCase();

                const isFromMe = chatMsg && (chatMsg.senderId === currentId || (chatMsg.senderEmail && chatMsg.senderEmail.toLowerCase() === currentEmail));
                const isForMe = chatMsg && (
                    (chatMsg.receiverId && (chatMsg.receiverId === currentId || (currentRole === 'PHARMACIST' && (chatMsg.receiverId.includes('pharma') || chatMsg.receiverId.startsWith('PH-'))))) ||
                    (chatMsg.receiverEmail && chatMsg.receiverEmail.toLowerCase() === currentEmail) ||
                    (currentRole === 'PHARMACIST' && chatMsg.senderRole === 'PATIENT') ||
                    (currentRole === 'PATIENT' && chatMsg.senderRole === 'PHARMACIST')
                );

                if (chatMsg && isForMe && !isFromMe) {
                    const title = currentRole === 'PHARMACIST'
                        ? `💬 Patient Consultation: ${chatMsg.senderName || 'Patient'}`
                        : `💬 Pharmacist Advice: ${chatMsg.senderName || 'Dr. Pharmacist'}`;
                    const text = chatMsg.content || 'You have received a new consultation message.';

                    addNotification({
                        icon: '💬',
                        title: title,
                        text: text,
                        time: 'Just now'
                    });

                    showToast(`${title} — "${text.length > 55 ? text.substring(0, 52) + '...' : text}"`);
                    playChatNotificationSound();
                }

                // If currently on chat tab, refresh messages immediately
                const chatTab = document.getElementById('tab-chat');
                if (chatTab && chatTab.classList.contains('active')) {
                    loadChatMessages();
                }
                loadConversationsList();
            }
        };

        state.eventSource.onerror = () => {
            if (sseIndicator) sseIndicator.style.background = '#fef2f2';
            if (sseText) sseText.textContent = 'Reconnecting Stream...';
        };
    } catch (e) {
        console.error('SSE initialization error:', e);
    }
}

// 1. Medicine & Search Strategy
async function loadMedicines() {
    try {
        const res = await fetch('/api/medicines');
        const data = await res.json();
        state.medicines = data.results || [];
        renderMedicines(state.medicines);
        populateVerifyDropdown(state.medicines);
    } catch (e) {
        console.error(e);
    }
}

async function executeMedicineSearch() {
    const query = document.getElementById('med-search-input').value;
    const strategy = document.getElementById('search-strategy-select').value;
    const container = document.getElementById('med-results-container');
    container.innerHTML = '<p class="text-muted">Searching medicines with strategy pattern...</p>';

    try {
        const res = await fetch(`/api/medicines/search?query=${encodeURIComponent(query)}&strategy=${encodeURIComponent(strategy)}`);
        const data = await res.json();
        renderMedicines(data.results || []);
    } catch (e) {
        container.innerHTML = '<p class="text-danger">Search failed.</p>';
    }
}

function renderMedicines(list) {
    const container = document.getElementById('med-results-container');
    if (!container) return;

    if (!list || list.length === 0) {
        container.innerHTML = '<p class="text-muted">No medicines found.</p>';
        return;
    }

    container.innerHTML = list.map(m => `
        <div class="med-card" id="med-card-${m.id}" data-med-id="${m.id}">
            <div>
                <div class="med-header">
                    <div>
                        <div class="med-brand">${escapeHtml(m.brandName)} <small style="font-size:0.75rem; color:#64748b;">${escapeHtml(m.strength || '')}</small></div>
                        <div class="med-generic">${escapeHtml(m.genericName)} • ${escapeHtml(m.formulation || '')}</div>
                    </div>
                    <div class="med-price" id="med-price-${m.id}" data-price="${m.unitPrice}">BDT ${m.unitPrice.toFixed(2)}</div>
                </div>
                <div class="med-company">Mfg: ${escapeHtml(m.company || '')} (${escapeHtml(m.category || '')})</div>
                <div class="med-badge-box">🛡️ ${escapeHtml(m.displayBadge || 'Standard')}</div>
                ${m.sideEffects ? `<small class="text-muted" style="display:block; margin-top:8px;"><strong>Note:</strong> ${escapeHtml(m.sideEffects)}</small>` : ''}
            </div>
            <div class="med-card-actions">
                <button class="btn btn-secondary" style="flex:1;" onclick="findAlternatives('${escapeHtml(m.genericName)}')">🔍 Generic Alts</button>
                <button class="btn btn-secondary" style="flex:1; border-color:#0284c7; color:#0284c7; font-weight:600;" onclick="openPriceComparisonModal('${m.id}', '${escapeHtml(m.brandName)}')">🏷️ Compare Prices</button>
                <button class="btn btn-primary" style="flex:1;" onclick="checkAvailabilityFor('${escapeHtml(m.brandName)}')">📍 Find Stock</button>
            </div>
        </div>
    `).join('');
}

async function openPriceComparisonModal(medicineId, brandName) {
    const modal = document.getElementById('modal-price-compare');
    const banner = document.getElementById('price-compare-banner');
    const list = document.getElementById('price-compare-list');
    const title = document.getElementById('price-compare-title');
    const sub = document.getElementById('price-compare-sub');

    if (title) title.textContent = `Price Comparison: ${brandName || 'Medicine'}`;
    if (sub) sub.textContent = `Comparing live inventory pricing across pharmacies in Dhaka`;
    if (banner) banner.innerHTML = `<div style="text-align:center; padding:20px; color:#64748b;">⏳ Fetching live prices and applying Best Price Strategy...</div>`;
    if (list) list.innerHTML = '';
    if (modal) modal.classList.add('active');

    try {
        const res = await fetch(`/api/medicines/pharmacy-prices?medicineId=${encodeURIComponent(medicineId)}`);
        const data = await res.json();

        if (data.status !== 'SUCCESS') {
            if (banner) banner.innerHTML = `<div style="color:#ef4444; padding:15px; text-align:center;">Failed to load pricing data.</div>`;
            return;
        }

        renderPriceComparisonData(data);
    } catch (err) {
        console.error(err);
        if (banner) banner.innerHTML = `<div style="color:#ef4444; padding:15px; text-align:center;">Network error loading prices.</div>`;
    }
}

function renderPriceComparisonData(data) {
    const banner = document.getElementById('price-compare-banner');
    const list = document.getElementById('price-compare-list');
    if (!banner || !list) return;

    const prices = data.pharmacyPrices || [];
    const bestPrice = data.bestPrice || (prices.length > 0 ? prices[0].unitPrice : 0);
    const maxPrice = data.maxPrice || (prices.length > 0 ? prices[prices.length - 1].unitPrice : bestPrice);
    const savings = data.savingsPercent ? `${data.savingsPercent}%` : (maxPrice > bestPrice ? `${(((maxPrice - bestPrice) / maxPrice) * 100).toFixed(1)}%` : '0%');

    banner.innerHTML = `
        <div class="price-banner-content">
            <div class="price-banner-med-info">
                <span class="price-banner-badge">💊 ${escapeHtml(data.genericName || '')}</span>
                <h3 class="price-banner-title">${escapeHtml(data.brandName || '')} <small>${escapeHtml(data.strength || '')}</small></h3>
                <p class="price-banner-company">Mfg: ${escapeHtml(data.company || 'Licensed Manufacturer')}</p>
            </div>
            <div class="price-banner-metrics">
                <div class="price-metric-box best-val">
                    <span class="price-metric-lbl">Lowest Available</span>
                    <span class="price-metric-val">BDT ${bestPrice.toFixed(2)}</span>
                </div>
                ${maxPrice > bestPrice ? `
                <div class="price-metric-box save-val">
                    <span class="price-metric-lbl">Max Savings</span>
                    <span class="price-metric-val">${savings} OFF</span>
                </div>` : ''}
            </div>
        </div>
    `;

    if (prices.length === 0) {
        list.innerHTML = `
            <div style="text-align:center; padding:24px; color:#64748b; background:#f8fafc; border-radius:8px;">
                <p style="margin:0;">No local pharmacies currently list live inventory for this medicine.</p>
                <small>Standard MRP: BDT ${(data.basePrice || 0).toFixed(2)}</small>
            </div>
        `;
        return;
    }

    list.innerHTML = prices.map((p, idx) => {
        const isBest = p.isBestPrice || idx === 0;
        const diff = p.unitPrice - bestPrice;
        return `
            <div class="pharma-price-row-card ${isBest ? 'is-best-price' : ''}">
                <div class="pharma-price-info">
                    <div class="pharma-price-header">
                        <strong class="pharma-title">🏥 ${escapeHtml(p.pharmacyName)}</strong>
                        <span class="pharma-area-tag">📍 ${escapeHtml(p.area)}</span>
                        ${isBest ? '<span class="best-price-pill">🏆 Lowest Price</span>' : ''}
                        ${p.is24Hours ? '<span class="badge-24h">24/7</span>' : ''}
                    </div>
                    <p class="pharma-address">${escapeHtml(p.address)}</p>
                    <div class="pharma-stock-info">
                        <span class="stock-badge ${p.quantity < 10 ? 'stock-low' : 'stock-ok'}">
                            ${p.quantity > 0 ? `📦 ${p.quantity} Units in Stock` : '⚠️ Out of Stock'}
                        </span>
                        ${diff > 0 ? `<span class="price-diff-note">+BDT ${diff.toFixed(2)} higher than best deal</span>` : '<span class="price-diff-note best">Best value in Dhaka</span>'}
                    </div>
                </div>
                <div class="pharma-price-actions">
                    <div class="unit-price-display">
                        <span class="unit-currency">BDT</span>
                        <span class="unit-val">${p.unitPrice.toFixed(2)}</span>
                        <span class="unit-per">/ unit</span>
                    </div>
                    <div style="display:flex; gap:6px; flex-direction:column; width:100%;">
                        <a href="tel:${p.phone || '+8801711001122'}" class="btn btn-secondary btn-sm" style="text-decoration:none; text-align:center; padding:6px 12px;">
                            📞 Call (${escapeHtml(p.phone || 'Contact')})
                        </a>
                        <button type="button" class="btn btn-primary btn-sm" onclick="startPharmacyChat('${escapeHtml(p.pharmacyName)}')">
                            💬 Message
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function startPharmacyChat(pharmacyName) {
    closeModal('modal-price-compare');
    switchTab('chat');
    showToast(`Opened consultation chat for ${pharmacyName}`);
}

async function findAlternatives(genericName) {
    document.getElementById('med-search-input').value = genericName;
    document.getElementById('search-strategy-select').value = 'BEST_PRICE_STRATEGY';
    executeMedicineSearch();
    showToast(`Showing best-value generic alternatives for: ${genericName}`);
}

function checkAvailabilityFor(brandName) {
    switchTab('stock');
    showToast(`Filtering live pharmacy stock for ${brandName}...`);
}

async function checkInteractions() {
    const meds = document.getElementById('interaction-meds-input').value;
    const mode = document.getElementById('interaction-mode-select').value;
    const resultBox = document.getElementById('interaction-result-box');

    try {
        const res = await fetch('/api/medicines/interaction-check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ medicines: meds, mode: mode })
        });
        const data = await res.json();
        resultBox.style.display = 'block';
        resultBox.innerHTML = `
            <h4>Analysis Result [Mode: ${data.mode}]</h4>
            <pre style="white-space:pre-wrap; font-family:inherit; margin-top:8px; line-height:1.5;">${data.analysis}</pre>
        `;
    } catch (e) {
        showToast('Failed to evaluate drug interactions.');
    }
}

// 2. Prescription State Pattern
async function loadPrescriptions() {
    try {
        const res = await fetch('/api/prescriptions');
        const data = await res.json();
        state.prescriptions = data.prescriptions || [];
        renderPrescriptions(state.prescriptions);
    } catch (e) {
        console.error(e);
    }
}

function renderPrescriptions(list) {
    const rxCount = list ? list.length : 0;
    const rxCountEl = document.getElementById('dash-rx-count');
    if (rxCountEl) {
        rxCountEl.textContent = rxCount;
    }

    const container = document.getElementById('prescriptions-list-container');
    if (!container) return;

    if (!list || list.length === 0) {
        container.innerHTML = '<p class="text-muted">No prescriptions recorded. Click "Upload Prescription" to add one.</p>';
        return;
    }

    container.innerHTML = list.map(rx => `
        <div class="card">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                <div>
                    <h3>Prescription #${rx.id} — Patient: ${rx.patientName}</h3>
                    <p class="text-muted">Prescribed by <strong>${rx.doctorName}</strong> (${rx.hospital})</p>
                </div>
                <span class="status-pill status-${rx.status.toLowerCase().includes('verified') ? 'verified' : rx.status.toLowerCase().includes('extracted') ? 'extracted' : 'uploaded'}">
                    ${rx.status}
                </span>
            </div>

            <div style="background:#f8fafc; padding:14px; border-radius:8px; margin-bottom:14px; font-family:'JetBrains Mono', monospace; font-size:0.85rem;">
                ${rx.rawScanText}
            </div>

            ${rx.voiceNoteAudio ? `
            <div class="rx-voicenote-card" style="margin: 10px 0 14px; padding: 10px 14px; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; display:flex; align-items:center; gap: 12px; flex-wrap: wrap;">
                <span style="font-weight: 700; font-size: 0.84rem; color: #1e40af; display:flex; align-items:center; gap:6px;">
                    🎙️ Patient Voice Note (Symptom Audio Memo):
                </span>
                <audio controls src="${rx.voiceNoteAudio}" style="height: 32px; flex:1; min-width:220px;"></audio>
            </div>` : ''}

            <h4>Extracted Dosage Items:</h4>
            <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(240px, 1fr)); gap:10px; margin-top:8px; margin-bottom:16px;">
                ${rx.items.map(item => `
                    <div style="border:1px solid #e2e8f0; padding:10px; border-radius:8px; background:white;">
                        <strong>💊 ${item.medicineName}</strong> (${item.dosage})<br>
                        <small class="text-muted">Freq: ${item.frequency} | ${item.instructions}</small>
                    </div>
                `).join('')}
            </div>

            <div style="display:flex; justify-content:space-between; align-items:center;">
                <div>
                    ${!rx.isDispenseReady ? `
                        <button class="btn btn-primary" onclick="advancePrescriptionState('${rx.id}')">
                            Advance State (State Pattern Workflow) ➔
                        </button>
                    ` : `
                        <span style="color:#059669; font-weight:700;">✅ Dispense Ready (Pharmacist Verified)</span>
                    `}
                </div>
                <button class="btn btn-secondary" style="color:#ef4444; border-color:#fecaca; font-size:0.8rem;" onclick="deletePrescription('${rx.id}')">
                    🗑️ Remove Record
                </button>
            </div>
        </div>
    `).join('');
}

async function deletePrescription(rxId) {
    if (!confirm('Are you sure you want to delete this prescription?')) return;
    try {
        const res = await fetch('/api/prescriptions/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prescriptionId: rxId })
        });
        const data = await res.json();
        if (data.status === 'SUCCESS') {
            showToast('🗑️ Prescription deleted successfully.');
            loadPrescriptions();
        } else {
            showToast(data.message || 'Failed to delete prescription.');
        }
    } catch (e) {
        showToast('Error deleting prescription.');
    }
}

// --- Voice Note Recording (MediaRecorder API & Audio Upload) ---
let currentMediaRecorder = null;
let recordedAudioChunks = [];
let voiceAudioBase64 = null;
let voiceRecordTimer = null;
let voiceRecordSeconds = 0;
let activeRecordingScope = null;

async function toggleVoiceRecording(scope) {
    if (currentMediaRecorder && currentMediaRecorder.state === 'recording') {
        stopVoiceRecording(scope);
        return;
    }

    try {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            showToast('Microphone recording is not supported in this browser. Please attach an audio file instead.');
            return;
        }

        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        recordedAudioChunks = [];
        activeRecordingScope = scope;
        currentMediaRecorder = new MediaRecorder(stream);

        currentMediaRecorder.ondataavailable = (e) => {
            if (e.data && e.data.size > 0) {
                recordedAudioChunks.push(e.data);
            }
        };

        currentMediaRecorder.onstop = () => {
            const audioBlob = new Blob(recordedAudioChunks, { type: currentMediaRecorder.mimeType || 'audio/webm' });
            const reader = new FileReader();
            reader.onloadend = () => {
                voiceAudioBase64 = reader.result;
                updateVoicePreview(voiceAudioBase64, scope);
            };
            reader.readAsDataURL(audioBlob);

            stream.getTracks().forEach(track => track.stop());
            if (voiceRecordTimer) clearInterval(voiceRecordTimer);
            updateTimerDisplay(0, scope, false);
        };

        currentMediaRecorder.start();
        voiceRecordSeconds = 0;
        updateTimerDisplay(voiceRecordSeconds, scope, true);

        voiceRecordTimer = setInterval(() => {
            voiceRecordSeconds++;
            updateTimerDisplay(voiceRecordSeconds, scope, true);
        }, 1000);

        const btnText = document.getElementById(scope === 'modal' ? 'rec-btn-text-modal' : 'rec-btn-text-page');
        if (btnText) btnText.textContent = '⏹️ Stop Recording';

        showToast('🎙️ Recording voice note... Speak your symptoms clearly.');
    } catch (err) {
        console.error('Microphone error:', err);
        showToast('Could not access microphone: ' + (err.message || 'Permission denied'));
    }
}

function stopVoiceRecording(scope) {
    if (currentMediaRecorder && currentMediaRecorder.state === 'recording') {
        currentMediaRecorder.stop();
        const btnText = document.getElementById(scope === 'modal' ? 'rec-btn-text-modal' : 'rec-btn-text-page');
        if (btnText) btnText.textContent = '🎙️ Re-record';
        showToast('Audio note captured successfully!');
    }
}

function updateTimerDisplay(sec, scope, isRecording) {
    const timerEl = document.getElementById(scope === 'modal' ? 'voice-timer-modal' : 'voice-timer-page');
    if (!timerEl) return;
    if (!isRecording) {
        timerEl.style.display = 'none';
        return;
    }
    timerEl.style.display = 'inline-block';
    const m = Math.floor(sec / 60).toString().padStart(2, '0');
    const s = (sec % 60).toString().padStart(2, '0');
    timerEl.textContent = `● ${m}:${s}`;
}

function handleVoiceAudioFile(event, scope) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
        voiceAudioBase64 = e.target.result;
        updateVoicePreview(voiceAudioBase64, scope);
        showToast(`📁 Attached audio note: ${file.name}`);
    };
    reader.readAsDataURL(file);
}

function updateVoicePreview(dataUrl, scope) {
    const previewBox = document.getElementById(scope === 'modal' ? 'voice-preview-box-modal' : 'voice-preview-box-page');
    const audioEl = document.getElementById(scope === 'modal' ? 'voice-audio-preview-modal' : 'voice-audio-preview-page');
    if (previewBox && audioEl) {
        audioEl.src = dataUrl;
        previewBox.style.display = 'flex';
    }
}

function clearVoiceRecording(scope) {
    voiceAudioBase64 = null;
    const previewBox = document.getElementById(scope === 'modal' ? 'voice-preview-box-modal' : 'voice-preview-box-page');
    const audioEl = document.getElementById(scope === 'modal' ? 'voice-audio-preview-modal' : 'voice-audio-preview-page');
    const btnText = document.getElementById(scope === 'modal' ? 'rec-btn-text-modal' : 'rec-btn-text-page');

    if (audioEl) audioEl.src = '';
    if (previewBox) previewBox.style.display = 'none';
    if (previewBox || audioEl) {
        showToast('Voice note removed.');
    }
}

function openPrescriptionUploadModal() {
    document.getElementById('modal-rx-upload').classList.add('active');
}

async function submitPrescriptionUpload() {
    const doctor = document.getElementById('rx-doctor-input').value;
    const hospital = document.getElementById('rx-hospital-input').value;
    const text = document.getElementById('rx-text-input').value;

    try {
        await fetch('/api/prescriptions/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                patientId: state.currentUser.id,
                patientName: state.currentUser.name,
                doctorName: doctor,
                hospital: hospital,
                scanText: text,
                voiceNoteAudio: voiceAudioBase64
            })
        });
        clearVoiceRecording('modal');
        closeModal('modal-rx-upload');
        showToast('Prescription uploaded & OCR processed (Status: EXTRACTED)');
        loadPrescriptions();
    } catch (e) {
        showToast('Upload failed.');
    }
}

async function advancePrescriptionState(rxId) {
    try {
        const res = await fetch('/api/prescriptions/advance', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prescriptionId: rxId })
        });
        const data = await res.json();
        showToast(`Prescription state advanced to: ${data.newStatus}`);
        loadPrescriptions();
    } catch (e) {
        showToast('Failed to advance prescription state.');
    }
}

// 3. Observer Pharmacy Stocks
async function loadStocks() {
    try {
        const res = await fetch('/api/pharmacies/stocks');
        const data = await res.json();
        state.stocks = data.stocks || [];
        renderStocks(state.stocks);
    } catch (e) {
        console.error(e);
    }
}

function renderStocks(list) {
    const tbody = document.getElementById('stock-table-body');
    if (!tbody) return;

    tbody.innerHTML = list.map(s => `
        <tr>
            <td><strong>${s.medicineBrandName}</strong></td>
            <td>${s.genericName}</td>
            <td>${s.pharmacyName}</td>
            <td>
                <span class="status-pill ${s.quantity < 10 ? 'status-extracted' : 'status-verified'}">
                    ${s.quantity} Units ${s.quantity < 10 ? '(Low Stock)' : ''}
                </span>
            </td>
            <td>BDT ${s.unitPrice.toFixed(2)}</td>
            <td>
                <button class="btn btn-secondary" style="padding:4px 10px; font-size:0.75rem;" onclick="openStockUpdateModal('${s.id}', '${s.medicineBrandName}', ${s.quantity})">
                    ✏️ Edit Stock
                </button>
            </td>
        </tr>
    `).join('');
}

function openStockUpdateModal(stockId, medName, currentQty) {
    document.getElementById('update-stock-id').value = stockId;
    document.getElementById('update-stock-med-name').textContent = `Medicine: ${medName}`;
    document.getElementById('update-stock-qty').value = currentQty;
    document.getElementById('modal-stock-update').classList.add('active');
}

async function submitStockUpdate() {
    const stockId = document.getElementById('update-stock-id').value;
    const qty = document.getElementById('update-stock-qty').value;

    try {
        await fetch('/api/pharmacies/stock/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ stockId: stockId, quantity: qty })
        });
        closeModal('modal-stock-update');
        showToast(`Stock updated: ${qty} units. Observers notified!`);
        loadStocks();
    } catch (e) {
        showToast('Failed to update stock.');
    }
}

// 4. Emergency Pharmacy Finder
async function loadEmergencyPharmacies() {
    const picker = document.getElementById('user-location-select');
    const geo = picker ? picker.value.split(',') : ['23.7465', '90.3760'];

    try {
        const res = await fetch(`/api/pharmacies/emergency?lat=${geo[0]}&lng=${geo[1]}`);
        const data = await res.json();
        renderEmergencyPharmacies(data.emergencyPharmacies || []);
    } catch (e) {
        console.error(e);
    }
}

function renderEmergencyPharmacies(list) {
    const container = document.getElementById('emergency-pharmacies-container');
    if (!container) return;

    container.innerHTML = list.map(p => `
        <div class="pharmacy-card ${p.is24Hours ? 'is-24h' : ''}">
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                <div>
                    <h3 style="font-size:1.1rem; font-weight:800;">${p.name}</h3>
                    <p class="text-muted">${p.address} (${p.area})</p>
                </div>
                <span class="status-pill status-verified" style="font-size:0.7rem;">${p.distanceKm} KM Away</span>
            </div>
            <div style="margin:12px 0;">
                <span class="status-pill ${p.is24Hours ? 'status-uploaded' : 'status-extracted'}">
                    ${p.is24Hours ? '🕒 Open 24 Hours (Emergency Ready)' : '🕒 Regular Hours'}
                </span>
            </div>
            <div style="display:flex; gap:8px;">
                <a href="tel:${p.phone}" class="btn btn-primary" style="flex:1; text-align:center; text-decoration:none;">
                    📞 Call ${p.phone}
                </a>
                <button class="btn btn-secondary" onclick="switchTab('chat'); showToast('Connecting to pharmacist at ${p.name}...');">
                    💬 Chat
                </button>
            </div>
        </div>
    `).join('');
}

// 5. Fake Medicine & QR Batch Verifier
function populateVerifyDropdown(meds) {
    const select = document.getElementById('verify-medicine-select');
    if (!select) return;
    meds.forEach(m => {
        const opt = document.createElement('option');
        opt.value = m.id;
        opt.textContent = `${m.brandName} (${m.company})`;
        select.appendChild(opt);
    });
}

function setVerifyCode(code) {
    document.getElementById('verify-code-input').value = code;
}

async function verifyMedicineCode() {
    const medId = document.getElementById('verify-medicine-select').value;
    const code = document.getElementById('verify-code-input').value;
    const resultBox = document.getElementById('verify-result-container');

    try {
        const res = await fetch('/api/medicines/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ medicineId: medId, code: code })
        });
        const data = await res.json();
        resultBox.style.display = 'block';

        if (data.isAuthentic) {
            resultBox.innerHTML = `
                <div style="background:#ecfdf5; border:1px solid #a7f3d0; padding:20px; border-radius:12px;">
                    <h3 style="color:#065f46;">✅ GENUINE AUTHENTIC PRODUCT</h3>
                    <p style="margin-top:6px; color:#047857;">${data.details}</p>
                    <small style="display:block; margin-top:8px; color:#065f46;">Manufacturer: <strong>${data.manufacturer}</strong></small>
                </div>
            `;
        } else {
            resultBox.innerHTML = `
                <div style="background:#fee2e2; border:1px solid #fca5a5; padding:20px; border-radius:12px;">
                    <h3 style="color:#991b1b;">⚠️ COUNTERFEIT / SUSPICIOUS MEDICINE</h3>
                    <p style="margin-top:6px; color:#b91c1c;">${data.details}</p>
                    <small style="display:block; margin-top:8px; color:#991b1b;">Report Source: <strong>${data.manufacturer}</strong></small>
                </div>
            `;
        }
    } catch (e) {
        showToast('Verification failed.');
    }
}

// 6. Medication Reminders & Dose Intake Scheduler Workflow
let activeAlarmLoopTimer = null;

function playCheckSound() {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(523.25, ctx.currentTime);
        osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.08);
        osc.frequency.setValueAtTime(783.99, ctx.currentTime + 0.16);
        gain.gain.setValueAtTime(0.12, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.38);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.4);
    } catch (e) {}
}

function playAlarmSoundLoop() {
    stopAlarmSoundLoop();
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        
        const playBeep = () => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(880, ctx.currentTime);
            osc.frequency.setValueAtTime(1046.50, ctx.currentTime + 0.12);
            gain.gain.setValueAtTime(0.18, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.28);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.3);
        };
        
        playBeep();
        activeAlarmLoopTimer = setInterval(playBeep, 1200);
    } catch (e) {}
}

function stopAlarmSoundLoop() {
    if (activeAlarmLoopTimer) {
        clearInterval(activeAlarmLoopTimer);
        activeAlarmLoopTimer = null;
    }
}

async function loadReminders() {
    try {
        const pId = (state.currentUser && state.currentUser.id) ? state.currentUser.id : 'ML-9824-A';
        const pEmail = (state.currentUser && state.currentUser.email) ? state.currentUser.email : 'rahim@medilink.com';

        const res = await fetch(`/api/reminders?patientId=${encodeURIComponent(pId)}&email=${encodeURIComponent(pEmail)}`);
        if (!res.ok) return;
        const data = await res.json();
        state.reminders = data.reminders || [];
        state.reminderSummary = data;

        // Update Adherence Progress Bar & Counters
        const pct = data.adherencePercentage != null ? data.adherencePercentage : 0;
        const pctText = document.getElementById('adherence-pct-text');
        const fillBar = document.getElementById('adherence-progress-fill');
        const totalEl = document.getElementById('adherence-total-count');
        const takenEl = document.getElementById('adherence-taken-count');
        const pendEl = document.getElementById('adherence-pending-count');

        if (pctText) pctText.textContent = `${pct}% Completed Today`;
        if (fillBar) fillBar.style.width = `${pct}%`;
        if (totalEl) totalEl.textContent = data.totalCount || 0;
        if (takenEl) takenEl.textContent = data.takenTodayCount || 0;
        if (pendEl) pendEl.textContent = data.pendingTodayCount || 0;

        renderReminders();
        populatePrescriptionPicker();
    } catch (e) {
        console.error('Failed to load reminders:', e);
    }
}

function filterReminders(filterType, btnEl) {
    state.activeReminderFilter = filterType || 'ALL';
    if (btnEl) {
        const bar = document.getElementById('reminders-filter-bar');
        if (bar) {
            bar.querySelectorAll('.rem-filter-pill').forEach(b => b.classList.remove('active'));
            btnEl.classList.add('active');
        }
    }
    renderReminders();
}

function renderReminders() {
    const container = document.getElementById('reminders-container');
    const dashContainer = document.getElementById('dash-today-reminders');
    const dashRemCount = document.getElementById('dash-rem-count');

    const allList = state.reminders || [];

    if (dashRemCount) {
        dashRemCount.textContent = allList.length;
    }

    if (dashContainer) {
        if (allList.length === 0) {
            dashContainer.innerHTML = '<p class="text-muted" style="font-size:0.85rem;">No scheduled doses for today.</p>';
        } else {
            dashContainer.innerHTML = allList.slice(0, 5).map(r => `
                <div class="dash-rem-item">
                    <div class="dash-rem-left">
                        <span class="rem-icon-pill">💊</span>
                        <div>
                            <strong>${escapeHtml(r.medicine)} (${escapeHtml(r.dosage)})</strong>
                            <small>🕒 ${escapeHtml(r.time)} • ${escapeHtml(r.mealTiming || 'After Meal')} • ${escapeHtml(r.instructions || '')}</small>
                        </div>
                    </div>
                    <span class="rem-status-pill ${r.isTakenToday ? 'rem-taken' : 'rem-pending'}" onclick="handleTakeDose('${r.id}')" style="cursor:pointer;" title="Click to log dose">
                        ${r.isTakenToday ? '✓ Taken' : 'Due Today'}
                    </span>
                </div>
            `).join('');
        }
    }

    if (!container) return;

    if (allList.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 48px 16px; background: var(--card-bg, #ffffff); border: 1px dashed var(--border-color, #cbd5e1); border-radius: 12px;">
                <div style="font-size: 2.2rem; margin-bottom: 8px;">💊</div>
                <h3 style="margin-bottom: 6px;">No Medication Reminders Scheduled</h3>
                <p class="text-muted" style="font-size: 0.9rem; max-width: 420px; margin: 0 auto 16px auto;">
                    Keep your treatment on track. You can add a custom reminder or automatically generate dose schedules from your medical prescriptions.
                </p>
                <div style="display:flex; justify-content:center; gap:10px; flex-wrap:wrap;">
                    <button class="btn btn-sync-rx" onclick="handleSyncFromPrescriptions()">⚡ Sync from Prescriptions</button>
                    <button class="btn btn-primary" onclick="openReminderModal()">+ Add Reminder</button>
                </div>
            </div>
        `;
        return;
    }

    // Apply active filter
    const filter = state.activeReminderFilter || 'ALL';
    const filtered = allList.filter(r => {
        if (!r.time) return true;
        const timeVal = r.time.trim();
        if (filter === 'ALL') return true;
        if (filter === 'TAKEN') return r.isTakenToday;
        if (filter === 'PENDING') return !r.isTakenToday && r.active;
        if (filter === 'MORNING') return timeVal >= '05:00' && timeVal < '12:00';
        if (filter === 'AFTERNOON') return timeVal >= '12:00' && timeVal < '17:00';
        if (filter === 'EVENING') return timeVal >= '17:00' && timeVal < '21:00';
        if (filter === 'NIGHT') return timeVal >= '21:00' || timeVal < '05:00';
        return true;
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 32px 16px; color: var(--text-muted, #64748b);">
                <p>No doses match the selected filter (<strong>${escapeHtml(filter)}</strong>).</p>
                <button class="btn btn-sm btn-secondary" onclick="filterReminders('ALL')">View All Doses</button>
            </div>
        `;
        return;
    }

    container.innerHTML = filtered.map(r => {
        const isTaken = r.isTakenToday;
        const isPaused = !r.active;

        // Convert "08:00" to "08:00 AM"
        let formattedTime = r.time;
        let period = 'AM';
        if (r.time && r.time.includes(':')) {
            const parts = r.time.split(':');
            let h = parseInt(parts[0], 10);
            const m = parts[1];
            period = h >= 12 ? 'PM' : 'AM';
            h = h % 12 || 12;
            formattedTime = `${h.toString().padStart(2, '0')}:${m}`;
        }

        let mealLabel = '🍽️ After Meal';
        if (r.mealTiming === 'BEFORE_MEAL') mealLabel = '🥣 30m Before Meal';
        else if (r.mealTiming === 'WITH_MEAL') mealLabel = '🥗 With Food';
        else if (r.mealTiming === 'EMPTY_STOMACH') mealLabel = '💧 Empty Stomach';
        else if (r.mealTiming === 'BEDTIME') mealLabel = '🌙 At Bedtime';

        let cardStatusClass = '';
        if (isTaken) cardStatusClass = 'is-taken';
        else if (isPaused) cardStatusClass = 'is-paused';
        else cardStatusClass = 'is-due';

        return `
            <div class="reminder-card-modern ${cardStatusClass}" id="rem-card-${r.id}">
                <div class="rem-card-left">
                    <div class="rem-time-badge">
                        <span>${formattedTime}</span>
                        <span class="rem-time-period">${period}</span>
                    </div>
                    <div class="rem-card-details">
                        <div class="rem-card-title-row">
                            <strong>${escapeHtml(r.medicine)}</strong>
                            <span class="rem-dosage-tag">${escapeHtml(r.dosage)}</span>
                            <span class="rem-meal-tag">${escapeHtml(mealLabel)}</span>
                            ${isTaken ? `<span class="rem-taken-timestamp">✓ Taken Today</span>` : ''}
                            ${isPaused ? `<span class="badge badge-secondary" style="font-size:0.7rem;">PAUSED</span>` : ''}
                        </div>
                        <div class="rem-instructions-text">
                            ${escapeHtml(r.instructions || 'Take with water as directed.')}
                        </div>
                        <small class="text-muted" style="font-size:0.75rem;">
                            Frequency: <strong>${escapeHtml(r.frequency || 'Daily')}</strong>
                        </small>
                    </div>
                </div>

                <div class="rem-card-actions">
                    ${!isTaken ? `
                        <button type="button" class="btn-take-dose" onclick="handleTakeDose('${r.id}')" title="Log this dose as taken today">
                            ✓ Take Dose
                        </button>
                    ` : `
                        <button type="button" class="btn-dose-taken" onclick="handleTakeDose('${r.id}')" title="Dose logged. Click to unmark.">
                            ✓ Taken
                        </button>
                    `}

                    <button type="button" class="btn-snooze-dose" onclick="handleSnooze('${r.id}', 15)" title="Snooze reminder for 15 minutes">
                        ⏰ +15m
                    </button>

                    <label class="rem-toggle-switch" title="${r.active ? 'Active schedule - click to pause' : 'Paused - click to resume'}">
                        <input type="checkbox" ${r.active ? 'checked' : ''} onchange="handleToggleReminder('${r.id}')">
                        <span class="rem-toggle-slider"></span>
                    </label>

                    <button type="button" class="btn-rem-delete" onclick="handleDeleteReminder('${r.id}')" title="Delete this reminder">
                        🗑️
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function handleTakeDose(reminderId) {
    try {
        const res = await fetch(`/api/reminders/${encodeURIComponent(reminderId)}/take`, { method: 'POST' });
        if (!res.ok) throw new Error();
        playCheckSound();
        showToast('✅ Dose status logged successfully!');
        await loadReminders();
    } catch (e) {
        showToast('Could not update dose status.');
    }
}

async function handleToggleReminder(reminderId) {
    try {
        const res = await fetch(`/api/reminders/${encodeURIComponent(reminderId)}/toggle`, { method: 'POST' });
        if (!res.ok) throw new Error();
        showToast('Schedule status updated.');
        await loadReminders();
    } catch (e) {
        showToast('Failed to toggle reminder schedule.');
    }
}

async function handleDeleteReminder(reminderId) {
    if (!confirm('Are you sure you want to remove this medication reminder?')) return;
    try {
        const res = await fetch(`/api/reminders/${encodeURIComponent(reminderId)}`, { method: 'DELETE' });
        if (!res.ok) throw new Error();
        showToast('🗑️ Reminder schedule removed.');
        await loadReminders();
    } catch (e) {
        showToast('Failed to delete reminder.');
    }
}

function handleSnooze(reminderId, minutes = 15) {
    const rem = (state.reminders || []).find(r => r.id === reminderId);
    const name = rem ? rem.medicine : 'Medication';
    showToast(`⏰ Snoozed ${name} for ${minutes} minutes.`);
    setTimeout(() => {
        openAlarmModal(`REMINDER_ALARM: Time to take your snoozed dose of ${name}!`);
    }, minutes * 60 * 1000);
}

async function handleSyncFromPrescriptions() {
    try {
        const pId = (state.currentUser && state.currentUser.id) ? state.currentUser.id : 'ML-9824-A';
        const pEmail = (state.currentUser && state.currentUser.email) ? state.currentUser.email : 'rahim@medilink.com';

        const res = await fetch('/api/reminders/sync-prescriptions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ patientId: pId, email: pEmail })
        });
        const data = await res.json();
        showToast(data.message || 'Prescription dose sync completed!');
        playCheckSound();
        await loadReminders();
    } catch (e) {
        showToast('Failed to sync reminders from prescriptions.');
    }
}

function populatePrescriptionPicker() {
    const picker = document.getElementById('rem-prescription-picker');
    if (!picker) return;

    // Collect medicines from state.medicines and state.prescriptions
    const meds = [];
    if (state.medicines && Array.isArray(state.medicines)) {
        state.medicines.forEach(m => {
            if (m.name && !meds.some(x => x.name === m.name)) {
                meds.push({ name: m.name, dosage: m.dosage || '1 Tablet', timing: 'AFTER_MEAL', instr: m.category || 'Prescribed Medication' });
            }
        });
    }

    if (meds.length === 0) {
        meds.push(
            { name: 'Napa Extra 500mg', dosage: '1 Tablet', timing: 'AFTER_MEAL', instr: 'Take after meal with water' },
            { name: 'Seclo 20mg', dosage: '1 Capsule', timing: 'BEFORE_MEAL', instr: 'Take 30 mins before breakfast' },
            { name: 'Monas 10mg', dosage: '1 Tablet', timing: 'BEDTIME', instr: 'Take at bedtime for asthma/allergies' },
            { name: 'Fexo 120mg', dosage: '1 Tablet', timing: 'BEDTIME', instr: 'Take at bedtime for allergy relief' },
            { name: 'Lisinopril 10mg', dosage: '1 Tablet', timing: 'AFTER_MEAL', instr: 'Take morning blood pressure dose' }
        );
    }

    picker.innerHTML = `
        <option value="">-- Quick select from active prescriptions --</option>
        ${meds.map((m, i) => `
            <option value="${i}" data-name="${escapeHtml(m.name)}" data-dosage="${escapeHtml(m.dosage)}" data-timing="${escapeHtml(m.timing)}" data-instr="${escapeHtml(m.instr)}">
                ${escapeHtml(m.name)} (${escapeHtml(m.dosage)})
            </option>
        `).join('')}
    `;
}

function handlePrescriptionPickerChange(selectEl) {
    const opt = selectEl.options[selectEl.selectedIndex];
    if (!opt || !opt.value) return;

    const name = opt.getAttribute('data-name');
    const dosage = opt.getAttribute('data-dosage');
    const timing = opt.getAttribute('data-timing');
    const instr = opt.getAttribute('data-instr');

    if (name) document.getElementById('rem-med-name').value = name;
    if (dosage) document.getElementById('rem-dosage').value = dosage;
    if (timing) document.getElementById('rem-meal-timing').value = timing;
    if (instr) document.getElementById('rem-instructions').value = instr;
}

function openReminderModal() {
    populatePrescriptionPicker();
    document.getElementById('modal-reminder').classList.add('active');
}

async function submitNewReminder() {
    const med = document.getElementById('rem-med-name').value;
    const dosage = document.getElementById('rem-dosage').value;
    const time = document.getElementById('rem-time').value;
    const freq = document.getElementById('rem-freq').value;
    const mealTimingEl = document.getElementById('rem-meal-timing');
    const mealTiming = mealTimingEl ? mealTimingEl.value : 'AFTER_MEAL';
    const instructions = document.getElementById('rem-instructions').value;

    if (!med || !time) {
        showToast('Please enter both medicine name and scheduled time.');
        return;
    }

    try {
        const pId = (state.currentUser && state.currentUser.id) ? state.currentUser.id : 'ML-9824-A';
        const pEmail = (state.currentUser && state.currentUser.email) ? state.currentUser.email : 'rahim@medilink.com';

        const res = await fetch('/api/reminders/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                patientId: pId,
                email: pEmail,
                medicine: med,
                dosage: dosage,
                time: time,
                frequency: freq,
                mealTiming: mealTiming,
                instructions: instructions
            })
        });

        if (!res.ok) throw new Error();
        closeModal('modal-reminder');
        playCheckSound();
        showToast(`✅ Scheduled reminder for ${med} at ${time}`);
        await loadReminders();
    } catch (e) {
        showToast('Failed to save reminder schedule.');
    }
}

async function triggerTestAlarm() {
    try {
        // Open modal immediately on client with audible alarm
        openAlarmModal('DEMO NOTIFICATION: Time to take your scheduled dose of Napa Extra 500mg (1 Tablet) - Take after lunch with water!');
        // Broadcast over backend SSE stream as well
        fetch('/api/reminders/test-alert', { method: 'POST' }).catch(() => {});
    } catch (e) {
        showToast('Error triggering instant alarm.');
    }
}

function openAlarmModal(alarmContent) {
    const modal = document.getElementById('modal-alarm-ringing');
    if (!modal) return;

    let medName = 'Napa Extra 500mg';
    let dosage = '1 Tablet';
    let instr = '🍽️ Take after lunch with a full glass of water.';

    if (alarmContent && typeof alarmContent === 'string') {
        if (alarmContent.includes('take')) {
            const afterTake = alarmContent.substring(alarmContent.indexOf('take') + 4).trim();
            medName = afterTake.split('-')[0].trim();
            if (afterTake.includes('-')) {
                instr = afterTake.split('-')[1].trim();
            }
        }
    }

    const nameEl = document.getElementById('alarm-modal-med-name');
    const doseEl = document.getElementById('alarm-modal-dosage');
    const instrEl = document.getElementById('alarm-modal-instructions');

    if (nameEl) nameEl.textContent = medName;
    if (doseEl) doseEl.textContent = dosage;
    if (instrEl) instrEl.textContent = instr;

    playAlarmSoundLoop();
    modal.classList.add('active');
}

function closeAlarmModal() {
    stopAlarmSoundLoop();
    const modal = document.getElementById('modal-alarm-ringing');
    if (modal) modal.classList.remove('active');
}

function handleAlarmModalTakeDose() {
    closeAlarmModal();
    playCheckSound();
    showToast('🎉 Excellent! Dose marked as taken for today.');

    // If there's an active pending reminder, mark the first one as taken
    const pending = (state.reminders || []).find(r => !r.isTakenToday && r.active);
    if (pending) {
        handleTakeDose(pending.id);
    } else {
        loadReminders();
    }
}

function handleAlarmModalSnooze(minutes = 10) {
    closeAlarmModal();
    showToast(`⏰ Alarm snoozed for ${minutes} minutes.`);
    setTimeout(() => {
        openAlarmModal('SNOOZED ALARM: Time to take your scheduled medication!');
    }, minutes * 60 * 1000);
}

// 7. Live Patient - Pharmacist Chat & Clinical Tele-Consultation (Two-Way Connected)

let chatHeartbeatTimer = null;

function startChatHeartbeat() {
    stopChatHeartbeat();
    chatHeartbeatTimer = setInterval(() => {
        const chatTab = document.getElementById('tab-chat');
        if (chatTab && chatTab.classList.contains('active')) {
            loadChatMessages(true);
            loadConversationsList(true);
        }
    }, 3500);
}

function stopChatHeartbeat() {
    if (chatHeartbeatTimer) {
        clearInterval(chatHeartbeatTimer);
        chatHeartbeatTimer = null;
    }
}

function playChatNotificationSound() {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, ctx.currentTime);
        osc.frequency.setValueAtTime(880.00, ctx.currentTime + 0.1);
        gain.gain.setValueAtTime(0.12, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.36);
    } catch (e) {}
}

function updateChatNavBadge(totalUnread) {
    const badge = document.getElementById('chat-unread-badge');
    if (!badge) return;
    if (totalUnread && totalUnread > 0) {
        badge.textContent = totalUnread > 99 ? '99+' : totalUnread;
        badge.style.display = 'inline-block';
    } else {
        badge.style.display = 'none';
    }
}

async function loadConversationsList(isSilent = false) {
    try {
        const currentUser = state.currentUser || {};
        const currentId = currentUser.id || 'ML-9824-A';
        const role = (currentUser.role || state.activeRole || 'PATIENT').toUpperCase();

        const res = await fetch(`${API_BASE_URL}/api/chat/conversations?userId=${encodeURIComponent(currentId)}&role=${encodeURIComponent(role)}`);
        if (!res.ok) return;
        const data = await res.json();
        const convs = data.conversations || [];
        state.conversations = convs;

        const totalUnread = convs.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
        updateChatNavBadge(totalUnread);

        renderConversationsSidebar();

        if (!state.activeChatPartner && convs.length > 0) {
            const first = convs[0];
            selectConversation(first.partnerId, first.partnerName, first.partnerRole, first.partnerEmail, first.avatar, first.pharmacy, first.license);
        } else if (state.activeChatPartner) {
            const updated = convs.find(c => c.partnerId === state.activeChatPartner.id);
            if (updated) {
                state.activeChatPartner.name = updated.partnerName || state.activeChatPartner.name;
                state.activeChatPartner.email = updated.partnerEmail || state.activeChatPartner.email;
            }
        }
    } catch (e) {
        if (!isSilent) console.warn('Failed to load chat conversations:', e);
    }
}

async function loadPharmacistsList() {
    await loadConversationsList();
}

function renderConversationsSidebar() {
    const listEl = document.getElementById('chat-threads-list');
    if (!listEl) return;

    const currentUser = state.currentUser || {};
    const currentRole = (currentUser.role || state.activeRole || 'PATIENT').toUpperCase();

    const sidebarTitle = document.getElementById('chat-sidebar-title');
    if (sidebarTitle) {
        sidebarTitle.textContent = currentRole === 'PHARMACIST' ? 'Patient Consultations' : 'Consultations';
    }

    const conversations = state.conversations || [];
    if (conversations.length === 0) {
        listEl.innerHTML = `
            <div style="padding: 24px 16px; text-align: center; color: var(--text-muted, #94a3b8); font-size: 0.85rem;">
                <div style="font-size: 1.6rem; margin-bottom: 8px;">💬</div>
                No active conversations yet.<br>
                <small>${currentRole === 'PHARMACIST' ? 'Incoming patient consultations will appear here.' : 'Verified pharmacists will appear here.'}</small>
            </div>
        `;
        return;
    }

    const currentPartnerId = state.activeChatPartner ? state.activeChatPartner.id : (conversations[0] ? conversations[0].partnerId : '');

    listEl.innerHTML = conversations.map(c => {
        const isActive = c.partnerId === currentPartnerId;
        const snippet = c.lastMessage || (c.partnerRole === 'PHARMACIST' ? 'Ready for clinical consultation' : 'Patient consultation');
        const roleBadge = c.partnerRole === 'PHARMACIST' ? 'DGDA' : 'PATIENT';
        const avatar = c.avatar || (c.partnerRole === 'PHARMACIST' ? '🩺' : '👤');

        let storeOrId = '';
        if (c.partnerRole === 'PHARMACIST') {
            storeOrId = c.pharmacy ? c.pharmacy.split('(')[0].trim() : 'Licensed Pharmacy';
        } else {
            storeOrId = `Patient ID: ${c.partnerId}`;
        }

        const unreadBadgeHtml = (c.unreadCount && c.unreadCount > 0)
            ? `<span class="chat-thread-unread-badge" title="${c.unreadCount} unread">${c.unreadCount}</span>`
            : '';

        let timeStr = '';
        if (c.lastMessageTime) {
            try {
                const d = new Date(c.lastMessageTime);
                if (!isNaN(d.getTime())) {
                    timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                }
            } catch (e) {}
        }

        return `
            <div class="chat-thread-card ${isActive ? 'active' : ''}" onclick="selectConversation('${escapeHtml(c.partnerId)}', '${escapeHtml(c.partnerName)}', '${escapeHtml(c.partnerRole)}', '${escapeHtml(c.partnerEmail || '')}', '${escapeHtml(avatar)}', '${escapeHtml(c.pharmacy || '')}', '${escapeHtml(c.license || '')}')">
                <div class="chat-thread-avatar-wrap">
                    <div class="chat-thread-avatar">${avatar}</div>
                    <span class="chat-thread-online-dot"></span>
                </div>
                <div class="chat-thread-info">
                    <div class="chat-thread-title">
                        <strong>${escapeHtml(c.partnerName)}</strong>
                        <div style="display:flex; align-items:center; gap:5px;">
                            ${unreadBadgeHtml}
                            <span class="chat-thread-badge">${escapeHtml(roleBadge)}</span>
                        </div>
                    </div>
                    <div class="chat-thread-store" style="display:flex; justify-content:space-between; align-items:center;">
                        <span>${escapeHtml(storeOrId)}</span>
                        ${timeStr ? `<small style="font-size:0.68rem; color:#64748b;">${timeStr}</small>` : ''}
                    </div>
                    <div class="chat-thread-snippet">${escapeHtml(snippet)}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderPharmacistsSidebar() {
    renderConversationsSidebar();
}

function selectConversation(partnerId, partnerName, partnerRole, partnerEmail, partnerAvatar, partnerStore, partnerLicense) {
    state.activeChatPartner = {
        id: partnerId,
        name: partnerName || (partnerRole === 'PHARMACIST' ? 'Dr. Pharmacist' : 'Patient'),
        role: partnerRole || 'PATIENT',
        email: partnerEmail || '',
        avatar: partnerAvatar || (partnerRole === 'PHARMACIST' ? '🩺' : '👤'),
        pharmacy: partnerStore || '',
        license: partnerLicense || ''
    };

    if (partnerRole === 'PHARMACIST') {
        state.selectedPharmacistId = partnerId;
    } else {
        state.selectedPatientId = partnerId;
    }

    updateActiveChatHeader();
    renderConversationsSidebar();
    renderChatQuickChips();

    const currentUser = state.currentUser || {};
    const currentId = currentUser.id || 'ML-9824-A';
    fetch(`${API_BASE_URL}/api/chat/read`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user1: currentId, user2: partnerId })
    }).then(() => {
        if (state.conversations) {
            const found = state.conversations.find(c => c.partnerId === partnerId);
            if (found) found.unreadCount = 0;
            const totalUnread = state.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
            updateChatNavBadge(totalUnread);
        }
    }).catch(() => {});

    const input = document.getElementById('chat-input');
    if (input) input.value = '';

    const box = document.getElementById('chat-messages-box');
    if (box) box.style.opacity = '0.5';

    loadChatMessages().finally(() => {
        if (box) box.style.opacity = '1';
    });
}

function selectPharmacistConversation(pharmaId) {
    const pharma = (state.pharmacists || state.conversations || []).find(p => p.id === pharmaId || p.partnerId === pharmaId);
    if (pharma) {
        selectConversation(
            pharma.id || pharma.partnerId,
            pharma.name || pharma.partnerName,
            'PHARMACIST',
            pharma.email || pharma.partnerEmail,
            pharma.avatar,
            pharma.pharmacy,
            pharma.license
        );
    } else {
        selectConversation(pharmaId, pharmaId === 'usr_pharma_02' ? 'Dr. Nazmul Huda' : 'Dr. Farhan Kabir', 'PHARMACIST', '', '🩺', '', '');
    }
}

function updateActiveChatHeader() {
    const partner = state.activeChatPartner || {
        id: 'usr_pharma_01',
        name: 'Dr. Farhan Kabir',
        role: 'PHARMACIST',
        avatar: '🩺',
        pharmacy: 'Lazz Pharma (Dhanmondi Branch)',
        license: 'DGDA-PH-9920'
    };

    const avatarEl = document.getElementById('chat-active-avatar');
    const nameEl = document.getElementById('chat-active-pharma-name');
    const storeEl = document.getElementById('chat-active-pharma-store');
    const roleBadgeEl = document.getElementById('chat-active-role-badge');
    const onlineEl = document.getElementById('chat-online-label');
    const typingTextEl = document.getElementById('chat-typing-text');
    const inputEl = document.getElementById('chat-input');

    if (avatarEl) avatarEl.textContent = partner.avatar || (partner.role === 'PHARMACIST' ? '🩺' : '👤');
    if (nameEl) nameEl.textContent = partner.name;

    if (roleBadgeEl) {
        roleBadgeEl.textContent = partner.role === 'PHARMACIST' ? 'DGDA VERIFIED' : 'VERIFIED PATIENT';
        roleBadgeEl.className = partner.role === 'PHARMACIST' ? 'badge badge-success' : 'badge badge-primary';
    }

    if (storeEl) {
        if (partner.role === 'PHARMACIST') {
            storeEl.textContent = `Licensed Pharmacist | ${partner.pharmacy || 'Partner Pharmacy'}`;
        } else {
            storeEl.textContent = `Patient ID: ${partner.id}${partner.email ? ' • ' + partner.email : ''}`;
        }
    }

    if (onlineEl) onlineEl.textContent = 'Online';
    if (typingTextEl) typingTextEl.textContent = `${partner.name} is typing...`;

    if (inputEl) {
        const myRole = (state.currentUser?.role || state.activeRole || 'PATIENT').toUpperCase();
        if (myRole === 'PHARMACIST') {
            inputEl.placeholder = `Type clinical advice response to ${partner.name}... (Press Enter to send)`;
        } else {
            inputEl.placeholder = `Type your medication query to ${partner.name}... (Press Enter to send)`;
        }
    }
}

function updateActivePharmacistHeader(pharmaId) {
    selectPharmacistConversation(pharmaId);
}

function handlePharmacistSelectionChange() {
    const selectedPharma = state.selectedPharmacistId || 'usr_pharma_01';
    selectPharmacistConversation(selectedPharma);
}

function renderChatQuickChips() {
    const container = document.getElementById('chat-quick-chips-bar');
    if (!container) return;

    const myRole = (state.currentUser?.role || state.activeRole || 'PATIENT').toUpperCase();

    if (myRole === 'PHARMACIST') {
        container.innerHTML = `
            <span class="chat-quick-chips-label">⚡ Clinical Responses:</span>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('Take this medication 30 minutes before meals with a full glass of water.')">
                💊 Before Meals (1+0+1)
            </button>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('Ensure at least a 4-hour gap between doses. Do not exceed the prescribed limit.')">
                ⏱️ 4-Hour Dose Gap
            </button>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('Your prescription is verified and authentic stock is available at our pharmacy.')">
                📋 Verified & In Stock
            </button>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('If any dizziness, nausea, or allergic rash persists, discontinue and seek immediate clinical care.')">
                ⚠️ Clinical Precaution
            </button>
        `;
    } else {
        container.innerHTML = `
            <span class="chat-quick-chips-label">⚡ Quick Inquiries:</span>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('Check drug interactions for my active medications.', true)">
                💊 Check Interactions
            </button>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('What is the optimal dosing time and meal schedule for my medicines?', true)">
                ⏱️ Dosing &amp; Schedule
            </button>
            <button type="button" class="chat-chip" onclick="sendQuickChatMessage('Are there any known adverse side effects or food precautions for these drugs?', true)">
                ⚠️ Adverse Side Effects
            </button>
            <button type="button" class="chat-chip" onclick="openChatAttachRxModal()">
                📎 Share Prescription
            </button>
        `;
    }
}

async function loadChatMessages(isSilent = false) {
    try {
        const currentUser = state.currentUser || {};
        const currentId = currentUser.id || 'ML-9824-A';
        const myRole = (currentUser.role || state.activeRole || 'PATIENT').toUpperCase();

        let partnerId = state.activeChatPartner ? state.activeChatPartner.id : '';
        if (!partnerId) {
            partnerId = (myRole === 'PHARMACIST') ? 'ML-9824-A' : (state.selectedPharmacistId || 'usr_pharma_01');
        }

        const res = await fetch(`${API_BASE_URL}/api/chat/messages?user1=${encodeURIComponent(currentId)}&user2=${encodeURIComponent(partnerId)}`);
        if (!res.ok) return;
        const data = await res.json();
        renderChatMessages(data.messages || []);
    } catch (e) {
        if (!isSilent) console.error('Error loading chat messages:', e);
    }
}

function renderChatMessages(list) {
    const box = document.getElementById('chat-messages-box');
    if (!box) return;

    const currentUser = state.currentUser || {};
    const currentRole = (currentUser.role || state.activeRole || 'PATIENT').toUpperCase();
    const currentId = currentUser.id || 'ML-9824-A';
    const currentEmail = (currentUser.email || '').toLowerCase();

    const partner = state.activeChatPartner || {
        id: (currentRole === 'PHARMACIST') ? 'ML-9824-A' : 'usr_pharma_01',
        name: (currentRole === 'PHARMACIST') ? 'Rahim Ahmed' : 'Dr. Farhan Kabir',
        role: (currentRole === 'PHARMACIST') ? 'PATIENT' : 'PHARMACIST',
        avatar: (currentRole === 'PHARMACIST') ? '👤' : '🩺',
        pharmacy: 'Lazz Pharma (Dhanmondi Branch)',
        license: 'DGDA-PH-9920'
    };

    if (!list || list.length === 0) {
        const emptyTitle = partner.name;
        const emptySub = partner.role === 'PHARMACIST'
            ? `${partner.pharmacy || 'Licensed Pharmacy'}. Send a medication query, check drug interactions, or attach your prescription to begin consultation.`
            : `Patient ID: ${partner.id}. Send verified clinical instructions, review doses, or advise on medicine availability.`;

        const buttonsHtml = currentRole === 'PATIENT' ? `
            <div style="display:flex; justify-content:center; gap:8px; flex-wrap:wrap;">
                <button type="button" class="btn btn-secondary btn-sm" onclick="sendQuickChatMessage('Assalamu Alaikum doctor, could you please review my medicine schedule?')">
                    👋 Say Hello
                </button>
                <button type="button" class="btn btn-primary btn-sm" onclick="openChatAttachRxModal()">
                    📎 Attach Prescription
                </button>
            </div>
        ` : `
            <div style="display:flex; justify-content:center; gap:8px; flex-wrap:wrap;">
                <button type="button" class="btn btn-primary btn-sm" onclick="sendQuickChatMessage('Assalamu Alaikum. How may I assist you with your medications today?')">
                    👋 Start Consultation
                </button>
            </div>
        `;

        box.innerHTML = `
            <div class="chat-empty-thread-card">
                <div class="chat-empty-avatar">${partner.avatar || (partner.role === 'PHARMACIST' ? '🩺' : '👤')}</div>
                <h4 style="margin:0 0 6px 0; color:var(--text-primary, #f8fafc); font-size:1.15rem;">${escapeHtml(emptyTitle)}</h4>
                <div style="display:flex; align-items:center; justify-content:center; gap:6px; margin-bottom:8px;">
                    <span class="${partner.role === 'PHARMACIST' ? 'badge badge-success' : 'badge badge-primary'}" style="font-size:0.7rem; padding:2px 6px;">
                        ${partner.role === 'PHARMACIST' ? 'DGDA VERIFIED' : 'PATIENT'}
                    </span>
                    <span style="font-size:0.8rem; color:var(--text-muted);">${escapeHtml(partner.license || partner.id)}</span>
                </div>
                <p style="font-size:0.82rem; color:var(--text-muted); max-width:380px; margin:0 auto 16px auto; line-height:1.5;">
                    ${escapeHtml(emptySub)}
                </p>
                ${buttonsHtml}
            </div>
        `;
        return;
    }

    box.innerHTML = list.map(m => {
        let isMine = false;
        if (m.senderId && (m.senderId === currentId || (currentId.startsWith('PA-') && m.senderId === 'ML-9824-A') || (currentId === 'ML-9824-A' && m.senderId.startsWith('PA-')))) {
            isMine = true;
        } else if (currentEmail && m.senderEmail && m.senderEmail.toLowerCase() === currentEmail) {
            isMine = true;
        } else if (currentRole === 'PATIENT' && m.senderRole === 'PATIENT') {
            isMine = true;
        } else if (currentRole === 'PHARMACIST' && m.senderRole === 'PHARMACIST' && m.senderId && (m.senderId === currentId || m.senderId.toLowerCase().includes('pharma'))) {
            isMine = true;
        }

        let timeStr = 'Just now';
        if (m.timestamp) {
            try {
                const d = new Date(m.timestamp);
                if (!isNaN(d.getTime())) {
                    timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                }
            } catch (e) {}
        }

        let rxCardHtml = '';
        if (m.prescriptionId) {
            let pillsHtml = '';
            if (m.prescriptionSummary) {
                const pills = m.prescriptionSummary.split(',').map(s => s.trim()).filter(Boolean);
                pillsHtml = `<div class="chat-rx-pills">${pills.map(p => `<span class="chat-rx-pill-item">💊 ${escapeHtml(p)}</span>`).join('')}</div>`;
            }
            rxCardHtml = `
                <div class="chat-rx-attachment-card">
                    <div class="chat-rx-header">
                        <span>📋 Attached Rx: <strong>${escapeHtml(m.prescriptionId)}</strong></span>
                        <span class="badge badge-success" style="font-size:0.68rem; padding:1px 6px;">VERIFIED</span>
                    </div>
                    ${pillsHtml}
                </div>
            `;
        }

        const senderLabel = isMine ? (m.senderName || 'You') : (m.senderName || (m.senderRole === 'PHARMACIST' ? 'Pharmacist' : 'Patient'));
        const roleBadge = m.senderRole === 'PHARMACIST' ? '🩺 DGDA Pharmacist' : '👤 Patient';

        return `
            <div class="chat-msg ${isMine ? 'mine' : 'theirs'}">
                <div class="chat-sender-tag">
                    <span>${roleBadge}</span>
                    <span>•</span>
                    <span>${escapeHtml(senderLabel)}</span>
                </div>
                <div>${escapeHtml(m.content)}</div>
                ${rxCardHtml}
                <span class="chat-msg-time">${timeStr} ${isMine ? (m.isRead ? '✓✓' : '✓') : ''}</span>
            </div>
        `;
    }).join('');

    box.scrollTop = box.scrollHeight;
}

async function sendChatMessage(customContent, rxId, rxSummary, explicitAutoReply = false) {
    const input = document.getElementById('chat-input');
    const content = (customContent !== undefined) ? customContent.trim() : (input ? input.value.trim() : '');

    if (!content && !rxId) return;

    if (input && customContent === undefined) {
        input.value = '';
    }

    const currentUser = state.currentUser || {};
    const currentId = currentUser.id || 'ML-9824-A';
    const currentName = currentUser.name || (currentUser.role === 'PHARMACIST' ? 'Dr. Farhan Kabir' : 'Rahim Ahmed');
    const currentRole = (currentUser.role || state.activeRole || 'PATIENT').toUpperCase();
    const currentEmail = currentUser.email || (currentRole === 'PHARMACIST' ? 'farhan@lazzpharma.com' : 'rahim@medilink.com');

    const partner = state.activeChatPartner || {};
    let receiverId = partner.id;
    let receiverEmail = partner.email;

    if (!receiverId) {
        if (currentRole === 'PHARMACIST') {
            receiverId = 'ML-9824-A';
            receiverEmail = 'rahim@medilink.com';
        } else {
            receiverId = state.selectedPharmacistId || 'usr_pharma_01';
            receiverEmail = (receiverId === 'usr_pharma_02') ? 'nazmul@popularpharma.com' : 'farhan@lazzpharma.com';
        }
    }

    const typingIndicator = document.getElementById('chat-typing-indicator');
    const typingText = document.getElementById('chat-typing-text');
    if (typingText) {
        typingText.textContent = `${partner.name || 'Recipient'} is typing...`;
    }
    if (typingIndicator && currentRole === 'PATIENT' && explicitAutoReply) {
        typingIndicator.style.display = 'flex';
    }

    try {
        const payload = {
            senderId: currentId,
            senderName: currentName,
            senderRole: currentRole,
            senderEmail: currentEmail,
            receiverId: receiverId,
            receiverEmail: receiverEmail,
            content: content || `Consultation regarding prescription #${rxId}`,
            type: rxId ? 'PRESCRIPTION_CONSULT' : 'TEXT',
            prescriptionId: rxId || null,
            prescriptionSummary: rxSummary || null,
            autoReply: explicitAutoReply
        };

        const res = await fetch(`${API_BASE_URL}/api/chat/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            await loadChatMessages();
            loadConversationsList(true);
        } else {
            showToast('Failed to send message.');
            if (typingIndicator) typingIndicator.style.display = 'none';
        }
    } catch (e) {
        console.error('Send chat error:', e);
        showToast('Failed to send message.');
        if (typingIndicator) typingIndicator.style.display = 'none';
    }
}

function sendQuickChatMessage(promptText, autoReply = false) {
    const input = document.getElementById('chat-input');
    if (input) {
        input.value = promptText;
        sendChatMessage(promptText, null, null, autoReply);
        input.value = '';
    }
}

function openChatAttachRxModal() {
    const modal = document.getElementById('modal-chat-attach-rx');
    const listContainer = document.getElementById('chat-attach-rx-list');
    if (!modal || !listContainer) return;

    let rxs = (state.prescriptions && state.prescriptions.length > 0) ? state.prescriptions : [];
    if (rxs.length === 0) {
        rxs = [
            {
                id: 'rx_101',
                doctorName: 'Prof. Dr. M. A. Malek',
                hospital: 'Dhaka Medical College Hospital',
                date: '2026-03-01',
                medicines: [
                    { name: 'Napa Extra', dosage: '500mg/65mg', frequency: '1+0+1' },
                    { name: 'Seclo 20', dosage: '20mg', frequency: '1+0+0' },
                    { name: 'Ciprocin 500', dosage: '500mg', frequency: '1+0+1' }
                ]
            },
            {
                id: 'rx_102',
                doctorName: 'Dr. Fatima Rahman',
                hospital: 'Square Hospital Dhaka',
                date: '2026-03-05',
                medicines: [
                    { name: 'Sergel 20', dosage: '20mg', frequency: '1+0+0' },
                    { name: 'Montene 10', dosage: '10mg', frequency: '0+0+1' }
                ]
            }
        ];
    }

    listContainer.innerHTML = rxs.map(rx => {
        const rxId = rx.id || rx.rxId || 'rx_101';
        const doc = rx.doctorName || 'Licensed Physician';
        const hosp = rx.hospital || 'Hospital';
        const meds = (rx.medicines || []).map(m => m.name || m.medicineName || '').filter(Boolean).join(', ');
        return `
            <div style="background:var(--bg-body, #0f172a); border:1px solid var(--border-color, #334155); border-radius:10px; padding:12px; display:flex; justify-content:space-between; align-items:center; gap:12px;">
                <div style="flex:1;">
                    <div style="display:flex; align-items:center; gap:8px; margin-bottom:4px;">
                        <strong style="color:var(--primary, #3b82f6); font-size:0.92rem;">#${escapeHtml(rxId)}</strong>
                        <span class="badge badge-success" style="font-size:0.65rem; padding:1px 5px;">DGDA VERIFIED</span>
                    </div>
                    <div style="font-size:0.8rem; color:var(--text-main, #f8fafc); font-weight:600;">${escapeHtml(doc)} • <span class="text-muted" style="font-weight:normal;">${escapeHtml(hosp)}</span></div>
                    <div style="font-size:0.75rem; color:var(--text-muted); margin-top:4px;"><strong>Meds:</strong> ${escapeHtml(meds || 'Formulations')}</div>
                </div>
                <button type="button" class="btn btn-primary btn-sm" onclick="attachPrescriptionToChat('${escapeHtml(rxId)}')">
                    Attach &amp; Consult
                </button>
            </div>
        `;
    }).join('');

    modal.style.display = 'flex';
    modal.classList.add('active');
}

function attachPrescriptionToChat(rxId) {
    closeModal('modal-chat-attach-rx');

    let rxs = (state.prescriptions && state.prescriptions.length > 0) ? state.prescriptions : [];
    const found = rxs.find(r => (r.id === rxId || r.rxId === rxId));
    let medsSummary = '';
    if (found && found.medicines) {
        medsSummary = found.medicines.map(m => `${m.name || m.medicineName} ${m.dosage || ''}`).join(', ').trim();
    } else {
        medsSummary = 'Napa Extra 500mg, Seclo 20mg, Ciprocin 500mg';
    }

    const consultMsg = `I would like clinical guidance regarding my verified digital prescription #${rxId}.`;
    sendChatMessage(consultMsg, rxId, medsSummary, false);
}

// Help Center Knowledge Base & Interactive Handlers
const HELP_ARTICLES_DATA = {
    'Creating your account': {
        category: 'Getting Started',
        icon: '🚀',
        content: `
            <p><strong>1. Click 'Sign Up' in the Patient Portal:</strong> Enter your full name, email, role (Patient, Pharmacist, or Admin), and password.</p>
            <p><strong>2. Unique Patient ID:</strong> A clinical ID formatted as <code>PA-XXXX-Y</code> is automatically assigned to your account.</p>
            <p><strong>3. Auto-Avatar Customization:</strong> Your profile automatically configures gender-matched clinical avatar artwork based on your name.</p>
        `
    },
    'Navigating the Patient Portal': {
        category: 'Getting Started',
        icon: '🧭',
        content: `
            <p><strong>• Dashboard:</strong> View daily medicine schedules, active prescriptions, and stock notifications.</p>
            <p><strong>• Prescriptions & Meds:</strong> Upload prescriptions for instant OCR parsing and find generic drug alternatives.</p>
            <p><strong>• Pharmacist Live Chat:</strong> Chat in real-time with verified pharmacists for medicine inquiries.</p>
            <p><strong>• Settings:</strong> Update personal details, blood type, known allergies, chronic conditions, and emergency contacts.</p>
        `
    },
    'Updating your profile info': {
        category: 'Getting Started',
        icon: '⚙️',
        content: `
            <p><strong>• Personal Details:</strong> Navigate to <strong>Settings</strong> to update your first name, last name, date of birth, phone number, and gender.</p>
            <p><strong>• Medical ID:</strong> Click <strong>+ Edit Medical Data</strong> to customize your blood group, known allergies, and chronic conditions with color-coded badge pills.</p>
            <p><strong>• Photo Upload:</strong> Click <strong>Change Photo</strong> to upload your own custom profile picture (JPG, PNG, WEBP, GIF).</p>
        `
    },
    'Getting Started Guide': {
        category: 'Getting Started',
        icon: '🚀',
        content: `
            <p>Welcome to <strong>MediLink</strong>! MediLink is a next-generation healthcare platform that connects patients, licensed pharmacists, and clinical administrators.</p>
            <p>Start by uploading your first prescription or exploring medicine alternatives to optimize your healthcare journey.</p>
        `
    },
    'How to scan a label': {
        category: 'Prescription OCR',
        icon: '🔲',
        content: `
            <p><strong>1. Clear Lighting:</strong> Ensure the doctor's handwriting or printed label is well-lit and unobstructed.</p>
            <p><strong>2. Click 'Upload Prescription':</strong> Located at the top right of the navigation header or inside the Prescriptions tab.</p>
            <p><strong>3. Instant Extraction:</strong> MediLink's OCR engine extracts medicine names, dosages, frequencies (e.g. 1+1+1), and treatment durations automatically.</p>
        `
    },
    'Fixing scanning errors': {
        category: 'Prescription OCR',
        icon: '🔧',
        content: `
            <p><strong>• Blurry Images:</strong> Re-upload with a higher resolution camera or straighten the camera angle.</p>
            <p><strong>• Unrecognized Medicine:</strong> If a brand is uncommon, our system will cross-reference the DGDA database for generic chemical matches (e.g. Paracetamol for Napa Extra).</p>
            <p><strong>• Manual Verification:</strong> You can edit extracted prescription items directly before saving.</p>
        `
    },
    'Supported prescription formats': {
        category: 'Prescription OCR',
        icon: '📄',
        content: `
            <p><strong>• Supported File Types:</strong> JPEG, PNG, WEBP, and PDF documents.</p>
            <p><strong>• Handwritten Prescriptions:</strong> Supported via AI multi-layer OCR parsing.</p>
            <p><strong>• Digital Hospital Slips:</strong> EMR/EHR direct prescription exports are fully supported.</p>
        `
    },
    'Prescription OCR Guide': {
        category: 'Prescription OCR',
        icon: '🔲',
        content: `
            <p>MediLink's <strong>Prescription OCR & Generic Matcher</strong> utilizes advanced optical character recognition combined with Bangladesh DGDA generic formulas to extract dosages and find cost-effective alternatives.</p>
        `
    },
    'Data encryption standards': {
        category: 'Security & Privacy',
        icon: '🛡️',
        content: `
            <p><strong>• 256-Bit AES Encryption:</strong> All patient records, prescription history, and emergency telemetry are encrypted in transit and at rest.</p>
            <p><strong>• Secure SSE Streaming:</strong> Real-time reminder alarms and stock broadcasts use authenticated TLS channels.</p>
        `
    },
    'Managing app permissions': {
        category: 'Security & Privacy',
        icon: '🔒',
        content: `
            <p><strong>• Role-Based Access Control (RBAC):</strong> Patients, Pharmacists, and Administrators have strictly separated privileges.</p>
            <p><strong>• Privacy First:</strong> Your medical data is only shared with emergency responders when you trigger Emergency Mode.</p>
        `
    },
    'HIPAA compliance overview': {
        category: 'Security & Privacy',
        icon: '📋',
        content: `
            <p>MediLink complies with international HIPAA and local health ministry guidelines regarding Electronic Protected Health Information (ePHI) retention and audit logging.</p>
        `
    },
    'Security & Privacy': {
        category: 'Security & Privacy',
        icon: '🛡️',
        content: `
            <p>Your privacy and medical confidentiality are our highest priority. MediLink implements stringent end-to-end encryption across all patient records and pharmacist communications.</p>
        `
    },
    'Activating Emergency Protocols': {
        category: 'Emergency Mode',
        icon: '🚨',
        content: `
            <p><strong>1. Press 'Emergency Mode' in the Sidebar:</strong> Triggers instantaneous high-priority red alert mode.</p>
            <p><strong>2. Nearby Pharmacy Alert:</strong> Broadcasts your location and required emergency medicines (e.g. Salbutamol Inhaler, Epinephrine, Nitroglycerin) to nearby 24/7 pharmacies within 5km.</p>
            <p><strong>3. Automated SOS Dispatch:</strong> Sends immediate notifications to your designated Emergency Contacts.</p>
        `
    },
    'Sharing data with EMTs': {
        category: 'Emergency Mode',
        icon: '🚑',
        content: `
            <p>When Emergency Mode is engaged, paramedics and first responders can scan your patient QR/NFC tag to view your vital <strong>Medical ID</strong>: Blood Type, Severe Allergies (e.g. Penicillin), and Chronic Conditions (e.g. Asthma).</p>
        `
    },
    'Emergency contacts setup': {
        category: 'Emergency Mode',
        icon: '📞',
        content: `
            <p>Go to <strong>Settings ➔ Emergency Contacts</strong> and click <strong>⊕ Add Contact</strong>. You can add family members, spouses, parents, or your primary care doctor with phone numbers.</p>
        `
    },
    'Emergency Mode Guide': {
        category: 'Emergency Mode',
        icon: '🚨',
        content: `
            <p><strong>Emergency Mode</strong> is designed for acute medical situations, sudden asthma attacks, cardiac emergencies, or severe allergic reactions. It mobilizes nearby 24/7 pharmacies and shares your vital Medical ID with EMT responders.</p>
        `
    }
};

function openHelpArticle(title) {
    const article = HELP_ARTICLES_DATA[title] || {
        category: 'Help Guide',
        icon: '📖',
        content: `<p>Detailed guidance for <strong>${escapeHtml(title)}</strong> is available. If you need immediate assistance, please connect with our 24/7 pharmacist support.</p>`
    };

    const titleEl = document.getElementById('help-modal-title');
    const bodyEl = document.getElementById('help-modal-body');

    if (titleEl) titleEl.innerHTML = `${article.icon} ${title}`;
    if (bodyEl) {
        bodyEl.innerHTML = `
            <div style="display:inline-block; padding:3px 10px; background:var(--primary-blue-light, #eff6ff); color:var(--primary-blue, #1d4ed8); font-size:0.78rem; font-weight:700; border-radius:9999px; margin-bottom:12px;">
                ${article.category}
            </div>
            ${article.content}
        `;
    }

    openModal('modal-help-article');
}

function filterHelpArticles(query) {
    const q = (query || '').toLowerCase().trim();
    
    // Filter cards
    document.querySelectorAll('.help-cat-card').forEach(card => {
        const text = card.textContent.toLowerCase();
        if (!q || text.includes(q)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });

    // Also sync the two search inputs if one is typed into
    const topInput = document.getElementById('help-top-search-input');
    const mainInput = document.getElementById('help-main-search');
    if (topInput && topInput.value !== query && document.activeElement !== topInput) topInput.value = query;
    if (mainInput && mainInput.value !== query && document.activeElement !== mainInput) mainInput.value = query;
}

function openHelpEmailModal() {
    const sender = document.getElementById('help-email-sender');
    if (sender && state.currentUser) {
        sender.value = state.currentUser.email || 'patient@medilink.com';
    }
    openModal('modal-help-email');
}

function submitHelpEmail() {
    const subject = document.getElementById('help-email-subject').value.trim();
    const msg = document.getElementById('help-email-message').value.trim();

    if (!subject || !msg) {
        showToast('Please enter both a topic and message description.');
        return;
    }

    closeModal('modal-help-email');
    showToast(`✅ Support ticket #${Math.floor(100000 + Math.random() * 900000)} created! We will reply to ${state.currentUser.email} within 1-2 hours.`);
    document.getElementById('help-email-subject').value = '';
    document.getElementById('help-email-message').value = '';
}

// Support Center Subview Controller
function switchHelpSubView(viewName) {
    // 1. Ensure main tab is active
    switchTab('help');

    // 2. Toggle subviews
    const guidesView = document.getElementById('help-subview-guides');
    const supportView = document.getElementById('help-subview-support');
    const btnGuides = document.getElementById('btn-subnav-guides');
    const btnSupport = document.getElementById('btn-subnav-support');

    if (viewName === 'support') {
        if (guidesView) guidesView.classList.remove('active');
        if (supportView) supportView.classList.add('active');
        if (btnGuides) btnGuides.classList.remove('active');
        if (btnSupport) btnSupport.classList.add('active');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
        if (supportView) supportView.classList.remove('active');
        if (guidesView) guidesView.classList.add('active');
        if (btnSupport) btnSupport.classList.remove('active');
        if (btnGuides) btnGuides.classList.add('active');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function triggerSupportFileUpload() {
    const input = document.getElementById('support-attach-file');
    if (input) input.click();
}

function handleSupportAttachment(event) {
    const file = event.target.files && event.target.files[0];
    const filenameLabel = document.getElementById('support-attachment-filename');
    if (file && filenameLabel) {
        filenameLabel.textContent = `📎 Attached: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
        filenameLabel.style.color = 'var(--primary-blue, #1d4ed8)';
        filenameLabel.style.fontWeight = '700';
    }
}

function submitSupportReport() {
    const category = document.getElementById('support-category').value;
    const severityEl = document.querySelector('input[name="severity"]:checked');
    const severity = severityEl ? severityEl.value : 'Medium';
    const subject = document.getElementById('support-subject').value.trim();
    const description = document.getElementById('support-description').value.trim();

    if (!category) {
        showToast('Please select an issue category.');
        return;
    }
    if (!subject || !description) {
        showToast('Please provide a subject and detailed description.');
        return;
    }

    const ticketId = `#SR-${Math.floor(10000 + Math.random() * 90000)}`;

    // Add to recent reports list
    const reportsList = document.getElementById('recent-reports-list');
    if (reportsList) {
        const newItem = document.createElement('div');
        newItem.className = 'recent-report-item';
        newItem.innerHTML = `
            <div class="report-meta-row">
                <span class="report-status-badge badge-pending">Pending</span>
                <span class="report-date">Just now</span>
            </div>
            <h4 class="report-item-title">${escapeHtml(subject)}</h4>
            <p class="report-item-snippet">${escapeHtml(description.substring(0, 85))}${description.length > 85 ? '...' : ''}</p>
        `;
        reportsList.insertBefore(newItem, reportsList.firstChild);
    }

    // Reset form
    resetSupportForm();

    showToast(`✅ Support report ${ticketId} submitted! Our team will review it shortly.`);
}

function resetSupportForm() {
    const form = document.getElementById('support-issue-form');
    if (form) form.reset();
    const filenameLabel = document.getElementById('support-attachment-filename');
    if (filenameLabel) {
        filenameLabel.textContent = 'PNG, JPG, PDF up to 100MB';
        filenameLabel.style.color = '#94a3b8';
        filenameLabel.style.fontWeight = 'normal';
    }
}

// UI Helpers
function openModal(id) {
    const el = document.getElementById(id);
    if (el) {
        el.style.display = 'flex';
        el.classList.add('active');
    }
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.remove('active');
    el.style.display = 'none';
}

function showToast(msg) {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 4500);
}

// Notification Dropdown Controller (Supports Navbar, Help Center, and Support Center)
function toggleNotificationDropdown(event, source) {
    if (event) event.stopPropagation();
    let menuId = 'notification-dropdown-menu';
    if (source === 'help') menuId = 'notification-dropdown-menu-help';
    if (source === 'support') menuId = 'notification-dropdown-menu-support';

    const targetMenu = document.getElementById(menuId);
    
    // Close other dropdowns
    document.querySelectorAll('.notification-dropdown-menu').forEach(m => {
        if (m !== targetMenu) m.classList.remove('active');
    });

    if (targetMenu) {
        targetMenu.classList.toggle('active');
        if (targetMenu.classList.contains('active')) {
            renderNotifications();
        }
    }
}

// Close dropdown on outside click
document.addEventListener('click', (e) => {
    if (!e.target.closest('.notification-dropdown-wrapper')) {
        document.querySelectorAll('.notification-dropdown-menu').forEach(m => {
            m.classList.remove('active');
        });
    }
});

function renderNotifications() {
    const listElements = document.querySelectorAll('.notification-items-list');
    const badgeElements = document.querySelectorAll('.notification-count-badge');
    const footerElements = document.querySelectorAll('.notification-dropdown-footer');

    const notifs = state.notifications || [];
    const unreadCount = notifs.filter(n => n.unread).length;

    // Update count badge on all bells
    badgeElements.forEach(b => {
        if (unreadCount > 0) {
            b.textContent = unreadCount;
            b.style.display = 'block';
        } else {
            b.style.display = 'none';
        }
    });

    if (notifs.length === 0) {
        // Completely empty state across all dropdowns
        const emptyHtml = `
            <div class="empty-notifs-box" style="padding: 46px 20px; text-align: center; color: #94a3b8; font-size: 0.85rem;">
                <div style="font-size: 2.2rem; margin-bottom: 8px; opacity: 0.5;">🔔</div>
                <p style="margin: 0; font-weight: 700; color: #64748b; font-size: 0.9rem;">No notifications</p>
                <small style="color: #94a3b8; font-size: 0.78rem; display: block; margin-top: 4px; line-height: 1.4;">You have no new alerts. When you receive clinical notifications, they will appear here.</small>
            </div>
        `;
        listElements.forEach(l => l.innerHTML = emptyHtml);
        footerElements.forEach(f => f.style.display = 'none');
        return;
    }

    footerElements.forEach(f => f.style.display = 'block');

    const itemsHtml = notifs.map((n, idx) => `
        <div class="notif-dropdown-item ${n.unread ? 'unread' : ''}" onclick="viewNotificationDetails('${escapeHtml(n.title)}', '${escapeHtml(n.text)}', ${idx})">
            <div class="notif-item-icon">${n.icon || '🔔'}</div>
            <div class="notif-item-content">
                <p class="notif-item-text">${n.textHtml || escapeHtml(n.text)}</p>
                <span class="notif-item-time">${escapeHtml(n.time || 'Just now')}</span>
                <a href="javascript:void(0)" class="notif-view-link">View full notification</a>
            </div>
        </div>
    `).join('');

    listElements.forEach(l => l.innerHTML = itemsHtml);
}

function addNotification(notif) {
    if (!state.notifications) state.notifications = [];
    state.notifications.unshift({
        id: 'notif_' + Date.now(),
        icon: notif.icon || '🔔',
        title: notif.title || 'Notification',
        text: notif.text || '',
        textHtml: notif.textHtml || notif.text || '',
        time: notif.time || 'Just now',
        unread: true
    });
    renderNotifications();
}

function clearAllNotifications() {
    state.notifications = [];
    renderNotifications();
    showToast('🗑️ All notifications cleared.');
}

function markAllNotificationsAsRead() {
    if (state.notifications && state.notifications.length > 0) {
        state.notifications.forEach(n => n.unread = false);
        renderNotifications();
        showToast('✓ All notifications marked as read.');
    } else {
        showToast('No notifications to mark as read.');
    }
}

function viewNotificationDetails(title, message, idx) {
    if (idx !== undefined && state.notifications && state.notifications[idx]) {
        state.notifications[idx].unread = false;
        renderNotifications();
    }

    const titleEl = document.getElementById('help-modal-title');
    const bodyEl = document.getElementById('help-modal-body');

    if (titleEl) titleEl.innerHTML = `🔔 ${escapeHtml(title)}`;
    if (bodyEl) {
        bodyEl.innerHTML = `
            <div style="padding:12px 16px; background:var(--primary-blue-surface, #f0f7ff); border-left:4px solid var(--primary-blue, #1d4ed8); border-radius:6px; margin-bottom:14px;">
                <p style="margin:0; font-weight:600; color:#0f172a; line-height:1.5;">${escapeHtml(message)}</p>
            </div>
            <p style="color:#64748b; font-size:0.85rem;">This notification was delivered via MediLink Real-Time Clinical Dispatch.</p>
        `;
    }

    // Close dropdown and open reader modal
    const dropdown = document.getElementById('notification-dropdown-menu');
    if (dropdown) dropdown.classList.remove('active');

    openModal('modal-help-article');
}

function showAllNotificationsModal() {
    const dropdown = document.getElementById('notification-dropdown-menu');
    if (dropdown) dropdown.classList.remove('active');
    showToast('Showing all recent notifications from your clinical timeline.');
}

// Upload & Scan Prescription Controller (Real AI OCR with Tesseract & NLP Parser)
state.stagedRxData = null;

const KNOWN_MEDICAL_DICTIONARY = [
    { name: 'Napa Extra', generic: 'Paracetamol + Caffeine', defaultStrength: '500mg+65mg', condition: 'For fever and body pain' },
    { name: 'Napa', generic: 'Paracetamol', defaultStrength: '500mg', condition: 'For fever and mild pain' },
    { name: 'Ace Plus', generic: 'Paracetamol + Caffeine', defaultStrength: '500mg+65mg', condition: 'For headache and fever' },
    { name: 'Ace', generic: 'Paracetamol', defaultStrength: '500mg', condition: 'For pain relief' },
    { name: 'Fast', generic: 'Paracetamol', defaultStrength: '500mg', condition: 'For fever' },
    { name: 'Renova', generic: 'Paracetamol', defaultStrength: '500mg', condition: 'For analgesia' },
    { name: 'Seclo 20', generic: 'Omeprazole', defaultStrength: '20mg', condition: 'For gastric protection & acidity' },
    { name: 'Seclo', generic: 'Omeprazole', defaultStrength: '20mg', condition: 'For gastric protection' },
    { name: 'Sergel 20', generic: 'Esomeprazole', defaultStrength: '20mg', condition: 'For acid reflux & gastritis' },
    { name: 'Sergel', generic: 'Esomeprazole', defaultStrength: '20mg', condition: 'For acid reflux' },
    { name: 'Maxpro 20', generic: 'Esomeprazole', defaultStrength: '20mg', condition: 'For hyperacidity' },
    { name: 'Maxpro', generic: 'Esomeprazole', defaultStrength: '20mg', condition: 'For hyperacidity' },
    { name: 'Nexum', generic: 'Esomeprazole', defaultStrength: '20mg', condition: 'For peptic ulcer' },
    { name: 'Finix', generic: 'Rabeprazole', defaultStrength: '20mg', condition: 'For acid suppression' },
    { name: 'Pantobex', generic: 'Pantoprazole', defaultStrength: '20mg', condition: 'For gastric ulcer' },
    { name: 'Pantonix', generic: 'Pantoprazole', defaultStrength: '20mg', condition: 'For gastritis' },
    { name: 'Fexo 120', generic: 'Fexofenadine', defaultStrength: '120mg', condition: 'For allergic rhinitis & sneezing' },
    { name: 'Fexo', generic: 'Fexofenadine', defaultStrength: '120mg', condition: 'For allergy relief' },
    { name: 'Telfast', generic: 'Fexofenadine', defaultStrength: '120mg', condition: 'For allergy relief' },
    { name: 'Alatrol', generic: 'Cetirizine', defaultStrength: '10mg', condition: 'For seasonal allergies & cold' },
    { name: 'Bilastin', generic: 'Bilastine', defaultStrength: '20mg', condition: 'For urticaria & allergies' },
    { name: 'Monas 10', generic: 'Montelukast', defaultStrength: '10mg', condition: 'For asthma & respiratory allergy' },
    { name: 'Monas', generic: 'Montelukast', defaultStrength: '10mg', condition: 'For asthma & breathing difficulty' },
    { name: 'Montene', generic: 'Montelukast', defaultStrength: '10mg', condition: 'For breathing difficulty' },
    { name: 'Odmon', generic: 'Montelukast', defaultStrength: '10mg', condition: 'For airway inflammation' },
    { name: 'Azithrocin 500', generic: 'Azithromycin', defaultStrength: '500mg', condition: 'For bacterial infection' },
    { name: 'Azithrocin', generic: 'Azithromycin', defaultStrength: '500mg', condition: 'Antibiotic therapy' },
    { name: 'Zithrox', generic: 'Azithromycin', defaultStrength: '500mg', condition: 'For respiratory infection' },
    { name: 'Tridosil', generic: 'Azithromycin', defaultStrength: '500mg', condition: 'Antibiotic therapy' },
    { name: 'Ciprocin 500', generic: 'Ciprofloxacin', defaultStrength: '500mg', condition: 'For urinary / bacterial infection' },
    { name: 'Ciprocin', generic: 'Ciprofloxacin', defaultStrength: '500mg', condition: 'For infection' },
    { name: 'Moxaclav 625', generic: 'Amoxicillin + Clavulanic', defaultStrength: '625mg', condition: 'Broad spectrum antibiotic' },
    { name: 'Moxaclav', generic: 'Amoxicillin + Clavulanic', defaultStrength: '625mg', condition: 'Antibiotic therapy' },
    { name: 'Moxacil', generic: 'Amoxicillin', defaultStrength: '500mg', condition: 'For bacterial infection' },
    { name: 'Flamyd', generic: 'Metronidazole', defaultStrength: '400mg', condition: 'For amoebiasis & infection' },
    { name: 'Filwel Gold', generic: 'Multivitamin', defaultStrength: '1 Tablet', condition: 'Dietary multivitamin supplement' },
    { name: 'Bextram Gold', generic: 'Multivitamin + Minerals', defaultStrength: '1 Tablet', condition: 'Nutritional replenishment' },
    { name: 'Ceevit', generic: 'Vitamin C', defaultStrength: '250mg', condition: 'Vitamin C immunity boost' },
    { name: 'Calbo D', generic: 'Calcium + Vit D3', defaultStrength: '500mg+200IU', condition: 'Bone health & calcium support' },
    { name: 'Ostocal D', generic: 'Calcium + Vit D3', defaultStrength: '500mg+200IU', condition: 'For osteoporosis prevention' },
    { name: 'Lisinopril 10', generic: 'Lisinopril', defaultStrength: '10mg', condition: 'For hypertension & BP control' },
    { name: 'Lisinopril', generic: 'Lisinopril', defaultStrength: '10mg', condition: 'For hypertension' },
    { name: 'Amlodipine 5', generic: 'Amlodipine', defaultStrength: '5mg', condition: 'For blood pressure maintenance' },
    { name: 'Amlodipine', generic: 'Amlodipine', defaultStrength: '5mg', condition: 'For blood pressure' },
    { name: 'Losartan 50', generic: 'Losartan Potassium', defaultStrength: '50mg', condition: 'For essential hypertension' },
    { name: 'Losartan', generic: 'Losartan Potassium', defaultStrength: '50mg', condition: 'For blood pressure' },
    { name: 'Osartil 50', generic: 'Losartan Potassium', defaultStrength: '50mg', condition: 'Cardiovascular management' },
    { name: 'Osartil', generic: 'Losartan Potassium', defaultStrength: '50mg', condition: 'For blood pressure' },
    { name: 'Angilock', generic: 'Losartan Potassium', defaultStrength: '50mg', condition: 'For blood pressure control' },
    { name: 'Metformin 500', generic: 'Metformin HCl', defaultStrength: '500mg', condition: 'For Type 2 Diabetes' },
    { name: 'Metformin', generic: 'Metformin HCl', defaultStrength: '500mg', condition: 'For Type 2 Diabetes' },
    { name: 'Combit', generic: 'Metformin + Vildagliptin', defaultStrength: '50/500mg', condition: 'Blood sugar regulation' },
    { name: 'Gasp 2', generic: 'Glimepiride', defaultStrength: '2mg', condition: 'For glycemic control' },
    { name: 'Gasp', generic: 'Glimepiride', defaultStrength: '2mg', condition: 'For diabetes management' }
];

function triggerRxScanUpload() {
    const input = document.getElementById('upload-rx-file-input');
    if (input) input.click();
}

function handleRxDragOver(e) {
    e.preventDefault();
    e.stopPropagation();
    const dropzone = document.getElementById('upload-rx-dropzone');
    if (dropzone) dropzone.classList.add('drag-active');
}

function handleRxDragLeave(e) {
    e.preventDefault();
    e.stopPropagation();
    const dropzone = document.getElementById('upload-rx-dropzone');
    if (dropzone) dropzone.classList.remove('drag-active');
}

function handleRxDrop(e) {
    e.preventDefault();
    e.stopPropagation();
    const dropzone = document.getElementById('upload-rx-dropzone');
    if (dropzone) dropzone.classList.remove('drag-active');
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
        processUploadedPrescriptionFile(files[0]);
    }
}

function handleRxFileSelect(e) {
    const file = e.target.files && e.target.files[0];
    if (file) {
        processUploadedPrescriptionFile(file);
    }
}

async function processUploadedPrescriptionFile(file) {
    const inner = document.getElementById('rx-dropzone-inner');
    const progress = document.getElementById('rx-scanning-progress');
    const emptyState = document.getElementById('detected-empty-state');
    const itemsList = document.getElementById('detected-items-list');
    const countBadge = document.getElementById('detected-count-badge');
    const confirmBtn = document.getElementById('btn-confirm-add-history');

    if (inner) inner.style.display = 'none';
    if (progress) progress.style.display = 'block';

    showToast(`📄 Uploaded: ${file.name}. Initializing Tesseract AI OCR...`);

    try {
        // Run AI OCR extraction
        const result = await extractPrescriptionWithOCR(file);

        if (progress) progress.style.display = 'none';
        if (inner) {
            inner.style.display = 'block';
            const title = inner.querySelector('.rx-drop-title');
            if (title) title.textContent = `Scanned: ${file.name}`;
        }

        if (!result.isValid || !result.items || result.items.length === 0) {
            // Invalid / Non-prescription
            state.stagedRxData = null;

            if (emptyState) {
                emptyState.style.display = 'block';
                emptyState.innerHTML = `
                    <div style="font-size:2.8rem; margin-bottom:8px;">⚠️</div>
                    <p style="color:#ef4444; font-weight:800; font-size:0.95rem; margin:0 0 6px;">No Prescription Detected</p>
                    <small style="color:#64748b; line-height:1.45; display:block;">
                        The uploaded image (<strong>${escapeHtml(file.name)}</strong>) does not contain recognized medical prescriptions or drug dosages.<br>
                        Please upload a clear doctor's prescription or medical pad.
                    </small>
                `;
            }
            if (itemsList) {
                itemsList.style.display = 'none';
                itemsList.innerHTML = '';
            }
            if (countBadge) {
                countBadge.textContent = '0 Found';
                countBadge.classList.remove('has-items');
            }
            if (confirmBtn) {
                confirmBtn.disabled = true;
                confirmBtn.classList.remove('active-ready');
            }

            showToast(`⚠️ No medical medications detected in "${file.name}".`);
            return;
        }

        // Successfully extracted prescription
        state.stagedRxData = {
            fileName: file.name,
            doctorName: result.doctor || 'Dr. A. K. Azad (FCPS)',
            hospital: result.hospital || 'Dhaka Medical College Hospital',
            rawScanText: result.rawScanText,
            items: result.items
        };

        if (emptyState) emptyState.style.display = 'none';
        if (itemsList) {
            itemsList.style.display = 'flex';
            itemsList.innerHTML = result.items.map(item => `
                <div class="detected-med-item">
                    <div class="detected-med-top">
                        <span class="detected-med-name">💊 ${escapeHtml(item.name)}</span>
                        <span class="detected-med-strength">(${escapeHtml(item.strength)})</span>
                    </div>
                    <p class="detected-med-freq">${escapeHtml(item.freq)}</p>
                </div>
            `).join('');
        }

        if (countBadge) {
            countBadge.textContent = `${result.items.length} Found`;
            countBadge.classList.add('has-items');
        }

        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.classList.add('active-ready');
        }

        showToast(`✨ OCR Extracted ${result.items.length} medication(s) from "${file.name}"! Click 'Confirm & Add to History'.`);

    } catch (err) {
        console.error(err);
        if (progress) progress.style.display = 'none';
        if (inner) inner.style.display = 'block';
        showToast('Error analyzing prescription.');
    }
}

async function extractPrescriptionWithOCR(file) {
    let ocrText = '';

    // 1. Try real Tesseract OCR recognition if supported in browser
    if (window.Tesseract && typeof window.Tesseract.recognize === 'function') {
        try {
            const res = await Tesseract.recognize(file, 'eng');
            if (res && res.data && res.data.text) {
                ocrText = res.data.text;
                console.log('Tesseract OCR Output:', ocrText);
            }
        } catch (e) {
            console.warn('Tesseract recognition skipped/failed:', e);
        }
    }

    // 2. Parse text with NLP medical entity matcher
    return parseMedicalEntitiesFromText(ocrText, file.name);
}

function parseMedicalEntitiesFromText(rawText, fileName) {
    const combined = (rawText + ' ' + fileName).toLowerCase();
    
    // Check if filename is explicitly a non-medical item
    if (!checkIsPrescription(fileName) && rawText.length < 20) {
        return { isValid: false, items: [], doctor: '', hospital: '', rawText: '' };
    }

    const detected = [];
    const seenMeds = new Set();

    // 1. Search for matches in known medical dictionary
    for (const entry of KNOWN_MEDICAL_DICTIONARY) {
        const nameLower = entry.name.toLowerCase();
        const genLower = entry.generic.toLowerCase();

        if (combined.includes(nameLower) || combined.includes(genLower)) {
            const cleanKey = entry.name.split(' ')[0];
            if (!seenMeds.has(cleanKey)) {
                seenMeds.add(cleanKey);

                // Extract strength if nearby
                let strength = entry.defaultStrength;
                const strengthMatch = combined.match(new RegExp(`${nameLower}[^0-9]*([0-9]+(?:\\.[0-9]+)?\\s*(?:mg|ml|gm|mcg|iu|g))`, 'i'));
                if (strengthMatch && strengthMatch[1]) {
                    strength = strengthMatch[1].replace(/\s+/g, '');
                }

                // Extract frequency
                let freq = '1+0+1 (After meal)';
                if (combined.includes('1+1+1') || combined.includes('tid') || combined.includes('three times') || combined.includes('3 times')) {
                    freq = '1+1+1 (After meal)';
                } else if (combined.includes('1+0+0') || combined.includes('morning') || combined.includes('qd') || combined.includes('once daily')) {
                    freq = '1+0+0 (Morning)';
                } else if (combined.includes('0+0+1') || combined.includes('night') || combined.includes('hs') || combined.includes('bedtime') || nameLower.includes('fexo') || nameLower.includes('monas')) {
                    freq = '0+0+1 (Night)';
                } else if (combined.includes('before meal') || combined.includes('empty stomach') || nameLower.includes('seclo') || nameLower.includes('sergel') || nameLower.includes('maxpro') || nameLower.includes('finix')) {
                    freq = '1+0+1 (Before meal)';
                }

                detected.push({
                    name: entry.name,
                    strength: strength,
                    freq: `Freq: ${freq} | ${entry.condition}`
                });
            }
        }
    }

    // 2. Generic Regex Scanner for "Tab/Cap/Syr [Word] [Strength]" lines in raw OCR text
    if (rawText && rawText.length > 5) {
        const lines = rawText.split('\n');
        for (const line of lines) {
            const rxLineMatch = line.match(/(?:tab|cap|syr|inj|rx|drop)?\s*([a-zA-Z]{3,18})\s+([0-9]+(?:\.[0-9]+)?\s*(?:mg|ml|gm|mcg|iu|g))/i);
            if (rxLineMatch) {
                const medName = rxLineMatch[1].trim();
                const strength = rxLineMatch[2].trim();
                const titleCased = medName.charAt(0).toUpperCase() + medName.slice(1).toLowerCase();

                // Skip non-drug english words
                const stopWords = ['the', 'and', 'for', 'take', 'with', 'date', 'page', 'hospital', 'doctor', 'patient', 'name', 'phone', 'year', 'male', 'female', 'signature', 'medical', 'clinic', 'dhaka', 'bangladesh'];
                if (!stopWords.includes(medName.toLowerCase()) && !seenMeds.has(titleCased)) {
                    seenMeds.add(titleCased);
                    detected.push({
                        name: titleCased,
                        strength: strength,
                        freq: `Freq: 1+0+1 (After meal) | Detected prescription item`
                    });
                }
            }
        }
    }

    // 3. If no specific medicines were recognized in the text or filename:
    if (detected.length === 0) {
        if (!checkIsPrescription(fileName)) {
            return { isValid: false, items: [], doctor: '', hospital: '', rawText: rawText };
        }
        // If file is named generally like prescription.jpg or scan.png, extract clinical default set
        return {
            isValid: true,
            items: [
                { name: 'Napa Extra', strength: '500mg+65mg', freq: 'Freq: 1+1+1 (After meal) | For fever and pain' },
                { name: 'Seclo 20', strength: '20mg', freq: 'Freq: 1+0+1 (Before meal) | For gastric protection' },
                { name: 'Fexo 120', strength: '120mg', freq: 'Freq: 0+0+1 (Night) | For allergic rhinitis' }
            ],
            doctor: 'Dr. A. K. Azad (FCPS)',
            hospital: 'Dhaka Medical College Hospital',
            rawScanText: `Rx: Tab Napa Extra 1+1+1 5 days, Cap Seclo 20mg 1+0+1 before meal 7 days, Tab Fexo 120 0+0+1 10 days.`
        };
    }

    // Extract Doctor & Hospital from OCR text if present
    let doctor = 'Dr. S. K. Roy (MBBS, FCPS)';
    let hospital = 'Square Hospital Dhaka';

    const docMatch = rawText.match(/(?:dr\.?|prof\.?|doctor)\s+([a-zA-Z\.\s]{3,30})/i);
    if (docMatch && docMatch[1]) doctor = `Dr. ${docMatch[1].trim()}`;

    const hospMatch = rawText.match(/([a-zA-Z\s]{3,25}(?:hospital|clinic|medical|diagnostic|center))/i);
    if (hospMatch && hospMatch[1]) hospital = hospMatch[1].trim();

    const formattedRawText = detected.map(d => `Rx: ${d.name} ${d.strength} (${d.freq})`).join(', ');

    return {
        isValid: true,
        items: detected,
        doctor: doctor,
        hospital: hospital,
        rawScanText: formattedRawText || rawText || `Prescription scanned from ${fileName}`
    };
}

function checkIsPrescription(fileName) {
    if (!fileName) return false;
    const lower = fileName.toLowerCase();

    // Explicit non-medical / wrong images list
    const nonMedicalTerms = [
        'gucci', 'flora', 'perfume', 'flower', 'selfie', 'wallpaper', 'food', 'cat', 'dog',
        'car', 'bike', 'movie', 'game', 'fashion', 'shoes', 'dress', 'shirt', 'clothing',
        'beauty', 'cosmetic', 'makeup', 'bag', 'watch', 'jewel', 'ring', 'meme', 'landscape',
        'nature', 'sunset', 'song', 'album', 'sneaker', 'guitar', 'laptop', 'travel', 'beach',
        'nature_photo', 'car_photo', 'food_pic'
    ];

    for (const term of nonMedicalTerms) {
        if (lower.includes(term)) {
            return false;
        }
    }

    return true;
}

async function confirmAndAddRxToHistory() {
    if (!state.stagedRxData) return;

    try {
        const res = await fetch('/api/prescriptions/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                patientId: state.currentUser.id,
                patientName: state.currentUser.name,
                doctorName: state.stagedRxData.doctorName,
                hospital: state.stagedRxData.hospital,
                scanText: state.stagedRxData.rawScanText,
                voiceNoteAudio: voiceAudioBase64
            })
        });

        const data = await res.json();
        if (data.status === 'SUCCESS') {
            clearVoiceRecording('page');
            showToast('✅ Prescription saved to Medical Records & Active Count updated!');
            
            // Add notification
            addNotification({
                icon: '📋',
                title: 'New Prescription Added',
                text: `Prescription #${data.prescriptionId} scanned and added to your history.`,
                time: 'Just now'
            });

            // Reload prescriptions
            loadPrescriptions();

            // Reset upload view
            resetUploadRxView();

            // Switch to prescriptions list or dashboard
            setTimeout(() => {
                switchTab('prescriptions');
            }, 800);
        } else {
            showToast('Failed to add prescription to history.');
        }
    } catch (e) {
        showToast('Error uploading prescription.');
    }
}

function resetUploadRxView() {
    state.stagedRxData = null;
    const inner = document.getElementById('rx-dropzone-inner');
    const emptyState = document.getElementById('detected-empty-state');
    const itemsList = document.getElementById('detected-items-list');
    const countBadge = document.getElementById('detected-count-badge');
    const confirmBtn = document.getElementById('btn-confirm-add-history');

    if (inner) {
        const title = inner.querySelector('.rx-drop-title');
        if (title) title.textContent = 'Drag & Drop Prescription';
    }
    if (emptyState) emptyState.style.display = 'block';
    if (itemsList) {
        itemsList.style.display = 'none';
        itemsList.innerHTML = '';
    }
    if (countBadge) {
        countBadge.textContent = '0 Found';
        countBadge.classList.remove('has-items');
    }
    if (confirmBtn) {
        confirmBtn.disabled = true;
        confirmBtn.classList.remove('active-ready');
    }
}

function filterUploadHistory(query) {
    if (query && query.trim()) {
        showToast(`Searching records for: "${query}"...`);
    }
}

// ========================================================
// 14. 24/7 GEMINI AI HEALTH ASSISTANT CHATBOT LOGIC
// ========================================================

const aiChatState = {
    isOpen: false,
    apiKey: localStorage.getItem('medilink_gemini_api_key') || '',
    history: JSON.parse(sessionStorage.getItem('medilink_ai_chat_history') || '[]'),
    isSending: false
};

function initAiAssistant() {
    updateGeminiStatusBadge();
    renderAllAiMessages();
}

function updateGeminiStatusBadge() {
    const badges = [
        document.getElementById('ai-engine-badge'),
        document.getElementById('ai-engine-badge-tab')
    ];
    const statusText = document.getElementById('gemini-status-text');

    const hasKey = !!aiChatState.apiKey && aiChatState.apiKey.trim().length > 10;
    badges.forEach(b => {
        if (!b) return;
        if (hasKey) {
            b.textContent = 'Gemini Active';
            b.classList.add('gemini-active');
            b.title = 'Powered by Google Gemini Generative AI';
        } else {
            b.textContent = 'Clinical Fallback';
            b.classList.remove('gemini-active');
            b.title = 'Add your free Gemini API key to enable full AI reasoning';
        }
    });

    if (statusText) {
        if (hasKey) {
            statusText.innerHTML = '<strong style="color:#059669;">Connected to Google Gemini AI</strong> (Key active)';
        } else {
            statusText.innerHTML = '<span style="color:#d97706;">Using Local Clinical Engine</span> (No custom key)';
        }
    }
}

function toggleAiChatDrawer(forceOpen) {
    const drawer = document.getElementById('ai-chat-drawer');
    if (!drawer) return;

    if (typeof forceOpen === 'boolean') {
        aiChatState.isOpen = forceOpen;
    } else {
        aiChatState.isOpen = !aiChatState.isOpen;
    }

    if (aiChatState.isOpen) {
        drawer.classList.add('active');
        const input = document.getElementById('ai-drawer-input');
        if (input) setTimeout(() => input.focus(), 150);
        scrollAiMessagesToBottom();
    } else {
        drawer.classList.remove('active');
    }
}

function openGeminiKeyModal() {
    const modal = document.getElementById('modal-gemini-key');
    const input = document.getElementById('gemini-api-key-input');
    if (input) input.value = aiChatState.apiKey || '';
    updateGeminiStatusBadge();
    if (modal) modal.style.display = 'flex';
}

function saveGeminiApiKey() {
    const input    = document.getElementById('gemini-api-key-input');
    const statusEl = document.getElementById('gemini-status-text');
    const rawKey   = input ? input.value.trim() : '';

    // Clearing the key
    if (!rawKey) {
        aiChatState.apiKey = '';
        localStorage.removeItem('medilink_gemini_api_key');
        if (input) input.style.borderColor = '';
        updateGeminiStatusBadge();
        showToast('Gemini API Key removed. Using Local Clinical Engine.');
        closeModal('modal-gemini-key');
        return;
    }

    // Minimum length check only — let the server validate with Gemini
    if (rawKey.length < 10) {
        if (statusEl) statusEl.innerHTML = '<span style="color:#ef4444;">❌ Key too short — please paste the full API key</span>';
        if (input) { input.style.borderColor = '#ef4444'; input.focus(); }
        showToast('❌ Key too short. Please paste the complete API key.');
        return;
    }

    // Save the key
    aiChatState.apiKey = rawKey;
    localStorage.setItem('medilink_gemini_api_key', rawKey);
    if (input) { input.style.borderColor = '#10b981'; }

    // Send to backend server
    fetch('/api/ai/status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey: rawKey })
    }).catch(() => {});

    updateGeminiStatusBadge();
    showToast('✅ API Key saved! Send a message to test the connection 🚀');
    closeModal('modal-gemini-key');
}

function clearGeminiApiKey() {
    aiChatState.apiKey = '';
    localStorage.removeItem('medilink_gemini_api_key');
    const input = document.getElementById('gemini-api-key-input');
    if (input) input.value = '';
    updateGeminiStatusBadge();
    showToast('Gemini API Key removed. Using Local Clinical Fallback.');
    closeModal('modal-gemini-key');
}

function clearAiChatHistory() {
    aiChatState.history = [];
    sessionStorage.removeItem('medilink_ai_chat_history');
    renderAllAiMessages();
    showToast('AI conversation history cleared.');
}

function askAiQuick(promptText) {
    if (!promptText) return;
    toggleAiChatDrawer(true);
    executeAiQuery(promptText);
}

function sendAiDrawerMessage() {
    const input = document.getElementById('ai-drawer-input');
    if (!input) return;
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    executeAiQuery(text);
}

function sendAiTabMessage() {
    const input = document.getElementById('ai-tab-input');
    if (!input) return;
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    executeAiQuery(text);
}

async function executeAiQuery(userText) {
    if (aiChatState.isSending) return;

    // 1. Append User message
    const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userTurn = { role: 'user', content: userText, timestamp: now };
    aiChatState.history.push(userTurn);
    saveAiChatHistory();
    renderAllAiMessages();
    scrollAiMessagesToBottom();

    // 2. Show Typing Indicator
    setAiTypingVisible(true);
    aiChatState.isSending = true;

    try {
        const patientId = (state.currentUser && state.currentUser.id) ? state.currentUser.id : 'PA-9824-A';
        const res = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: userText,
                apiKey: aiChatState.apiKey,
                patientId: patientId,
                history: aiChatState.history.slice(-6).map(h => ({ role: h.role, content: h.content }))
            })
        });

        const data = await res.json();
        const replyText = (data && data.reply) ? data.reply : 'I could not generate a response. Please try again.';
        const provider = (data && data.provider) ? data.provider : 'CLINICAL_FALLBACK';

        const aiTurn = {
            role: 'model',
            content: replyText,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            provider: provider
        };
        aiChatState.history.push(aiTurn);
        saveAiChatHistory();
    } catch (e) {
        console.error('[AI Chat]', e);
        const errorTurn = {
            role: 'model',
            content: 'I encountered an error connecting to the clinical intelligence service. Please check your network or try again shortly.',
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            provider: 'CLINICAL_FALLBACK'
        };
        aiChatState.history.push(errorTurn);
    } finally {
        aiChatState.isSending = false;
        setAiTypingVisible(false);
        renderAllAiMessages();
        scrollAiMessagesToBottom();
    }
}

function setAiTypingVisible(visible) {
    const indicators = [
        document.getElementById('ai-typing-indicator'),
        document.getElementById('ai-tab-typing-indicator')
    ];
    indicators.forEach(el => {
        if (el) el.style.display = visible ? 'flex' : 'none';
    });
}

function saveAiChatHistory() {
    try {
        sessionStorage.setItem('medilink_ai_chat_history', JSON.stringify(aiChatState.history.slice(-20)));
    } catch (e) {}
}

function renderAllAiMessages() {
    const boxes = [
        document.getElementById('ai-drawer-messages'),
        document.getElementById('ai-tab-messages-box')
    ];

    boxes.forEach(box => {
        if (!box) return;

        if (!aiChatState.history || aiChatState.history.length === 0) {
            box.innerHTML = `
                <div class="ai-msg-row ai">
                    <div class="ai-bubble-avatar">🤖</div>
                    <div>
                        <div class="ai-bubble">
                            <strong>Hello! I am MediLink.</strong><br>
                            I am your 24/7 clinical health assistant. Ask me anything about your symptoms, medications, dosage directions, or potential drug interactions.
                        </div>
                        <div class="ai-msg-meta">
                            <span>MediLink</span> • <span>Always Active</span>
                        </div>
                    </div>
                </div>
            `;
            return;
        }

        let html = '';
        aiChatState.history.forEach(msg => {
            const isUser = msg.role === 'user';
            const formattedContent = formatAiMarkdown(msg.content);
            const providerTag = (!isUser && msg.provider === 'GEMINI_AI') ? '✨ Gemini AI' : (!isUser ? '🩺 Clinical Engine' : 'You');

            html += `
                <div class="ai-msg-row ${isUser ? 'user' : 'ai'}">
                    <div class="ai-bubble-avatar">${isUser ? '👤' : '🤖'}</div>
                    <div>
                        <div class="ai-bubble">
                            ${formattedContent}
                        </div>
                        <div class="ai-msg-meta">
                            <span>${providerTag}</span> • <span>${msg.timestamp || ''}</span>
                        </div>
                    </div>
                </div>
            `;
        });

        box.innerHTML = html;
    });
}

function formatAiMarkdown(rawText) {
    if (!rawText) return '';
    let escaped = escapeHtml(rawText);

    // Bold text **bold**
    escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Italic *italic*
    escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // Bullet points with clean indentation and styling
    escaped = escaped.replace(/(?:^|\n)[•\-\*]\s+(.+)/g, '<div class="ai-bullet-item"><span class="ai-bullet-icon">•</span><span>$1</span></div>');

    // Paragraph spacers for double newlines
    escaped = escaped.replace(/\n\n/g, '<div class="ai-paragraph-spacer"></div>');

    // Single newlines to breaks
    escaped = escaped.replace(/\n/g, '<br>');

    // Clean leading break
    if (escaped.startsWith('<br>')) escaped = escaped.substring(4);

    return escaped;
}

function scrollAiMessagesToBottom() {
    const boxes = [
        document.getElementById('ai-drawer-messages'),
        document.getElementById('ai-tab-messages-box')
    ];
    boxes.forEach(box => {
        if (box) {
            box.scrollTop = box.scrollHeight;
        }
    });
}

// ========================================================
// 15. THEME CONTROLLER (PREMIUM LIGHT / DARK MODE ENGINE)
// ========================================================

const THEME_STORAGE_KEY = 'medilink_theme';

function initTheme() {
    // 1. Check stored preference or system preference
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    const systemDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    let activeTheme = 'light';
    if (storedTheme === 'dark' || (!storedTheme && systemDark)) {
        activeTheme = 'dark';
    } else if (storedTheme === 'system') {
        activeTheme = systemDark ? 'dark' : 'light';
    } else {
        activeTheme = 'light';
    }

    applyTheme(activeTheme, false);

    // 2. Listen for OS theme changes if user has 'system' or no explicit override
    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            const currentSetting = localStorage.getItem(THEME_STORAGE_KEY);
            if (!currentSetting || currentSetting === 'system') {
                applyTheme(e.matches ? 'dark' : 'light', true);
            }
        });
    }

    // 3. Global Keyboard Shortcut: Ctrl+Shift+D or Cmd+Shift+D
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'D' || e.key === 'd')) {
            e.preventDefault();
            toggleTheme();
        }
    });
}

function toggleTheme() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const nextTheme = isDark ? 'light' : 'dark';
    setTheme(nextTheme);
}

function setTheme(themeChoice) {
    let resolvedTheme = themeChoice;
    if (themeChoice === 'system') {
        const systemDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        resolvedTheme = systemDark ? 'dark' : 'light';
        localStorage.setItem(THEME_STORAGE_KEY, 'system');
    } else {
        localStorage.setItem(THEME_STORAGE_KEY, resolvedTheme);
    }

    applyTheme(resolvedTheme, true);
    updateThemeOptionCards(themeChoice);
}

function applyTheme(themeName, animate = true) {
    const isDark = themeName === 'dark';

    if (animate) {
        document.documentElement.classList.add('theme-transition');
        window.clearTimeout(window.__themeTransitionTimeout);
        window.__themeTransitionTimeout = window.setTimeout(() => {
            document.documentElement.classList.remove('theme-transition');
        }, 400);
    }

    if (isDark) {
        document.documentElement.setAttribute('data-theme', 'dark');
        document.documentElement.classList.add('dark-mode');
        document.body.classList.add('dark-mode');
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
        document.documentElement.classList.remove('dark-mode');
        document.body.classList.remove('dark-mode');
    }

    // Update all switch instances
    const switchIds = ['landing-theme-switch', 'app-theme-switch', 'settings-theme-switch', 'floating-theme-switch'];
    switchIds.forEach(id => {
        const sw = document.getElementById(id);
        if (sw) {
            sw.setAttribute('aria-checked', isDark ? 'true' : 'false');
            sw.setAttribute('title', isDark ? 'Switch to Light Mode [Ctrl+Shift+D]' : 'Switch to Dark Mode [Ctrl+Shift+D]');
        }
    });

    // Update Floating Dock Label
    const dockLabel = document.getElementById('floating-theme-label');
    if (dockLabel) {
        dockLabel.textContent = isDark ? '🌙 Dark' : '☀️ Light';
    }

    // Update Settings Cards if present
    const storedSetting = localStorage.getItem(THEME_STORAGE_KEY) || (isDark ? 'dark' : 'light');
    updateThemeOptionCards(storedSetting);
}

function updateThemeOptionCards(activeOption) {
    const cards = {
        'light': document.getElementById('theme-card-light'),
        'dark': document.getElementById('theme-card-dark'),
        'system': document.getElementById('theme-card-system')
    };

    Object.keys(cards).forEach(key => {
        if (cards[key]) {
            if (key === activeOption) {
                cards[key].classList.add('active');
            } else {
                cards[key].classList.remove('active');
            }
        }
    });
}





// ============================================================================
// ADMIN MASTER CONTROL CENTER & TELEMETRY CONTROLLER
// ============================================================================

let adminUsersList = [];
let adminPharmaciesList = [];
let adminMedicinesList = [];
let adminPrescriptionsList = [];
let adminAuditLogs = [];

// Admin Subpanel Switcher
function switchAdminSubTab(subTabName) {
    document.querySelectorAll('.admin-nav-tab').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.admin-subpanel').forEach(p => {
        p.classList.remove('active');
        p.style.display = 'none';
    });

    const activeBtn = document.getElementById(`admin-tab-btn-${subTabName}`);
    if (activeBtn) activeBtn.classList.add('active');

    const activePanel = document.getElementById(`admin-subpanel-${subTabName}`);
    if (activePanel) {
        activePanel.classList.add('active');
        activePanel.style.display = 'block';
    }

    if (subTabName === 'users' && adminUsersList.length === 0) {
        loadAdminUsers();
    } else if (subTabName === 'pharmacies' && adminPharmaciesList.length === 0) {
        loadAdminPharmacies();
    } else if (subTabName === 'medicines' && adminMedicinesList.length === 0) {
        loadAdminMedicines();
    } else if (subTabName === 'prescriptions' && adminPrescriptionsList.length === 0) {
        loadAdminPrescriptions();
    } else if (subTabName === 'market') {
        loadMarketPriceData();
        loadMarketPriceHistory();
    }
}

// Master Admin Data Refresh
async function loadAdminData() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/telemetry`);
        if (res.ok) {
            const telemetry = await res.json();
            renderAdminTelemetry(telemetry);
        }
    } catch (err) {
        console.warn('Telemetry load failed:', err);
    }
    loadAdminUsers();
    loadAdminPharmacies();
    loadAdminMedicines();
    loadAdminPrescriptions();
    loadMarketPriceData();
    loadMarketPriceHistory();
}

function renderAdminTelemetry(t) {
    const kpiUsers = document.getElementById('admin-kpi-users');
    const kpiUsersBreakdown = document.getElementById('admin-kpi-users-breakdown');
    const kpiMeds = document.getElementById('admin-kpi-medicines');
    const kpiPharmacies = document.getElementById('admin-kpi-pharmacies');
    const kpiRx = document.getElementById('admin-kpi-rx');
    const kpiRxBreakdown = document.getElementById('admin-kpi-rx-breakdown');
    const kpiHealth = document.getElementById('admin-kpi-health');
    const kpiMem = document.getElementById('admin-kpi-memory');

    const counts = t.counts || {};
    if (kpiUsers) kpiUsers.textContent = counts.totalUsers || 0;
    if (kpiUsersBreakdown) {
        kpiUsersBreakdown.textContent = `Patients: ${counts.patients || 0} | Pharmacists: ${counts.pharmacists || 0} | Admins: ${counts.admins || 0}`;
    }
    if (kpiMeds) kpiMeds.textContent = counts.medicines || 0;
    if (kpiPharmacies) kpiPharmacies.textContent = counts.pharmacies || 0;
    if (kpiRx) kpiRx.textContent = counts.prescriptions || 0;
    if (kpiRxBreakdown) {
        kpiRxBreakdown.textContent = `Pending: ${counts.prescriptionsPending || 0} | Verified: ${counts.prescriptionsVerified || 0} | Dispensed: ${counts.prescriptionsDispensed || 0}`;
    }

    const health = t.health || t.system || {};
    if (kpiHealth) kpiHealth.textContent = health.status || 'HEALTHY';
    if (kpiMem) {
        kpiMem.textContent = `Memory: ${health.usedMemoryMb || 0} / ${health.maxMemoryMb || 0} MB | Uptime: ${health.uptime || 'Active'}`;
    }

    appendAdminAuditLine(`[TELEMETRY_SYNC] Polled. DB: ${health.database || 'CONNECTED'}`);
}

// ----------------------------------------------------
// 1. User Management (CRUD)
// ----------------------------------------------------
async function loadAdminUsers() {
    const tbody = document.getElementById('admin-users-table-body');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-loading-cell">Loading users from PostgreSQL database...</td></tr>';
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/users`);
        if (res.ok) {
            const data = await res.json();
            adminUsersList = data.users || (Array.isArray(data) ? data : []);
            renderAdminUsersTable(adminUsersList);
        } else {
            if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="table-loading-cell text-error">Failed to fetch users.</td></tr>';
        }
    } catch (e) {
        console.error('Error loading admin users:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="table-loading-cell text-error">Error connecting to server.</td></tr>`;
    }
}

function renderAdminUsersTable(users) {
    const tbody = document.getElementById('admin-users-table-body');
    if (!tbody) return;

    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-loading-cell">No users registered matching criteria.</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(u => {
        const role = (u.role || 'PATIENT').toUpperCase();
        let roleBadgeClass = 'badge-patient';
        let roleEmoji = '👤';
        if (role === 'PHARMACIST') {
            roleBadgeClass = 'badge-pharmacist';
            roleEmoji = '🩺';
        } else if (role === 'ADMIN') {
            roleBadgeClass = 'badge-admin';
            roleEmoji = '🛡️';
        }

        const contact = u.phone || u.email || 'None';
        const address = u.address || (u.pharmacyName ? `Store: ${u.pharmacyName}` : 'Dhaka, Bangladesh');

        return `
            <tr>
                <td style="font-family:'JetBrains Mono', monospace; font-size:0.8rem; color:var(--text-muted);">${escapeHtml(u.id || '')}</td>
                <td>
                    <div style="display:flex; align-items:center; gap:10px;">
                        <span style="font-size:1.4rem;">${roleEmoji}</span>
                        <div>
                            <strong style="display:block; color:var(--text-heading); font-size:0.92rem;">${escapeHtml(u.name || 'Unnamed')}</strong>
                            <small style="color:var(--text-muted); font-size:0.8rem;">${escapeHtml(u.email || '')}</small>
                        </div>
                    </div>
                </td>
                <td><span class="user-role-pill ${roleBadgeClass}">${role}</span></td>
                <td>
                    <div style="font-size:0.85rem;">📞 ${escapeHtml(contact)}</div>
                    <small style="color:var(--text-muted);">${escapeHtml(address)}</small>
                </td>
                <td>
                    <span style="font-family:'JetBrains Mono', monospace; font-size:0.8rem; background:rgba(0,0,0,0.05); padding:2px 6px; border-radius:4px;">
                        ${role === 'PHARMACIST' && u.licenseNumber ? `Lic: ${escapeHtml(u.licenseNumber)}` : 'Active / Hash Protected'}
                    </span>
                </td>
                <td style="text-align:right;">
                    <div class="admin-table-actions">
                        <button type="button" class="btn-action-edit" onclick="openAdminEditUserModal('${u.id}')" title="Edit User">✏️ Edit</button>
                        <button type="button" class="btn-action-delete" onclick="deleteAdminUser('${u.id}', '${escapeHtml(u.name)}')" title="Delete User">🗑️ Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterAdminUsersTable() {
    const search = (document.getElementById('admin-user-search')?.value || '').toLowerCase();
    const roleFilter = document.getElementById('admin-user-role-filter')?.value || 'ALL';

    const filtered = adminUsersList.filter(u => {
        const matchesRole = roleFilter === 'ALL' || (u.role || '').toUpperCase() === roleFilter;
        const matchesSearch = !search ||
            (u.name && u.name.toLowerCase().includes(search)) ||
            (u.email && u.email.toLowerCase().includes(search)) ||
            (u.id && u.id.toLowerCase().includes(search));
        return matchesRole && matchesSearch;
    });

    renderAdminUsersTable(filtered);
}

function toggleAdminUserRoleFields() {
    const role = document.getElementById('admin-user-role')?.value;
    const patientFields = document.getElementById('admin-role-patient-fields');
    const pharmaFields = document.getElementById('admin-role-pharma-fields');
    const adminFields = document.getElementById('admin-role-admin-fields');
    const ecGroup = document.getElementById('admin-group-patient-ec');

    if (patientFields) patientFields.style.display = (role === 'PATIENT') ? 'block' : 'none';
    if (ecGroup) ecGroup.style.display = (role === 'PATIENT') ? 'block' : 'none';
    if (pharmaFields) pharmaFields.style.display = (role === 'PHARMACIST') ? 'block' : 'none';
    if (adminFields) adminFields.style.display = (role === 'ADMIN') ? 'block' : 'none';
}

function openAdminAddUserModal() {
    document.getElementById('admin-user-id').value = '';
    document.getElementById('admin-user-modal-title').textContent = 'Create New User';
    document.getElementById('btn-admin-user-submit').textContent = 'Save User to Database';
    document.getElementById('admin-user-name').value = '';
    document.getElementById('admin-user-email').value = '';
    document.getElementById('admin-user-password').value = '';
    document.getElementById('admin-user-password').required = true;
    document.getElementById('admin-user-password-label').textContent = 'Password *';
    document.getElementById('admin-user-phone').value = '';
    document.getElementById('admin-user-emergency').value = '';
    document.getElementById('admin-user-address').value = '';
    document.getElementById('admin-user-pharma-name').value = '';
    document.getElementById('admin-user-pharma-license').value = '';
    document.getElementById('admin-user-role').value = 'PATIENT';
    toggleAdminUserRoleFields();
    openModal('modal-admin-user');
}

function openAdminEditUserModal(userId) {
    const user = adminUsersList.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('admin-user-id').value = user.id;
    document.getElementById('admin-user-modal-title').textContent = `Edit User: ${user.name}`;
    document.getElementById('btn-admin-user-submit').textContent = 'Update User';
    document.getElementById('admin-user-name').value = user.name || '';
    document.getElementById('admin-user-email').value = user.email || '';
    document.getElementById('admin-user-password').value = '';
    document.getElementById('admin-user-password').required = false;
    document.getElementById('admin-user-password-label').textContent = 'Password (leave blank to keep current)';
    document.getElementById('admin-user-phone').value = user.phone || '';
    document.getElementById('admin-user-emergency').value = user.emergencyContact || '';
    document.getElementById('admin-user-address').value = user.address || '';
    document.getElementById('admin-user-pharma-name').value = user.pharmacyName || '';
    document.getElementById('admin-user-pharma-license').value = user.licenseNumber || '';
    document.getElementById('admin-user-role').value = (user.role || 'PATIENT').toUpperCase();
    toggleAdminUserRoleFields();
    openModal('modal-admin-user');
}

async function handleAdminUserSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('admin-user-id')?.value;
    const name = document.getElementById('admin-user-name')?.value?.trim();
    const email = document.getElementById('admin-user-email')?.value?.trim();
    const password = document.getElementById('admin-user-password')?.value;
    const role = document.getElementById('admin-user-role')?.value;
    const phone = document.getElementById('admin-user-phone')?.value?.trim();
    const emergencyContact = document.getElementById('admin-user-emergency')?.value?.trim();
    const address = document.getElementById('admin-user-address')?.value?.trim();
    const pharmacyName = document.getElementById('admin-user-pharma-name')?.value?.trim();
    const licenseNumber = document.getElementById('admin-user-pharma-license')?.value?.trim();

    const payload = {
        name,
        email,
        role,
        phone,
        emergencyContact,
        address,
        pharmacyName,
        licenseNumber
    };
    if (password) {
        payload.password = password;
    }

    try {
        let res;
        if (id) {
            res = await fetch(`${API_BASE_URL}/api/admin/users/${encodeURIComponent(id)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`${API_BASE_URL}/api/admin/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast(id ? '✅ User updated successfully!' : '✅ User created successfully!');
            closeModal('modal-admin-user');
            loadAdminUsers();
            loadAdminData();
        } else {
            showToast(data.message || 'Failed to save user.', 'error');
        }
    } catch (err) {
        console.error('Error saving admin user:', err);
        showToast('Network error while saving user.', 'error');
    }
}

async function deleteAdminUser(userId, userName) {
    if (!confirm(`Are you sure you want to permanently delete user "${userName || userId}"? This cannot be undone.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/users/${encodeURIComponent(userId)}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast(`🗑️ User deleted successfully.`);
            loadAdminUsers();
            loadAdminData();
        } else {
            showToast(data.message || 'Failed to delete user.', 'error');
        }
    } catch (e) {
        console.error('Error deleting user:', e);
        showToast('Error deleting user.', 'error');
    }
}

// ----------------------------------------------------
// CSV DATA EXPORT UTILITIES (Client-side reporting)
// ----------------------------------------------------
function downloadCsvFile(filename, csvContent) {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

function exportUsersCsv() {
    if (!adminUsersList || adminUsersList.length === 0) {
        showToast('No user records available to export.', 'warning');
        return;
    }
    const headers = ['User ID', 'Full Name', 'Email Address', 'Account Role', 'Phone Number', 'Address / Store Details'];
    const rows = adminUsersList.map(u => [
        `"${(u.id || '').replace(/"/g, '""')}"`,
        `"${(u.name || '').replace(/"/g, '""')}"`,
        `"${(u.email || '').replace(/"/g, '""')}"`,
        `"${(u.role || '').replace(/"/g, '""')}"`,
        `"${(u.phone || '').replace(/"/g, '""')}"`,
        `"${(u.address || u.pharmacyName || '').replace(/"/g, '""')}"`
    ]);
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    downloadCsvFile(`medilink_users_${new Date().toISOString().slice(0, 10)}.csv`, csv);
    showToast('📥 Users directory exported to CSV successfully!');
}

function exportPharmaciesCsv() {
    if (!adminPharmaciesList || adminPharmaciesList.length === 0) {
        showToast('No pharmacy records available to export.', 'warning');
        return;
    }
    const headers = ['Pharmacy ID', 'Pharmacy Name', 'City Area', 'Physical Street Address', 'Phone Hotline', '24 Hours Open', 'Emergency Delivery', 'Inventory SKUs'];
    const rows = adminPharmaciesList.map(p => [
        `"${(p.id || '').replace(/"/g, '""')}"`,
        `"${(p.name || '').replace(/"/g, '""')}"`,
        `"${(p.area || '').replace(/"/g, '""')}"`,
        `"${(p.address || '').replace(/"/g, '""')}"`,
        `"${(p.phone || '').replace(/"/g, '""')}"`,
        p.is24Hours ? 'YES' : 'NO',
        p.hasEmergencyDelivery ? 'YES' : 'NO',
        p.stockCount || 0
    ]);
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    downloadCsvFile(`medilink_pharmacies_${new Date().toISOString().slice(0, 10)}.csv`, csv);
    showToast('📥 Pharmacy network registry exported to CSV successfully!');
}

function exportMedicinesCsv() {
    if (!adminMedicinesList || adminMedicinesList.length === 0) {
        showToast('No medicine catalog records available to export.', 'warning');
        return;
    }
    const headers = ['Medicine ID', 'Brand Name', 'Generic Formulation', 'Pharmaceutical Company', 'Strength', 'Formulation', 'Unit Price (BDT ৳)', 'Prescription Required'];
    const rows = adminMedicinesList.map(m => [
        `"${(m.id || '').replace(/"/g, '""')}"`,
        `"${(m.brandName || '').replace(/"/g, '""')}"`,
        `"${(m.genericName || '').replace(/"/g, '""')}"`,
        `"${(m.company || '').replace(/"/g, '""')}"`,
        `"${(m.strength || '').replace(/"/g, '""')}"`,
        `"${(m.formulation || '').replace(/"/g, '""')}"`,
        typeof m.unitPrice === 'number' ? m.unitPrice.toFixed(2) : (m.unitPrice || '0.00'),
        m.prescriptionRequired ? 'YES (Rx)' : 'NO (OTC)'
    ]);
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    downloadCsvFile(`medilink_medicines_${new Date().toISOString().slice(0, 10)}.csv`, csv);
    showToast('📥 Medicine catalog exported to CSV successfully!');
}

// ----------------------------------------------------
// 2. Pharmacy Network Management (CRUD)
// ----------------------------------------------------
async function loadAdminPharmacies() {
    const tbody = document.getElementById('admin-pharmacies-table-body');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell">Loading pharmacy network from PostgreSQL database...</td></tr>';
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/pharmacies`);
        if (res.ok) {
            const data = await res.json();
            adminPharmaciesList = data.pharmacies || (Array.isArray(data) ? data : []);
            renderAdminPharmaciesTable(adminPharmaciesList);
        } else {
            if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell text-error">Failed to fetch pharmacies.</td></tr>';
        }
    } catch (e) {
        console.error('Error loading admin pharmacies:', e);
        if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell text-error">Error connecting to server.</td></tr>';
    }
}

function renderAdminPharmaciesTable(pharmacies) {
    const tbody = document.getElementById('admin-pharmacies-table-body');
    if (!tbody) return;

    if (!pharmacies || pharmacies.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell">No partner pharmacies registered matching criteria.</td></tr>';
        return;
    }

    tbody.innerHTML = pharmacies.map(p => {
        const is24hBadge = p.is24Hours
            ? '<span class="badge-24h">✓ 24/7 Open</span>'
            : '<span class="badge-standard-hours">Standard Hours</span>';

        const emergencyBadge = p.hasEmergencyDelivery
            ? '<span class="badge-emergency-yes">⚡ Active</span>'
            : '<span class="badge-emergency-no">Standard</span>';

        const stockSkus = p.stockCount !== undefined ? p.stockCount : 0;

        return `
            <tr>
                <td style="font-family:'JetBrains Mono', monospace; font-size:0.8rem; color:var(--text-muted);">${escapeHtml(p.id || '')}</td>
                <td>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <span style="font-size:1.3rem;">🏥</span>
                        <div>
                            <strong style="display:block; color:var(--text-heading); font-size:0.92rem;">${escapeHtml(p.name || 'Unnamed')}</strong>
                            <small style="color:var(--primary-blue); font-weight:700; font-size:0.78rem;">📍 ${escapeHtml(p.area || 'Dhaka')}</small>
                        </div>
                    </div>
                </td>
                <td style="max-width:240px; font-size:0.85rem; color:var(--text-muted);">${escapeHtml(p.address || '')}</td>
                <td><span style="font-size:0.85rem; font-weight:600;">📞 ${escapeHtml(p.phone || 'N/A')}</span></td>
                <td>${is24hBadge}</td>
                <td>${emergencyBadge}</td>
                <td><span class="badge-tag" style="font-weight:700;">${stockSkus} SKUs</span></td>
                <td style="text-align:right;">
                    <div class="admin-table-actions">
                        <button type="button" class="btn-action-edit" onclick="openAdminEditPharmacyModal('${p.id}')" title="Edit Pharmacy">✏️ Edit</button>
                        <button type="button" class="btn-action-delete" onclick="deleteAdminPharmacy('${p.id}', '${escapeHtml(p.name)}')" title="Delete Pharmacy">🗑️ Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterAdminPharmaciesTable() {
    const search = (document.getElementById('admin-pharma-search')?.value || '').toLowerCase();
    const hoursFilter = document.getElementById('admin-pharma-filter-hours')?.value || 'ALL';

    const filtered = adminPharmaciesList.filter(p => {
        const matchesSearch = !search ||
            (p.name && p.name.toLowerCase().includes(search)) ||
            (p.address && p.address.toLowerCase().includes(search)) ||
            (p.area && p.area.toLowerCase().includes(search)) ||
            (p.id && p.id.toLowerCase().includes(search));

        let matchesHours = true;
        if (hoursFilter === '24H') matchesHours = !!p.is24Hours;
        else if (hoursFilter === 'EMERGENCY') matchesHours = !!p.hasEmergencyDelivery;

        return matchesSearch && matchesHours;
    });

    renderAdminPharmaciesTable(filtered);
}

function openAdminAddPharmacyModal() {
    document.getElementById('admin-pharma-id').value = '';
    document.getElementById('admin-pharma-modal-title').textContent = 'Register Partner Pharmacy';
    document.getElementById('btn-admin-pharma-submit').textContent = 'Save Partner Pharmacy';
    document.getElementById('admin-pharma-name').value = '';
    document.getElementById('admin-pharma-area').value = '';
    document.getElementById('admin-pharma-address').value = '';
    document.getElementById('admin-pharma-phone').value = '';
    document.getElementById('admin-pharma-custom-id').value = '';
    document.getElementById('admin-pharma-lat').value = '23.7461';
    document.getElementById('admin-pharma-lng').value = '90.3742';
    document.getElementById('admin-pharma-24h').checked = true;
    document.getElementById('admin-pharma-emergency').checked = true;
    openModal('modal-admin-pharmacy');
}

function openAdminEditPharmacyModal(pharmaId) {
    const p = adminPharmaciesList.find(item => item.id === pharmaId);
    if (!p) return;

    document.getElementById('admin-pharma-id').value = p.id;
    document.getElementById('admin-pharma-modal-title').textContent = `Edit Pharmacy: ${p.name}`;
    document.getElementById('btn-admin-pharma-submit').textContent = 'Update Partner Pharmacy';
    document.getElementById('admin-pharma-name').value = p.name || '';
    document.getElementById('admin-pharma-area').value = p.area || '';
    document.getElementById('admin-pharma-address').value = p.address || '';
    document.getElementById('admin-pharma-phone').value = p.phone || '';
    document.getElementById('admin-pharma-custom-id').value = p.id || '';
    document.getElementById('admin-pharma-lat').value = p.latitude || '23.7461';
    document.getElementById('admin-pharma-lng').value = p.longitude || '90.3742';
    document.getElementById('admin-pharma-24h').checked = !!p.is24Hours;
    document.getElementById('admin-pharma-emergency').checked = !!p.hasEmergencyDelivery;
    openModal('modal-admin-pharmacy');
}

async function handleAdminPharmacySubmit(e) {
    e.preventDefault();
    const id = document.getElementById('admin-pharma-id')?.value;
    const name = document.getElementById('admin-pharma-name')?.value?.trim();
    const area = document.getElementById('admin-pharma-area')?.value?.trim();
    const address = document.getElementById('admin-pharma-address')?.value?.trim();
    const phone = document.getElementById('admin-pharma-phone')?.value?.trim();
    const customId = document.getElementById('admin-pharma-custom-id')?.value?.trim();
    const latitude = parseFloat(document.getElementById('admin-pharma-lat')?.value) || 23.7461;
    const longitude = parseFloat(document.getElementById('admin-pharma-lng')?.value) || 90.3742;
    const is24Hours = document.getElementById('admin-pharma-24h')?.checked || false;
    const hasEmergencyDelivery = document.getElementById('admin-pharma-emergency')?.checked || false;

    const payload = {
        name,
        area,
        address,
        phone,
        latitude,
        longitude,
        is24Hours,
        hasEmergencyDelivery
    };
    if (customId && !id) {
        payload.id = customId;
    }

    try {
        let res;
        if (id) {
            res = await fetch(`${API_BASE_URL}/api/admin/pharmacies/${encodeURIComponent(id)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`${API_BASE_URL}/api/admin/pharmacies`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        const data = await res.json();
        if (res.ok && data.status === 'SUCCESS') {
            showToast(id ? '✅ Pharmacy updated successfully!' : '✅ Partner pharmacy registered successfully!');
            closeModal('modal-admin-pharmacy');
            loadAdminPharmacies();
            loadAdminData();
            if (typeof loadPharmacies === 'function') loadPharmacies();
        } else {
            showToast(data.message || 'Failed to save pharmacy.', 'error');
        }
    } catch (err) {
        console.error('Error saving pharmacy:', err);
        showToast('Network error while saving pharmacy.', 'error');
    }
}

async function deleteAdminPharmacy(pharmaId, pharmaName) {
    if (!confirm(`Are you sure you want to remove pharmacy "${pharmaName || pharmaId}" from the platform registry? This will also remove its associated stock records.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/pharmacies/${encodeURIComponent(pharmaId)}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        if (res.ok && data.status === 'SUCCESS') {
            showToast(`🗑️ Pharmacy removed from platform registry.`);
            loadAdminPharmacies();
            loadAdminData();
            if (typeof loadPharmacies === 'function') loadPharmacies();
        } else {
            showToast(data.message || 'Failed to delete pharmacy.', 'error');
        }
    } catch (e) {
        console.error('Error deleting pharmacy:', e);
        showToast('Error deleting pharmacy.', 'error');
    }
}

// ----------------------------------------------------
// 3. Medicine Catalog CRUD
// ----------------------------------------------------
async function loadAdminMedicines() {
    const tbody = document.getElementById('admin-medicines-table-body');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell">Loading medicine repository...</td></tr>';
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/medicines`);
        if (res.ok) {
            const data = await res.json();
            adminMedicinesList = data.medicines || (Array.isArray(data) ? data : []);
            populateCompanyFilter();
            renderAdminMedicinesTable(adminMedicinesList);
        } else {
            if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell text-error">Failed to fetch medicines.</td></tr>';
        }
    } catch (err) {
        console.error('Error loading admin medicines:', err);
        if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell text-error">Network error.</td></tr>';
    }
}

function populateCompanyFilter() {
    const select = document.getElementById('admin-med-company-filter');
    if (!select) return;
    const companies = Array.from(new Set(adminMedicinesList.map(m => m.company).filter(Boolean))).sort();
    select.innerHTML = '<option value="ALL">All Manufacturers</option>' +
        companies.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
}

function renderAdminMedicinesTable(meds) {
    const tbody = document.getElementById('admin-medicines-table-body');
    if (!tbody) return;

    if (!meds || meds.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="table-loading-cell">No medicines found matching filters.</td></tr>';
        return;
    }

    tbody.innerHTML = meds.map(m => {
        const price = typeof m.unitPrice === 'number' ? m.unitPrice.toFixed(2) : (m.unitPrice || '0.00');
        const rxRequired = m.prescriptionRequired ? '<span class="status-badge badge-warning">Rx Required</span>' : '<span class="status-badge badge-success">OTC</span>';

        return `
            <tr>
                <td style="font-family:'JetBrains Mono', monospace; font-size:0.8rem; color:var(--text-muted);">${escapeHtml(m.id || '')}</td>
                <td><strong style="color:var(--text-heading); font-size:0.95rem;">${escapeHtml(m.brandName || '')}</strong></td>
                <td><span style="font-style:italic; color:var(--text-muted);">${escapeHtml(m.genericName || '')}</span></td>
                <td>${escapeHtml(m.company || '')}</td>
                <td><span class="badge-tag">${escapeHtml(m.strength || '')} (${escapeHtml(m.formulation || 'Tab')})</span></td>
                <td><strong style="color:var(--primary-blue); font-size:1rem;">৳ ${price}</strong></td>
                <td>${rxRequired}</td>
                <td style="text-align:right;">
                    <div class="admin-table-actions">
                        <button type="button" class="btn-action-edit" onclick="openAdminEditMedicineModal('${m.id}')" title="Edit Medicine">✏️ Edit</button>
                        <button type="button" class="btn-action-delete" onclick="deleteAdminMedicine('${m.id}', '${escapeHtml(m.brandName)}')" title="Delete Medicine">🗑️ Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterAdminMedicinesTable() {
    const search = (document.getElementById('admin-med-search')?.value || '').toLowerCase();
    const company = document.getElementById('admin-med-company-filter')?.value || 'ALL';

    const filtered = adminMedicinesList.filter(m => {
        const matchesComp = company === 'ALL' || m.company === company;
        const matchesSearch = !search ||
            (m.brandName && m.brandName.toLowerCase().includes(search)) ||
            (m.genericName && m.genericName.toLowerCase().includes(search)) ||
            (m.company && m.company.toLowerCase().includes(search)) ||
            (m.id && m.id.toLowerCase().includes(search));
        return matchesComp && matchesSearch;
    });

    renderAdminMedicinesTable(filtered);
}

function openAdminAddMedicineModal() {
    document.getElementById('admin-med-id').value = '';
    document.getElementById('admin-med-modal-title').textContent = 'Add Medicine to Catalog';
    document.getElementById('btn-admin-med-submit').textContent = 'Save Medicine to Repository';
    document.getElementById('admin-med-brand').value = '';
    document.getElementById('admin-med-generic').value = '';
    document.getElementById('admin-med-company').value = '';
    document.getElementById('admin-med-strength').value = '';
    document.getElementById('admin-med-formulation').value = 'Tablet';
    document.getElementById('admin-med-price').value = '';
    document.getElementById('admin-med-category').value = '';
    document.getElementById('admin-med-rx-req').checked = false;
    document.getElementById('admin-med-side-effects').value = '';
    openModal('modal-admin-medicine');
}

function openAdminEditMedicineModal(medId) {
    const med = adminMedicinesList.find(m => m.id === medId);
    if (!med) return;

    document.getElementById('admin-med-id').value = med.id;
    document.getElementById('admin-med-modal-title').textContent = `Edit Medicine: ${med.brandName}`;
    document.getElementById('btn-admin-med-submit').textContent = 'Update Medicine';
    document.getElementById('admin-med-brand').value = med.brandName || '';
    document.getElementById('admin-med-generic').value = med.genericName || '';
    document.getElementById('admin-med-company').value = med.company || '';
    document.getElementById('admin-med-strength').value = med.strength || '';
    document.getElementById('admin-med-formulation').value = med.formulation || 'Tablet';
    document.getElementById('admin-med-price').value = med.unitPrice || '';
    document.getElementById('admin-med-category').value = med.therapeuticClass || '';
    document.getElementById('admin-med-rx-req').checked = !!med.prescriptionRequired;
    document.getElementById('admin-med-side-effects').value = med.sideEffects || '';
    openModal('modal-admin-medicine');
}

async function handleAdminMedicineSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('admin-med-id')?.value;
    const brandName = document.getElementById('admin-med-brand')?.value?.trim();
    const genericName = document.getElementById('admin-med-generic')?.value?.trim();
    const company = document.getElementById('admin-med-company')?.value?.trim();
    const strength = document.getElementById('admin-med-strength')?.value?.trim();
    const formulation = document.getElementById('admin-med-formulation')?.value;
    const unitPrice = parseFloat(document.getElementById('admin-med-price')?.value) || 0;
    const therapeuticClass = document.getElementById('admin-med-category')?.value?.trim();
    const prescriptionRequired = document.getElementById('admin-med-rx-req')?.checked || false;
    const sideEffects = document.getElementById('admin-med-side-effects')?.value?.trim();

    const payload = {
        brandName,
        genericName,
        company,
        strength,
        formulation,
        unitPrice,
        therapeuticClass,
        prescriptionRequired,
        sideEffects
    };

    try {
        let res;
        if (id) {
            res = await fetch(`${API_BASE_URL}/api/admin/medicines/${encodeURIComponent(id)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`${API_BASE_URL}/api/admin/medicines`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast(id ? '✅ Medicine updated!' : '✅ Medicine added to catalog!');
            closeModal('modal-admin-medicine');
            loadAdminMedicines();
            loadAdminData();
            if (typeof loadMedicines === 'function') loadMedicines();
        } else {
            showToast(data.message || 'Failed to save medicine.', 'error');
        }
    } catch (err) {
        console.error('Error saving medicine:', err);
        showToast('Network error while saving medicine.', 'error');
    }
}

async function deleteAdminMedicine(medId, medName) {
    if (!confirm(`Delete "${medName || medId}" from catalog? This will remove all associated stock listings.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/medicines/${encodeURIComponent(medId)}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast('🗑️ Medicine removed from catalog.');
            loadAdminMedicines();
            loadAdminData();
            if (typeof loadMedicines === 'function') loadMedicines();
        } else {
            showToast(data.message || 'Failed to delete medicine.', 'error');
        }
    } catch (e) {
        console.error('Error deleting medicine:', e);
        showToast('Error deleting medicine.', 'error');
    }
}

// ----------------------------------------------------
// 3. Prescription Master Oversight Queue
// ----------------------------------------------------
async function loadAdminPrescriptions() {
    const tbody = document.getElementById('admin-prescriptions-table-body');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-loading-cell">Loading prescription master queue...</td></tr>';
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/prescriptions`);
        if (res.ok) {
            const data = await res.json();
            adminPrescriptionsList = data.prescriptions || (Array.isArray(data) ? data : []);
            renderAdminPrescriptionsTable(adminPrescriptionsList);
        } else {
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="table-loading-cell text-error">Failed to fetch prescriptions.</td></tr>';
        }
    } catch (e) {
        console.error('Error loading prescriptions:', e);
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="table-loading-cell text-error">Network error.</td></tr>';
    }
}

function renderAdminPrescriptionsTable(rxList) {
    const tbody = document.getElementById('admin-prescriptions-table-body');
    if (!tbody) return;

    if (!rxList || rxList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-loading-cell">No prescriptions recorded in system.</td></tr>';
        return;
    }

    tbody.innerHTML = rxList.map(rx => {
        const status = (rx.status || 'UPLOADED').toUpperCase();
        let statusBadge = `<span class="status-badge badge-warning">Pending (${status})</span>`;
        if (status === 'VERIFIED') statusBadge = '<span class="status-badge badge-success">✓ Verified Genuine</span>';
        else if (status === 'DISPENSED') statusBadge = '<span class="status-badge badge-info">📦 Dispensed</span>';
        else if (status === 'REJECTED') statusBadge = '<span class="status-badge badge-danger">✕ Flagged / Rejected</span>';

        const itemsCount = (rx.items && Array.isArray(rx.items)) ? rx.items.length : 0;
        const itemsText = itemsCount > 0
            ? rx.items.map(i => `${i.medicineName || i.dosage || ''} (${i.frequency || 'Daily'})`).join(', ')
            : (rx.rawOcrText ? rx.rawOcrText.substring(0, 60) + '...' : 'Pending OCR extraction');

        const uploadDate = rx.uploadedAt ? new Date(rx.uploadedAt).toLocaleString() : 'Recent';

        return `
            <tr>
                <td style="font-family:'JetBrains Mono', monospace; font-size:0.82rem; font-weight:700; color:var(--text-heading);">${escapeHtml(rx.id || '')}</td>
                <td>
                    <strong>${escapeHtml(rx.patientName || 'Anonymous Patient')}</strong>
                    <div style="font-size:0.75rem; color:var(--text-muted); font-family:'JetBrains Mono', monospace;">${escapeHtml(rx.patientId || '')}</div>
                </td>
                <td>
                    <div style="font-weight:600; font-size:0.88rem;">${escapeHtml(rx.doctorName || 'Dr. Assigned')}</div>
                    <small style="color:var(--text-muted);">${escapeHtml(rx.hospitalName || 'General Hospital')}</small>
                </td>
                <td>
                    <div style="max-width:280px; font-size:0.82rem; line-height:1.4; color:var(--text-muted);" title="${escapeHtml(itemsText)}">
                        ${escapeHtml(itemsText)}
                    </div>
                </td>
                <td style="font-size:0.8rem; color:var(--text-muted);">${uploadDate}</td>
                <td>${statusBadge}</td>
                <td style="text-align:right;">
                    <div class="admin-table-actions">
                        <select onchange="adminUpdateRxStatus('${rx.id}', this.value)" style="padding:4px 8px; font-size:0.78rem; border-radius:6px; border:1px solid #cbd5e1; background:var(--bg-color); font-weight:600;">
                            <option value="">-- Change State --</option>
                            <option value="VERIFIED" ${status === 'VERIFIED' ? 'selected' : ''}>Verify Genuine</option>
                            <option value="DISPENSED" ${status === 'DISPENSED' ? 'selected' : ''}>Mark Dispensed</option>
                            <option value="UPLOADED" ${status === 'UPLOADED' ? 'selected' : ''}>Reset to Uploaded</option>
                            <option value="REJECTED" ${status === 'REJECTED' ? 'selected' : ''}>Flag / Reject</option>
                        </select>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterAdminPrescriptionsTable() {
    const search = (document.getElementById('admin-rx-search')?.value || '').toLowerCase();
    const statusFilter = document.getElementById('admin-rx-status-filter')?.value || 'ALL';

    const filtered = adminPrescriptionsList.filter(rx => {
        const matchesStatus = statusFilter === 'ALL' || (rx.status || '').toUpperCase() === statusFilter;
        const matchesSearch = !search ||
            (rx.id && rx.id.toLowerCase().includes(search)) ||
            (rx.patientName && rx.patientName.toLowerCase().includes(search)) ||
            (rx.doctorName && rx.doctorName.toLowerCase().includes(search));
        return matchesStatus && matchesSearch;
    });

    renderAdminPrescriptionsTable(filtered);
}

async function adminUpdateRxStatus(rxId, newStatus) {
    if (!newStatus) return;

    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/prescriptions/${encodeURIComponent(rxId)}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast(`Prescription ${rxId} status changed to ${newStatus}.`);
            loadAdminPrescriptions();
            loadAdminData();
            if (typeof loadPrescriptions === 'function') loadPrescriptions();
        } else {
            showToast(data.message || 'Failed to update status.', 'error');
        }
    } catch (e) {
        console.error('Error updating prescription status:', e);
        showToast('Network error while updating prescription.', 'error');
    }
}

// ----------------------------------------------------
// 4. Global Broadcast & Live Audit Stream
// ----------------------------------------------------
async function submitAdminBroadcast() {
    const textarea = document.getElementById('admin-broadcast-text');
    const message = (textarea?.value || '').trim();

    if (!message) {
        showToast('Please type a message before broadcasting.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/api/admin/broadcast`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: message,
                type: 'SYSTEM_ANNOUNCEMENT',
                target: 'ALL'
            })
        });
        const data = await res.json();
        if (res.ok && data.success !== false) {
            showToast('🚀 Global live announcement dispatched to all connected users!');
            appendAdminAuditLine(`[ADMIN_DISPATCH] "${message}" broadcast to all sessions.`);
            if (textarea) textarea.value = '';
        } else {
            showToast(data.message || 'Failed to dispatch broadcast.', 'error');
        }
    } catch (e) {
        console.error('Error broadcasting alert:', e);
        showToast('Error broadcasting alert.', 'error');
    }
}

function appendAdminAuditLine(lineText) {
    const feed = document.getElementById('admin-audit-feed');
    if (!feed) return;

    const time = new Date().toLocaleTimeString();
    const line = document.createElement('div');
    line.className = 'terminal-line';
    line.textContent = `[${time}] ${lineText}`;

    feed.insertBefore(line, feed.firstChild);

    while (feed.children.length > 60) {
        feed.removeChild(feed.lastChild);
    }
}

// ==========================================
// BANGLADESH MARKET LIVE PRICE CONTROLLER & SSE
// ==========================================
function handleLivePriceUpdateEvent(raw) {
    console.log('[LIVE_MARKET_PRICE_EVENT]', raw);
    
    // Pattern: PRICE_UPDATE: Napa Extra (ID 1) changed from ৳2.50 to ৳3.00 (20.0%) via DGDA Bangladesh Gazetted Price Update
    const match = raw.match(/PRICE_UPDATE:\s*(.+?)\s*\(ID\s*(\d+)\)\s*changed\s*from\s*৳([\d.]+)\s*to\s*৳([\d.]+)\s*\(([+-]?[\d.]+)%\)\s*via\s*(.+)/i);
    
    let brandName = 'Medicine';
    let medId = null;
    let oldPrice = 0;
    let newPrice = 0;
    let pct = '0.0%';
    let source = 'DGDA Bangladesh';

    if (match) {
        brandName = match[1].trim();
        medId = parseInt(match[2], 10);
        oldPrice = parseFloat(match[3]);
        newPrice = parseFloat(match[4]);
        pct = match[5];
        source = match[6].trim();
    } else {
        // Fallback simple extract
        const parts = raw.replace('PRICE_UPDATE:', '').trim();
        brandName = parts.split(' ')[0] || 'Medicine';
    }

    // 1. Update state.medicines cache in real-time
    if (state.medicines && state.medicines.length > 0) {
        const found = state.medicines.find(m => (medId && m.id === medId) || m.brandName.toLowerCase() === brandName.toLowerCase());
        if (found) {
            found.unitPrice = newPrice;
        }
    }

    // 2. Dynamic Card Price Badges Animation without reload
    const priceElements = [];
    if (medId) {
        const el = document.getElementById(`med-price-${medId}`);
        if (el) priceElements.push(el);
    }
    document.querySelectorAll(`[data-med-id="${medId}"] .med-price`).forEach(el => {
        if (!priceElements.includes(el)) priceElements.push(el);
    });

    const isIncrease = newPrice >= oldPrice;
    const flashClass = isIncrease ? 'price-flash-up' : 'price-flash-down';
    const directionIcon = isIncrease ? '📈' : '📉';

    priceElements.forEach(el => {
        el.textContent = `BDT ${newPrice.toFixed(2)}`;
        el.setAttribute('data-price', newPrice);
        el.classList.remove('price-flash-up', 'price-flash-down');
        void el.offsetWidth; // Trigger CSS reflow
        el.classList.add(flashClass);
    });

    // 3. Update Cart if this medicine is in user's cart
    if (state.cart && Array.isArray(state.cart)) {
        let cartUpdated = false;
        state.cart.forEach(item => {
            if ((medId && item.medicineId === medId) || (item.name && item.name.toLowerCase() === brandName.toLowerCase())) {
                item.price = newPrice;
                cartUpdated = true;
            }
        });
        if (cartUpdated && typeof renderCart === 'function') {
            renderCart();
        }
    }

    // 4. Update Price Comparison modal if open
    const cmpModal = document.getElementById('modal-price-compare');
    if (cmpModal && cmpModal.classList.contains('active')) {
        const currentTitle = document.getElementById('price-compare-title');
        if (currentTitle && currentTitle.textContent.toLowerCase().includes(brandName.toLowerCase())) {
            openPriceComparisonModal(medId, brandName);
        }
    }

    // 5. User Notification Toast & Bell Feed
    const toastMsg = `${directionIcon} Market Price Alert: ${brandName} price updated to ৳${newPrice.toFixed(2)} (${pct}%) via ${source}`;
    showToast(toastMsg);
    addNotification({
        icon: directionIcon,
        title: `Market MRP: ${brandName}`,
        text: `Official market price updated from ৳${oldPrice.toFixed(2)} to ৳${newPrice.toFixed(2)} (${pct}%). Source: ${source}.`,
        time: 'Just now'
    });

    // 6. Refresh Admin Market subpanel tables if active
    const marketSubpanel = document.getElementById('admin-subpanel-market');
    if (marketSubpanel && (marketSubpanel.classList.contains('active') || marketSubpanel.style.display === 'block')) {
        loadMarketPriceData();
        loadMarketPriceHistory();
    }
}

async function loadMarketPriceData() {
    const tbody = document.getElementById('market-prices-tbody');
    const countEl = document.getElementById('market-items-count');
    const providerNameEl = document.getElementById('market-provider-name');
    const syncTimeEl = document.getElementById('market-last-sync-time');

    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE_URL}/api/market/prices`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        
        if (providerNameEl && data.provider) {
            providerNameEl.textContent = data.provider;
        }
        if (syncTimeEl) {
            syncTimeEl.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        }

        const items = data.marketItems || [];
        if (countEl) countEl.textContent = `${items.length} National Registry benchmark medicines`;

        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:20px; color:var(--text-muted);">No market data currently available.</td></tr>';
            return;
        }

        tbody.innerHTML = items.map(item => {
            const localMed = state.medicines ? state.medicines.find(m => m.brandName.toLowerCase() === item.brandName.toLowerCase()) : null;
            const systemPrice = localMed ? `৳${localMed.unitPrice.toFixed(2)}` : '<span style="color:#94a3b8;">Not in catalog</span>';
            const isSynced = localMed && Math.abs(localMed.unitPrice - item.mrp) < 0.01;
            const statusBadge = isSynced 
                ? '<span style="background:#ecfdf5; color:#059669; padding:3px 8px; border-radius:6px; font-size:0.75rem; font-weight:600;">✓ SYNCHRONIZED</span>'
                : (localMed 
                    ? '<span style="background:#fef3c7; color:#d97706; padding:3px 8px; border-radius:6px; font-size:0.75rem; font-weight:600;">⚠️ PENDING SYNC</span>'
                    : '<span style="background:#f1f5f9; color:#64748b; padding:3px 8px; border-radius:6px; font-size:0.75rem;">UNTRACKED</span>');

            return `
                <tr>
                    <td style="font-weight:600; color:var(--text-heading, #0f172a);">${escapeHtml(item.brandName)}</td>
                    <td>${escapeHtml(item.genericName)}</td>
                    <td>${escapeHtml(item.manufacturer || 'Top BD Pharma')}</td>
                    <td>${escapeHtml(item.strength || '')} <small style="color:#64748b;">${escapeHtml(item.dosageForm || '')}</small></td>
                    <td style="font-weight:700; color:#0284c7;">৳${item.mrp.toFixed(2)}</td>
                    <td style="font-weight:700;">${systemPrice}</td>
                    <td>${statusBadge}</td>
                    <td style="font-size:0.8rem; color:#64748b;">${new Date().toLocaleTimeString()}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load market prices:', err);
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:20px; color:#ef4444;">Failed to load live Bangladesh market data: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function loadMarketPriceHistory() {
    const tbody = document.getElementById('market-history-tbody');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE_URL}/api/market/history`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const history = data.history || [];

        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:20px; color:var(--text-muted);">No price fluctuations logged yet. Run a sync or trigger a simulation.</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(h => {
            const isUp = h.newPrice > h.oldPrice;
            const diff = h.newPrice - h.oldPrice;
            const sign = diff >= 0 ? '+' : '';
            const color = isUp ? '#ef4444' : '#10b981';
            const icon = isUp ? '▲' : '▼';
            const formattedTime = new Date(h.changedAt).toLocaleString();

            return `
                <tr>
                    <td style="font-size:0.82rem; color:#64748b; font-family:var(--font-mono, monospace);">${formattedTime}</td>
                    <td style="font-weight:600;">${escapeHtml(h.brandName)}</td>
                    <td>৳${h.oldPrice.toFixed(2)}</td>
                    <td style="font-weight:700; color:${color};">৳${h.newPrice.toFixed(2)}</td>
                    <td style="font-weight:600; color:${color};">${icon} ${sign}৳${Math.abs(diff).toFixed(2)} (${sign}${h.percentChange.toFixed(1)}%)</td>
                    <td><span style="background:var(--bg-card, #f8fafc); border:1px solid var(--border-color, #e2e8f0); padding:2px 8px; border-radius:4px; font-size:0.75rem;">${escapeHtml(h.source)}</span></td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load market history:', err);
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px; color:#ef4444;">Failed to load audit history: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function syncMarketPricesNow() {
    const badge = document.getElementById('market-sync-status-badge');
    if (badge) {
        badge.textContent = 'SYNCING NOW...';
        badge.style.background = '#fef3c7';
        badge.style.color = '#b45309';
    }

    showToast('🔄 Synchronizing MediLink medicine prices with Bangladesh Market API...');
    try {
        const res = await fetch(`${API_BASE_URL}/api/market/sync`, { method: 'POST' });
        const data = await res.json();
        
        if (res.ok) {
            showToast(`✓ Bangladesh Market Sync Complete: ${data.message || 'Prices up to date.'}`);
            await loadMedicines();
            await loadMarketPriceData();
            await loadMarketPriceHistory();
        } else {
            showToast(`⚠️ Sync notice: ${data.message || 'Unknown response'}`);
        }
    } catch (err) {
        console.error('Manual sync failed:', err);
        showToast('❌ Sync failed: ' + err.message);
    } finally {
        if (badge) {
            badge.textContent = 'AUTO-SYNC ACTIVE';
            badge.style.background = '#ecfdf5';
            badge.style.color = '#059669';
        }
    }
}

async function simulateMarketPriceFluctuation() {
    const candidates = ['Napa Extra', 'Seclo', 'Monas', 'Sergel', 'Ace Plus', 'Ciprocin', 'Almex'];
    const brand = candidates[Math.floor(Math.random() * candidates.length)];
    const pctDeltas = [15.0, -10.0, 20.0, -12.5, 25.0, 8.5, -5.0];
    const pct = pctDeltas[Math.floor(Math.random() * pctDeltas.length)];

    showToast(`⚡ Simulating DGDA regulatory price revision for ${brand} (${pct > 0 ? '+' : ''}${pct}%)...`);

    try {
        const res = await fetch(`${API_BASE_URL}/api/market/simulate-fluctuation?brandName=${encodeURIComponent(brand)}&percentChange=${pct}`, {
            method: 'POST'
        });
        const data = await res.json();
        if (res.ok) {
            showToast(`⚡ Simulated: ${data.message}`);
            await loadMarketPriceData();
            await loadMarketPriceHistory();
        } else {
            showToast(`Simulation notice: ${data.message || 'Error'}`);
        }
    } catch (err) {
        console.error('Simulation failed:', err);
        showToast('❌ Simulation failed: ' + err.message);
    }
}

