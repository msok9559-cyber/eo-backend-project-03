window.addEventListener("DOMContentLoaded", function (){
    //통계 숫자 카운팅 애니메이션
    function animateNumber(el, target, duration) {
        if (!el) return;
        const startTime = performance.now();

        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration , 1);
            const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
            el.textContent = Math.floor(eased * target).toLocaleString();
            if (progress < 1 ) {
                requestAnimationFrame(update);
            } else {
                el.textContent = target.toLocaleString();
            }
        }

        requestAnimationFrame(update);
    }

    // 통계 api 호출
    const statUsers = document.getElementById('stat-users');
    const statMessages = document.getElementById('stat-messages');
    const statPayments = document.getElementById('stat-payments');

    fetch('/api/stats')
        .then(function (res){
            if (!res.ok) throw new Error('통계 API 오류');
            return res.json();
        })
        .then(function (data) {
            animateNumber(statUsers,    data.totalUsers,    1500);
            animateNumber(statMessages, data.totalMessages, 1500);
            animateNumber(statPayments, data.totalPayments, 1500);
        })
        .catch(function (err) {
            console.error('통계 로드 실패:', err);
            if (statUsers)    statUsers.textContent    = '-';
            if (statMessages) statMessages.textContent = '-';
            if (statPayments) statPayments.textContent = '-';
        });
})