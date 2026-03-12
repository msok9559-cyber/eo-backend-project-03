window.addEventListener('DOMContentLoaded', function () {

    /* 상태 변수 */
    let isEmailVerified = false;

    /* 요소 */
    const emailInput        = document.getElementById('email');
    const sendEmailBtn      = document.getElementById('send-email-btn');
    const emailMsg          = document.getElementById('email-msg');

    const verifyGroup       = document.getElementById('verify-group');
    const verifyCodeInput   = document.getElementById('verify-code');
    const verifyBtn         = document.getElementById('verify-btn');
    const verifyMsg         = document.getElementById('verify-msg');

    const newPasswordGroup  = document.getElementById('reset-new-password-group');
    const newPasswordInput  = document.getElementById('reset-new-password');
    const newPasswordConfirm = document.getElementById('reset-new-password-confirm');
    const passwordMsg       = document.getElementById('reset-password-msg');

    const resetBtn          = document.getElementById('reset-btn');
    const resetError        = document.getElementById('reset-error');
    const resetErrorMsg     = document.getElementById('reset-error-msg');

    /* 이메일 인증번호 발송 */
    sendEmailBtn.addEventListener('click', async function () {
        const email = emailInput.value.trim();
        if (!email) {
            setMsg(emailMsg, '이메일을 입력해주세요.', 'error');
            return;
        }
        if (!isValidEmail(email)) {
            setMsg(emailMsg, '올바른 이메일 형식을 입력해주세요.', 'error');
            return;
        }

        sendEmailBtn.disabled = true;
        sendEmailBtn.textContent = '발송 중...';
        try {
            const res = await fetch(`/api/email/send-verification?email=${encodeURIComponent(email)}`);
            const data = await res.json();
            if (data.success) {
                setMsg(emailMsg, '인증번호가 발송되었습니다.', 'success');
                verifyGroup.style.display = 'flex';
                isEmailVerified = false;
                verifyCodeInput.value = '';
                verifyMsg.textContent = '';
                verifyMsg.className = 'field-msg';
                // 60초 재발송 타이머
                let count = 60;
                sendEmailBtn.textContent = `재발송 (${count}s)`;
                const timer = setInterval(() => {
                    count--;
                    sendEmailBtn.textContent = `재발송 (${count}s)`;
                    if (count <= 0) {
                        clearInterval(timer);
                        sendEmailBtn.disabled = false;
                        sendEmailBtn.textContent = '재발송';
                    }
                }, 1000);
            } else {
                setMsg(emailMsg, data.message || '발송에 실패했습니다.', 'error');
                sendEmailBtn.disabled = false;
                sendEmailBtn.textContent = '인증번호 발송';
            }
        } catch (e) {
            setMsg(emailMsg, '발송 중 오류가 발생했습니다.', 'error');
            sendEmailBtn.disabled = false;
            sendEmailBtn.textContent = '인증번호 발송';
        }
    });

    /* 인증번호 확인 */
    verifyBtn.addEventListener('click', async function () {
        const email = emailInput.value.trim();
        const code  = verifyCodeInput.value.trim();
        if (!code) {
            setMsg(verifyMsg, '인증번호를 입력해주세요.', 'error');
            return;
        }

        verifyBtn.disabled = true;
        try {
            const res = await fetch(`/api/email/verify-code?email=${encodeURIComponent(email)}&code=${encodeURIComponent(code)}`);
            const data = await res.json();
            if (data.success) {
                setMsg(verifyMsg, '인증이 완료되었습니다.', 'success');
                isEmailVerified = true;
                verifyCodeInput.disabled = true;
                verifyBtn.disabled = true;
                emailInput.disabled = true;
                sendEmailBtn.disabled = true;
                // 새 비밀번호 칸 + 재설정 버튼 표시
                newPasswordGroup.style.display = 'flex';
                resetBtn.style.display = 'block';
            } else {
                setMsg(verifyMsg, data.message || '인증번호가 일치하지 않습니다.', 'error');
                isEmailVerified = false;
                verifyBtn.disabled = false;
            }
        } catch (e) {
            setMsg(verifyMsg, '확인 중 오류가 발생했습니다.', 'error');
            verifyBtn.disabled = false;
        }
    });

    /* 새 비밀번호 확인 실시간 */
    newPasswordConfirm.addEventListener('input', function () {
        if (!newPasswordConfirm.value) {
            passwordMsg.textContent = '';
            passwordMsg.className = 'field-msg';
            return;
        }
        if (newPasswordInput.value === newPasswordConfirm.value) {
            setMsg(passwordMsg, '비밀번호가 일치합니다.', 'success');
        } else {
            setMsg(passwordMsg, '비밀번호가 일치하지 않습니다.', 'error');
        }
    });

    /*  비밀번호 재설정 버튼 */
    resetBtn.addEventListener('click', async function () {
        hideError();

        const email    = emailInput.value.trim();
        const password = newPasswordInput.value.trim();
        const passwordConfirm = newPasswordConfirm.value.trim();

        if (!isEmailVerified) return showError('이메일 인증을 완료해주세요.');
        if (!password) return showError('새 비밀번호를 입력해주세요.');
        if (password.length < 8) return showError('비밀번호는 8자 이상 입력해주세요.');
        if (password !== passwordConfirm) return showError('비밀번호가 일치하지 않습니다.');

        resetBtn.disabled = true;
        resetBtn.textContent = '처리 중...';

        try {
            const res = await fetch(`/api/user/reset-password?email=${encodeURIComponent(email)}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password, passwordConfirm })
            });
            const data = await res.json();
            if (data.success) {
                alert('비밀번호가 변경되었습니다! 로그인해주세요.');
                window.location.href = '/login';
            } else {
                showError(data.message || '비밀번호 재설정에 실패했습니다.');
                resetBtn.disabled = false;
                resetBtn.textContent = '비밀번호 재설정';
            }
        } catch (e) {
            showError('오류가 발생했습니다. 다시 시도해주세요.');
            resetBtn.disabled = false;
            resetBtn.textContent = '비밀번호 재설정';
        }
    });

    /* 유틸 */
    function setMsg(el, msg, type) {
        el.textContent = msg;
        el.className = `field-msg ${type}`;
    }

    function showError(msg) {
        resetErrorMsg.textContent = msg;
        resetError.style.display = 'block';
    }

    function hideError() {
        resetError.style.display = 'none';
        resetErrorMsg.textContent = '';
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

});