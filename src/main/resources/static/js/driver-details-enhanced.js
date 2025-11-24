document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('driverForm');
  const vehicleType = document.getElementById('vehicleType');
  const numberPlate = document.getElementById('numberPlate');
  const messageEl = document.getElementById('message');
  const summaryEl = document.getElementById('summary');
  const summaryVehicle = document.getElementById('summaryVehicle');
  const summaryPlate = document.getElementById('summaryPlate');
  const submitBtn = document.getElementById('submitBtn');
  const basicArea = document.getElementById('basicArea');

  // read onboarding data
  let onboard = null;
  try {
    const raw = sessionStorage.getItem('driverOnboard') || sessionStorage.getItem('pendingDriver');
    if (raw) onboard = JSON.parse(raw);
  } catch (e) {
    onboard = null;
  }

  // if onboard exists, prefill but hide basic fields
  if (onboard) {
    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const pwdInput = document.getElementById('password');
    if (nameInput) nameInput.value = onboard.name || '';
    if (emailInput) emailInput.value = onboard.email || '';
    if (pwdInput) pwdInput.value = onboard.password || '';
    // hide the basic area
    if (basicArea) basicArea.style.display = 'none';
    messageEl.innerHTML = '<div class="msg small">Basic details taken from the previous step.</div>';
  }

  function showMessage(type, text) {
    messageEl.innerHTML = `<div class="msg ${type === 'error' ? 'error' : 'success'}">${text}</div>`;
  }

  function clearMessage() { messageEl.innerHTML = ''; }

  function validatePlate(plate) {
    // basic normalization and check: at least 3 characters
    if (!plate) return false;
    const p = plate.replace(/\s+/g, '').toUpperCase();
    return p.length >= 3;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearMessage();
    const vType = vehicleType.value;
    const plateRaw = numberPlate.value.trim();
    const plate = plateRaw.toUpperCase();

    if (!vType) { showMessage('error', 'Please select a vehicle type'); return; }
    if (!validatePlate(plate)) { showMessage('error', 'Please enter a valid vehicle number plate'); return; }

    // assemble payload using onboard if present
    const payload = {};
    if (onboard && onboard.name) {
      payload.name = onboard.name;
      payload.email = onboard.email;
      payload.password = onboard.password;
    } else {
      // if basicArea visible, take from inputs
      const name = document.getElementById('name') ? document.getElementById('name').value.trim() : '';
      const email = document.getElementById('email') ? document.getElementById('email').value.trim() : '';
      const password = document.getElementById('password') ? document.getElementById('password').value.trim() : '';
      if (!name || !email || !password) { showMessage('error', 'Please fill name, email and password'); return; }
      payload.name = name; payload.email = email; payload.password = password;
    }
    payload.vehicleType = vType; payload.numberPlate = plate;

    // disable UI
    submitBtn.disabled = true; submitBtn.textContent = 'Registering...';

    try {
      const res = await fetch('/api/auth/driver/register', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (!res.ok) {
        showMessage('error', data.error || JSON.stringify(data));
        submitBtn.disabled = false; submitBtn.textContent = 'Register as Driver';
        return;
      }
      // success: clear onboard storage and show summary
      try { sessionStorage.removeItem('driverOnboard'); sessionStorage.removeItem('pendingDriver'); } catch(e){}
      if (basicArea) basicArea.style.display = 'none';
      document.getElementById('vehicleArea').style.display = 'none';
      submitBtn.style.display = 'none';
      summaryVehicle.textContent = payload.vehicleType;
      summaryPlate.textContent = payload.numberPlate;
      summaryEl.style.display = 'block';
      showMessage('success', 'Driver registered and pending admin approval');
    } catch (err) {
      showMessage('error', 'Network error. Please try again');
      submitBtn.disabled = false; submitBtn.textContent = 'Register as Driver';
    }
  });
});

