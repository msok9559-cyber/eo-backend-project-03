window.addEventListener('DOMContentLoaded', function() {
    // 로그인 폼 유효성 검사
    const form = document.querySelector("#login > div > form");
    const userid = document.getElementById("userid");
    const password = document.getElementById("password");

    if (form){
        form.addEventListener("submit", function (e){
            const useridVal = userid.value.trim();
            const passwordVal = password.value.trim();

            if (!useridVal){
                e.preventDefault();
                userid.focus();
                return;
            }
            if (!passwordVal){
                e.preventDefault();
                password.focus();
                return;
            }
        })
    }
})