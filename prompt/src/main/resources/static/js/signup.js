window.addEventListener('DOMContentLoaded', function() {
    // 상태 변수
    let isUseridChecked = false;
    let isEmailVerified = false;

    // 요소
    const useridInput = document.getElementById('userid');
    const checkIdBtn = document.getElementById('check-id-btn');
    const useridMsg = document.getElementById('userid-msg');

    const passwordInput = document.getElementById('password');
    const passwordConfirm = document.getElementById('password-confirm');
    const passwordMsg = document.getElementById('password-msg');

    const emailInput        = document.getElementById('email');
    const sendEmailBtn      = document.getElementById('send-email-btn');
    const emailMsg          = document.getElementById('email-msg');

    const verifyGroup       = document.getElementById('verify-group');
    const verifyCodeInput   = document.getElementById('verify-code');
    const verifyBtn         = document.getElementById('verify-btn');
    const verifyMsg         = document.getElementById('verify-msg');

    const agreeAll          = document.getElementById('agree-all');
    const agreeItems        = document.querySelectorAll('.agree-item');
    const agreeService      = document.getElementById('agree-service');
    const agreePrivacy      = document.getElementById('agree-privacy');

    const signupBtn         = document.getElementById('signup-btn');
    const signupError       = document.getElementById('signup-error');
    const signupErrorMsg    = document.getElementById('signup-error-msg');

    // 아이디 중복 확인
    // 아이디 입력시 중복확인 초기화

    useridInput.addEventListener('input', function () {
        isUseridChecked = false;
        useridMsg.textContent = '';
        useridMsg.className = 'field-msg';
    });

    checkIdBtn.addEventListener('click', async function () {
        const userid = useridInput.value.trim();
        if (!userid) {
            setMsg(useridMsg, '아이디를 입력해주세요.', 'error');
            return;
        }

        checkIdBtn.disabled = true;
        try {
            const res = await fetch(`/api/users/check-id?userid=${encodeURIComponent(userid)}`);
            const data = await res.json();
            if (data.available) {
                setMsg(useridMsg, '사용 가능한 아이디입니다.', 'success');
                isUseridChecked = true;
            } else {
                setMsg(useridMsg, '이미 사용 중인 아이디입니다.', 'error');
                isUseridChecked = false;
            }
        } catch (e) {
            setMsg(useridMsg, '확인 중 오류가 발생했습니다.', 'error');
        } finally {
            checkIdBtn.disabled = false;
        }
    });

    // 비밀번호 확인
    passwordConfirm.addEventListener('input', function () {
        if (!passwordConfirm.value) {
            passwordMsg.textContent = '';
            passwordMsg.className = 'field-msg';
            return;
        }
        if (passwordInput.value === passwordConfirm.value) {
            setMsg(passwordMsg, '비밀번호가 일치합니다.', 'success');
        } else {
            setMsg(passwordMsg, '비밀번호가 일치하지 않습니다.', 'error');
        }
    });

    // 이메일 인증번호 발송
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
                // 60초 후 재발송 가능
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

    // 인증번호 확인
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

    // 전체 동의 체크 박스
    agreeAll.addEventListener('change', function () {
        agreeItems.forEach(item => item.checked = agreeAll.checked);
    });

    agreeItems.forEach(item => {
        item.addEventListener('change', function () {
            agreeAll.checked = Array.from(agreeItems).every(i => i.checked);
        });
    });

    // 회원 가입 버튼
    signupBtn.addEventListener('click', async function () {
        hideError();

        const userid          = useridInput.value.trim();
        const password        = passwordInput.value.trim();
        const passwordConfirmVal = passwordConfirm.value.trim();
        const username        = document.getElementById('username').value.trim();
        const email           = emailInput.value.trim();

        // 유효성 검사
        if (!userid) return showError('아이디를 입력해주세요.');
        if (!isUseridChecked) return showError('아이디 중복확인을 해주세요.');
        if (!password) return showError('비밀번호를 입력해주세요.');
        if (password.length < 8) return showError('비밀번호는 8자 이상 입력해주세요.');
        if (password !== passwordConfirmVal) return showError('비밀번호가 일치하지 않습니다.');
        if (!username) return showError('이름을 입력해주세요.');
        if (!email) return showError('이메일을 입력해주세요.');
        if (!isEmailVerified) return showError('이메일 인증을 완료해주세요.');
        if (!agreeService.checked) return showError('서비스 이용약관에 동의해주세요.');
        if (!agreePrivacy.checked) return showError('개인정보 처리방침에 동의해주세요.');

        signupBtn.disabled = true;
        signupBtn.textContent = '처리 중...';

        try {
            const res = await fetch('/api/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userid, password, passwordConfirm: passwordConfirmVal, username, email })
            });
            const data = await res.json();
            if (data.success) {
                alert('회원가입이 완료되었습니다! 로그인해주세요.');
                window.location.href = '/login';
            } else {
                showError(data.message || '회원가입에 실패했습니다.');
                signupBtn.disabled = false;
                signupBtn.textContent = '회원가입';
            }
        } catch (e) {
            showError('오류가 발생했습니다. 다시 시도해주세요.');
            signupBtn.disabled = false;
            signupBtn.textContent = '회원가입';
        }
    });

    function setMsg(el, msg, type) {
        el.textContent = msg;
        el.className = `field-msg ${type}`;
    }

    function showError(msg) {
        signupErrorMsg.textContent = msg;
        signupError.style.display = 'block';
    }

    function hideError() {
        signupError.style.display = 'none';
        signupErrorMsg.textContent = '';
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }
})