window.addEventListener('DOMContentLoaded', function () {

    const overlay   = document.getElementById('mypage-overlay');
    const modal     = document.getElementById('mypage-modal');
    const closeBtn  = document.getElementById('mypage-close');
    const menuBtns  = document.querySelectorAll('.mypage-menu');
    const tabs      = document.querySelectorAll('.mypage-tab');

    // 팝업 열기
    function openModal() {
        overlay.classList.add('active');
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    // 팝업 닫기
    function closeModal() {
        overlay.classList.remove('active');
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    // 마이페이지 버튼 클릭
    const mypageBtn = document.getElementById('mypage-btn');
    if (mypageBtn) {
        mypageBtn.addEventListener('click', function (e) {
            e.preventDefault();
            openModal();
        });
    }

    // 닫기 버튼
    if (closeBtn) {
        closeBtn.addEventListener('click', closeModal);
    }

    // 오버레이 클릭 시 닫기
    if (overlay) {
        overlay.addEventListener('click', closeModal);
    }

    // ESC 키 닫기
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeModal();
    });

    /* 탭 전환 */
    menuBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const tab = this.dataset.tab;

            menuBtns.forEach(b => b.classList.remove('active'));
            tabs.forEach(t => t.classList.remove('active'));

            this.classList.add('active');
            document.getElementById(`tab-${tab}`).classList.add('active');

            // 탭 전환 시 메시지 초기화
            clearPasswordForm();
        });
    });

    /* 비밀번호 변경 */
    const currentPasswordInput   = document.getElementById('current-password');
    const newPasswordInput       = document.getElementById('new-password');
    const newPasswordConfirm     = document.getElementById('new-password-confirm');
    const passwordMatchMsg       = document.getElementById('password-match-msg');
    const passwordError          = document.getElementById('password-error');
    const passwordErrorMsg       = document.getElementById('password-error-msg');
    const passwordSuccess        = document.getElementById('password-success');
    const passwordSuccessMsg     = document.getElementById('password-success-msg');
    const updatePasswordBtn      = document.getElementById('update-password-btn');

    // 새 비밀번호 확인 실시간
    if (newPasswordConfirm) {
        newPasswordConfirm.addEventListener('input', function () {
            if (!newPasswordConfirm.value) {
                passwordMatchMsg.textContent = '';
                passwordMatchMsg.className = 'field-msg';
                return;
            }
            if (newPasswordInput.value === newPasswordConfirm.value) {
                setMsg(passwordMatchMsg, '비밀번호가 일치합니다.', 'success');
            } else {
                setMsg(passwordMatchMsg, '비밀번호가 일치하지 않습니다.', 'error');
            }
        });
    }

    // 비밀번호 변경 버튼
    if (updatePasswordBtn) {
        updatePasswordBtn.addEventListener('click', async function () {
            hidePasswordMsg();

            const currentPassword = currentPasswordInput.value.trim();
            const newPassword     = newPasswordInput.value.trim();
            const confirmPassword = newPasswordConfirm.value.trim();

            if (!currentPassword) return showPasswordError('현재 비밀번호를 입력해주세요.');
            if (!newPassword) return showPasswordError('새 비밀번호를 입력해주세요.');
            if (newPassword.length < 8) return showPasswordError('비밀번호는 8자 이상 입력해주세요.');
            if (newPassword !== confirmPassword) return showPasswordError('새 비밀번호가 일치하지 않습니다.');
            if (currentPassword === newPassword) return showPasswordError('현재 비밀번호와 다른 비밀번호를 입력해주세요.');

            updatePasswordBtn.disabled = true;
            updatePasswordBtn.textContent = '변경 중...';

            try {
                const csrfToken = document.getElementById('csrf-token')?.value;

                const res = await fetch('/mypage/password', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        ...(csrfToken && { 'X-CSRF-TOKEN': csrfToken })
                    },
                    credentials: 'include',
                    body: JSON.stringify({
                        currentPassword,
                        password: newPassword,
                        passwordConfirm: confirmPassword
                    })
                });
                const data = await res.json();
                if (data.success) {
                    alert('비밀번호가 변경되었습니다.\n보안을 위해 다시 로그인해주세요.');
                    window.location.href = '/login';
                } else {
                    showPasswordError(data.message || '비밀번호 변경에 실패했습니다.');
                }
            } catch (e) {
                showPasswordError('오류가 발생했습니다. 다시 시도해주세요.');
            } finally {
                updatePasswordBtn.disabled = false;
                updatePasswordBtn.textContent = '비밀번호 변경';
            }
        });
    }

    /*  유틸 */
    function setMsg(el, msg, type) {
        el.textContent = msg;
        el.className = `field-msg ${type}`;
    }

    function showPasswordError(msg) {
        passwordErrorMsg.textContent = msg;
        passwordError.style.display = 'block';
        passwordSuccess.style.display = 'none';
    }

    function showPasswordSuccess(msg) {
        passwordSuccessMsg.textContent = msg;
        passwordSuccess.style.display = 'block';
        passwordError.style.display = 'none';
    }

    function hidePasswordMsg() {
        passwordError.style.display = 'none';
        passwordSuccess.style.display = 'none';
    }

    function clearPasswordForm() {
        if (currentPasswordInput) currentPasswordInput.value = '';
        if (newPasswordInput) newPasswordInput.value = '';
        if (newPasswordConfirm) newPasswordConfirm.value = '';
        if (passwordMatchMsg) {
            passwordMatchMsg.textContent = '';
            passwordMatchMsg.className = 'field-msg';
        }
        hidePasswordMsg();
    }

});