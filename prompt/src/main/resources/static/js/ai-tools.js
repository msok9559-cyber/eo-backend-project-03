'use strict';

window.addEventListener('DOMContentLoaded', function () {

    // 이력 저장 키 및 최대 개수
    const HISTORY_KEY  = 'aiToolsHistory';
    const HISTORY_MAX  = 30;

    // 탭별 설정
    const typeConfig = {
        summary:   { tabId: 'summary',   inputId: 'summaryInput',   resultId: 'summaryResult',   textId: 'summaryResultText',   label: '요약' },
        translate: { tabId: 'translate', inputId: 'translateInput', resultId: 'translateResult', textId: 'translateResultText', label: '번역' },
        youtube:   { tabId: 'youtube',   inputId: 'youtubeUrlInput', resultId: 'youtubeResult',   textId: 'youtubeResultText',   label: '유튜브' },
        question:  { tabId: 'question',  inputId: 'questionInput',  resultId: 'questionResult',  textId: 'questionResultText',  label: '질문' }
    };

    const aside          = document.getElementById('aside');
    const btnToggle      = document.getElementById('btnToggle');
    const btnSettings    = document.getElementById('btnSettings');
    const settingsMenu   = document.getElementById('settingsMenu');
    const historyList    = document.getElementById('historyList');
    const toast          = document.getElementById('toast');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const loadingMsg     = document.getElementById('loadingMsg');

    // 사이드바 토글
    btnToggle.addEventListener('click', function () {
        const collapsed = aside.classList.toggle('collapsed');
        btnToggle.setAttribute('aria-expanded', String(!collapsed));
    });

    // 설정 메뉴 열기/닫기
    btnSettings.addEventListener('click', function (e) {
        e.stopPropagation();
        const isOpen = !settingsMenu.classList.contains('hidden');
        settingsMenu.classList.toggle('hidden');
        btnSettings.setAttribute('aria-expanded', String(!isOpen));
    });

    document.addEventListener('click', function () {
        if (!settingsMenu.classList.contains('hidden')) {
            settingsMenu.classList.add('hidden');
            btnSettings.setAttribute('aria-expanded', 'false');
        }
    });

    // 설정 메뉴 액션
    // profile: mypage-open-btn 클래스로 mypage.js가 모달 처리
    // theme:   common.js와 동일한 방식으로 다크모드 토글
    settingsMenu.addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;

        const action = btn.dataset.action;

        if (action !== 'profile') {
            settingsMenu.classList.add('hidden');
            btnSettings.setAttribute('aria-expanded', 'false');
        }

        if (action === 'theme') {
            const html   = document.documentElement;
            const isDark = html.getAttribute('data-theme') === 'dark';
            if (isDark) {
                html.removeAttribute('data-theme');
                localStorage.setItem('theme', 'light');
            } else {
                html.setAttribute('data-theme', 'dark');
                localStorage.setItem('theme', 'dark');
            }
        } else if (action === 'plan') {
            location.href = '/payment';
        } else if (action === 'help') {
            location.href = '/guide';
        } else if (action === 'logout') {
            fetch('/logout', {
                method: 'POST',
                headers: { 'X-CSRF-TOKEN': getCsrfToken() }
            }).finally(function () { location.href = '/login'; });
        }
    });

    // 탭 전환
    document.getElementById('tabBar').addEventListener('click', function (e) {
        const btn = e.target.closest('.tabBtn');
        if (!btn) return;

        document.querySelectorAll('.tabBtn').forEach(function (b) {
            b.classList.remove('active');
            b.setAttribute('aria-selected', 'false');
        });
        document.querySelectorAll('.panel').forEach(function (p) {
            p.classList.add('hidden');
        });

        btn.classList.add('active');
        btn.setAttribute('aria-selected', 'true');

        const tab = btn.dataset.tab;
        document.getElementById('panel' + tab.charAt(0).toUpperCase() + tab.slice(1))
            .classList.remove('hidden');
    });

    // 글자 수 카운터
    [
        ['summaryInput',   'summaryCount'],
        ['translateInput', 'translateCount'],
        ['questionInput',  'questionCount']
    ].forEach(function (pair) {
        const textarea = document.getElementById(pair[0]);
        const countEl  = document.getElementById(pair[1]);
        textarea.addEventListener('input', function () {
            countEl.textContent = textarea.value.length.toLocaleString() + '자';
        });
    });

    // 초기화 버튼 - data-clear 속성이 있는 버튼(질문 탭)은 공통 처리
    document.querySelectorAll('.clearBtn[data-clear]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            btn.dataset.clear.split(' ').forEach(function (id) {
                const el = document.getElementById(id);
                if (!el) return;
                if (el.tagName === 'TEXTAREA') { el.value = ''; }
                else if (el.classList.contains('count')) { el.textContent = '0자'; }
                else { el.classList.add('hidden'); }
            });
        });
    });

    // 초기화 버튼 - 요약 (URL + 텍스트 + 결과 함께)
    document.getElementById('summaryClearBtn').addEventListener('click', function () {
        document.getElementById('summaryUrlInput').value = '';
        document.getElementById('summaryInput').value    = '';
        document.getElementById('summaryCount').textContent = '0자';
        document.getElementById('summaryResult').classList.add('hidden');
    });

    // 초기화 버튼 - 번역
    document.getElementById('translateClearBtn').addEventListener('click', function () {
        document.getElementById('translateUrlInput').value = '';
        document.getElementById('translateInput').value    = '';
        document.getElementById('translateCount').textContent = '0자';
        document.getElementById('translateResult').classList.add('hidden');
    });

    // 초기화 버튼 - 유튜브
    document.getElementById('youtubeClearBtn').addEventListener('click', function () {
        document.getElementById('youtubeUrlInput').value   = '';
        document.getElementById('youtubeTextInput').value  = '';
        document.getElementById('youtubeCount').textContent = '0자';
        document.getElementById('youtubeResult').classList.add('hidden');
    });


    // 복사 버튼
    document.querySelectorAll('.copyBtn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const target = document.getElementById(btn.dataset.target);
            if (!target || !target.textContent.trim()) return;
            navigator.clipboard.writeText(target.textContent.trim())
                .then(function () {
                    btn.textContent = '복사됨';
                    btn.classList.add('copied');
                    setTimeout(function () {
                        btn.textContent = '복사';
                        btn.classList.remove('copied');
                    }, 2000);
                })
                .catch(function () { showToast('복사에 실패했습니다.', true); });
        });
    });

    // 페이지 요약
    // URL 입력 시 → POST /api/alan/page/summary/url
    // 텍스트 입력 시 → POST /api/alan/page/summary
    document.getElementById('summaryBtn').addEventListener('click', function () {
        const url     = document.getElementById('summaryUrlInput').value.trim();
        const content = document.getElementById('summaryInput').value.trim();
        if (!url && !content) { showToast('URL 또는 텍스트를 입력해주세요.', true); return; }

        const isUrl    = !!url;
        const endpoint = isUrl ? '/api/alan/page/summary/url' : '/api/alan/page/summary';
        const body     = isUrl ? { url: url } : { content: content };

        showLoading('페이지를 요약하고 있습니다...');

        fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
            body: JSON.stringify(body)
        })
            .then(function (r) {
                if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                return r.json();
            })
            .then(function (res) {
                // res.data.summary 는 JSON 문자열이므로 파싱해서 가독성 있게 표시
                let inner;
                try { inner = JSON.parse(res.data.summary); } catch (e) { inner = null; }

                let text = '';
                if (inner) {
                    if (inner.summary)        { text += inner.summary + '\n\n'; }
                    if (inner.main_sentences && inner.main_sentences.length) {
                        text += '[ 핵심 문장 ]\n';
                        inner.main_sentences.forEach(function (s, i) {
                            text += (i + 1) + '. ' + s + '\n';
                        });
                        text += '\n';
                    }
                    if (inner.questions && inner.questions.length) {
                        text += '[ 관련 질문 ]\n';
                        inner.questions.forEach(function (q, i) {
                            text += (i + 1) + '. ' + q + '\n';
                        });
                    }
                } else {
                    text = (res.data && res.data.summary) ? res.data.summary : '요약 결과가 없습니다.';
                }

                text = text.trim();
                showResult('summaryResult', 'summaryResultText', text);
                saveHistory('summary', isUrl ? url : content, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });


    // 페이지 번역
    // URL 입력 시 → POST /api/alan/page/translate/url
    // 텍스트 입력 시 → POST /api/alan/page/translate
    document.getElementById('translateBtn').addEventListener('click', function () {
        const url = document.getElementById('translateUrlInput').value.trim();
        const raw = document.getElementById('translateInput').value.trim();
        if (!url && !raw) { showToast('URL 또는 텍스트를 입력해주세요.', true); return; }

        const isUrl    = !!url;
        const endpoint = isUrl ? '/api/alan/page/translate/url' : '/api/alan/page/translate';
        const body     = isUrl
            ? { url: url }
            : { contents: raw.split('\n').filter(function (l) { return l.trim(); }) };

        showLoading('번역하고 있습니다...');

        fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
            body: JSON.stringify(body)
        })
            .then(function (r) {
                if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                return r.json();
            })
            .then(function (res) {
                // res.data.translated 는 JSON 문자열이므로 파싱해서 번역 목록 추출
                let inner;
                try { inner = JSON.parse(res.data.translated); } catch (e) { inner = null; }

                let text = '';
                if (inner && inner.translated_contents && inner.translated_contents.length) {
                    text = inner.translated_contents.join('\n\n');
                } else {
                    text = (res.data && res.data.translated) ? res.data.translated : '번역 결과가 없습니다.';
                }

                text = text.trim();
                showResult('translateResult', 'translateResultText', text);
                saveHistory('translate', isUrl ? url : raw, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });

    // 유튜브 텍스트 글자 수 카운트
    document.getElementById('youtubeTextInput').addEventListener('input', function () {
        document.getElementById('youtubeCount').textContent = this.value.length + '자';
    });

    // 유튜브 요약 버튼 - URL 입력 시 자막 자동 추출, 텍스트 입력 시 직접 전송
    document.getElementById('youtubeBtn').addEventListener('click', function () {
        const url  = document.getElementById('youtubeUrlInput').value.trim();
        const text = document.getElementById('youtubeTextInput').value.trim();

        if (!url && !text) { showToast('YouTube URL 또는 자막 텍스트를 입력해주세요.', true); return; }

        // URL 입력된 경우 → /api/alan/youtube/url (자막 자동 추출)
        if (url) {
            if (!url.includes('youtube.com') && !url.includes('youtu.be')) {
                showToast('올바른 YouTube URL을 입력해주세요.', true);
                return;
            }

            showLoading('YouTube 자막을 가져오고 있습니다...');

            fetch('/api/alan/youtube/url', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
                body: JSON.stringify({ url: url })
            })
                .then(function (r) {
                    if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                    return r.json();
                })
                .then(function (res) { handleYoutubeResult(res, url); })
                .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
                .finally(hideLoading);

            // 텍스트 입력된 경우 → /api/alan/youtube/summary (자막 텍스트 직접 전송)
        } else {
            showLoading('AI가 자막을 요약하고 있습니다...');

            // 텍스트를 줄 단위로 나눠 SubtitleText 배열로 변환
            const lines = text.split('\n')
                .map(function (line) { return line.trim(); })
                .filter(function (line) { return line.length > 0; });

            const subtitleBody = {
                subtitle: [{
                    chapter_idx: 0,
                    chapter_title: '동영상 내용',
                    text: lines.map(function (line, i) {
                        return { timestamp: '0:' + String(i).padStart(2, '0'), content: line };
                    })
                }]
            };

            fetch('/api/alan/youtube/summary', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
                body: JSON.stringify(subtitleBody)
            })
                .then(function (r) {
                    if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                    return r.json();
                })
                .then(function (res) { handleYoutubeResult(res, text.substring(0, 50) + '...'); })
                .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
                .finally(hideLoading);
        }
    });

    // 유튜브 결과 처리 공통 함수
    function handleYoutubeResult(res, inputValue) {
        const data = res.data;
        let text = '';

        if (data && data.summary) {
            const s = data.summary;
            if (s.totalSummary && s.totalSummary.length) {
                text += '[ 전체 요약 ]\n';
                s.totalSummary.forEach(function (line) { text += line + '\n'; });
                text += '\n';
            }
            if (s.chapters && s.chapters.length) {
                s.chapters.forEach(function (ch) {
                    text += '[ ' + (ch.title || ('챕터 ' + (ch.index + 1))) + ' ]\n';
                    if (ch.summary && ch.summary.length) {
                        ch.summary.forEach(function (line) { text += '- ' + line + '\n'; });
                    }
                    text += '\n';
                });
            }
        }

        if (!text.trim()) {
            text = JSON.stringify(res.data, null, 2);
        }

        text = text.trim();
        showResult('youtubeResult', 'youtubeResultText', text);
        saveHistory('youtube', inputValue, text);
    }





    // AI 질문 GET /api/alan/question
    document.getElementById('questionBtn').addEventListener('click', function () {
        const content = document.getElementById('questionInput').value.trim();
        if (!content) { showToast('질문 내용을 입력해주세요.', true); return; }

        showLoading('AI가 답변을 생성하고 있습니다...');

        fetch('/api/alan/question?content=' + encodeURIComponent(content), {
            method: 'GET',
            headers: { 'X-CSRF-TOKEN': getCsrfToken() }
        })
            .then(function (r) {
                if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                return r.json();
            })
            .then(function (res) {
                const text = (res.data && res.data.answer) ? res.data.answer : '답변 결과가 없습니다.';
                showResult('questionResult', 'questionResultText', text);
                saveHistory('question', content, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });

    // 이력 저장 (localStorage)
    function saveHistory(type, input, result) {
        let history = loadAllHistory();
        let title   = input.replace(/\s+/g, ' ').trim().substring(0, 28);
        if (title.length === 28) title += '...';

        history.unshift({
            id:        Date.now(),
            type:      type,
            title:     title,
            input:     input,
            result:    result,
            createdAt: new Date().toISOString()
        });

        // 최대 개수 초과 시 오래된 것 제거
        if (history.length > HISTORY_MAX) {
            history = history.slice(0, HISTORY_MAX);
        }

        try {
            localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
        } catch (e) {
            // localStorage 용량 초과 시 절반 삭제 후 재시도
            history = history.slice(0, Math.floor(HISTORY_MAX / 2));
            localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
        }

        renderHistory();
    }

    function loadAllHistory() {
        try {
            return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
        } catch (e) {
            return [];
        }
    }

    function deleteHistory(id) {
        const history = loadAllHistory().filter(function (item) {
            return item.id !== id;
        });
        localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
        renderHistory();
    }

    // 이력 사이드바 렌더링
    function renderHistory() {
        const history = loadAllHistory();
        historyList.innerHTML = '';

        if (history.length === 0) {
            const li = document.createElement('li');
            li.className   = 'listEmpty';
            li.textContent = '저장된 이력이 없습니다';
            historyList.appendChild(li);
            return;
        }

        history.forEach(function (item) {
            historyList.appendChild(buildHistoryItem(item));
        });
    }

    function buildHistoryItem(item) {
        const li    = document.createElement('li');
        const btn   = document.createElement('button');
        const badge = document.createElement('span');
        const del   = document.createElement('button');

        li.dataset.historyId = item.id;

        badge.className   = 'historyBadge';
        badge.textContent = typeConfig[item.type] ? typeConfig[item.type].label : item.type;

        btn.className = 'itemTitle';
        btn.appendChild(badge);
        btn.appendChild(document.createTextNode(item.title));

        del.className = 'btnDel';
        del.setAttribute('aria-label', '이력 삭제');

        // 클릭 시 해당 탭으로 전환 후 결과 복원
        btn.addEventListener('click', function () {
            restoreHistory(item);
            document.querySelectorAll('#historyList > li').forEach(function (el) {
                el.classList.remove('active');
            });
            li.classList.add('active');
        });

        del.addEventListener('click', function (e) {
            e.stopPropagation();
            deleteHistory(item.id);
        });

        li.appendChild(btn);
        li.appendChild(del);
        return li;
    }

    // 이력 복원
    function restoreHistory(item) {
        const cfg = typeConfig[item.type];
        if (!cfg) return;

        // 해당 탭으로 전환
        document.querySelectorAll('.tabBtn').forEach(function (b) {
            const isTarget = b.dataset.tab === cfg.tabId;
            b.classList.toggle('active', isTarget);
            b.setAttribute('aria-selected', String(isTarget));
        });
        document.querySelectorAll('.panel').forEach(function (p) {
            p.classList.add('hidden');
        });
        document.getElementById('panel' + cfg.tabId.charAt(0).toUpperCase() + cfg.tabId.slice(1))
            .classList.remove('hidden');

        // 입력값 복원 - URL 이력이면 urlInput에, 텍스트 이력이면 textarea에
        const isUrl = item.input.startsWith('http://') || item.input.startsWith('https://');

        if (item.type === 'summary') {
            document.getElementById('summaryUrlInput').value = isUrl ? item.input : '';
            document.getElementById('summaryInput').value    = isUrl ? '' : item.input;
            document.getElementById('summaryCount').textContent = isUrl ? '0자' : item.input.length.toLocaleString() + '자';
        } else if (item.type === 'translate') {
            document.getElementById('translateUrlInput').value = isUrl ? item.input : '';
            document.getElementById('translateInput').value    = isUrl ? '' : item.input;
            document.getElementById('translateCount').textContent = isUrl ? '0자' : item.input.length.toLocaleString() + '자';
        } else if (item.type === 'youtube') {
            const isYoutubeUrl = item.input.includes('youtube.com') || item.input.includes('youtu.be');
            document.getElementById('youtubeUrlInput').value   = isYoutubeUrl ? item.input : '';
            document.getElementById('youtubeTextInput').value  = isYoutubeUrl ? '' : item.input;
            document.getElementById('youtubeCount').textContent = isYoutubeUrl ? '0자' : item.input.length.toLocaleString() + '자';
        } else {
            document.getElementById(cfg.inputId).value = item.input;
            const countEl = document.getElementById('questionCount');
            if (countEl && item.type === 'question') { countEl.textContent = item.input.length.toLocaleString() + '자'; }
        }

        // 결과 복원
        showResult(cfg.resultId, cfg.textId, item.result);
    }

    // 결과 표시
    function showResult(resultId, textId, content) {
        const resultCard = document.getElementById(resultId);
        document.getElementById(textId).textContent = content;
        resultCard.classList.remove('hidden');
        resultCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // 로딩 오버레이
    function showLoading(msg) {
        loadingMsg.textContent = msg || 'AI가 분석 중입니다...';
        loadingOverlay.classList.remove('hidden');
    }

    function hideLoading() {
        loadingOverlay.classList.add('hidden');
    }

    // 서버 에러 응답에서 메시지 추출
    // GlobalExceptionHandler가 {"success": false, "message": "..."} 형태로 반환
    function parseErrorMessage(r) {
        return r.json()
            .then(function (body) {
                if (body && body.message) return body.message;
                return 'HTTP ' + r.status + ' 오류가 발생했습니다.';
            })
            .catch(function () {
                return 'HTTP ' + r.status + ' 오류가 발생했습니다.';
            });
    }

    // 토스트 알림
    let toastTimer = null;

    function showToast(msg, isError) {
        toast.textContent = msg;
        toast.className   = 'show' + (isError ? ' error' : '');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { toast.className = ''; }, 2800);
    }

    // CSRF 토큰
    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    // 초기화
    renderHistory();
});