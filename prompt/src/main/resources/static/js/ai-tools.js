'use strict';

window.addEventListener('DOMContentLoaded', function () {

    // 이력 저장 키 및 최대 개수
    const HISTORY_KEY  = 'aiToolsHistory';
    const HISTORY_MAX  = 30;

    // 탭별 설정
    const typeConfig = {
        summary:   { tabId: 'summary',   inputId: 'summaryInput',   resultId: 'summaryResult',   textId: 'summaryResultText',   label: '요약' },
        translate: { tabId: 'translate', inputId: 'translateInput', resultId: 'translateResult', textId: 'translateResultText', label: '번역' },
        youtube:   { tabId: 'youtube',   inputId: 'youtubeInput',   resultId: 'youtubeResult',   textId: 'youtubeResultText',   label: '유튜브' },
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
            showToast('도움말 페이지 준비 중입니다.');
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
        ['youtubeInput',   'youtubeCount'],
        ['questionInput',  'questionCount']
    ].forEach(function (pair) {
        const textarea = document.getElementById(pair[0]);
        const countEl  = document.getElementById(pair[1]);
        textarea.addEventListener('input', function () {
            countEl.textContent = textarea.value.length.toLocaleString() + '자';
        });
    });

    // 초기화 버튼
    document.querySelectorAll('.clearBtn').forEach(function (btn) {
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

    // 페이지 요약 POST /api/alan/page/summary
    document.getElementById('summaryBtn').addEventListener('click', function () {
        const content = document.getElementById('summaryInput').value.trim();
        if (!content) { showToast('요약할 내용을 입력해주세요.', true); return; }

        showLoading('페이지를 요약하고 있습니다...');

        fetch('/api/alan/page/summary', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
            body: JSON.stringify({ content: content })
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
                saveHistory('summary', content, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });

    // 페이지 번역 POST /api/alan/page/translate
    document.getElementById('translateBtn').addEventListener('click', function () {
        const raw = document.getElementById('translateInput').value.trim();
        if (!raw) { showToast('번역할 텍스트를 입력해주세요.', true); return; }

        const contents = raw.split('\n').filter(function (l) { return l.trim(); });

        showLoading('번역하고 있습니다...');

        fetch('/api/alan/page/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
            body: JSON.stringify({ contents: contents })
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
                saveHistory('translate', raw, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });

    // 유튜브 자막 요약 POST /api/alan/youtube/summary
    document.getElementById('youtubeBtn').addEventListener('click', function () {
        const raw = document.getElementById('youtubeInput').value.trim();
        if (!raw) { showToast('유튜브 자막을 입력해주세요.', true); return; }

        // AlanAiDto.YoutubeSubtitleRequest 형식에 맞게 변환
        // Chapter 구조: {chapterIdx, chapterTitle, text:[{timestamp, content}]}
        const lines = raw.split('\n')
            .map(function (line) { return line.trim(); })
            .filter(function (line) { return line.length > 0; });

        const subtitleTexts = lines.map(function (line, idx) {
            // "0:00 내용" 형식이면 타임스탬프 분리, 아니면 순번으로 대체
            const match = line.match(/^(\d+:\d+)\s+(.+)/);
            return match
                ? { timestamp: match[1], content: match[2] }
                : { timestamp: '0:' + String(idx).padStart(2, '0'), content: line };
        });

        // @JsonProperty("chapter_idx"), @JsonProperty("chapter_title") 때문에 snake_case로 전송
        const subtitle = [{ chapter_idx: 0, chapter_title: '전체 자막', text: subtitleTexts }];

        showLoading('유튜브 자막을 요약하고 있습니다...');

        fetch('/api/alan/youtube/summary', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
            body: JSON.stringify({ subtitle: subtitle })
        })
            .then(function (r) {
                if (!r.ok) return parseErrorMessage(r).then(function (msg) { throw new Error(msg); });
                return r.json();
            })
            .then(function (res) {
                // res.data 안의 summary 구조를 가독성 있게 표시
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
                saveHistory('youtube', raw, text);
            })
            .catch(function (err) { showToast(err.message || '오류가 발생했습니다.', true); })
            .finally(hideLoading);
    });


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

        // 입력값 복원
        document.getElementById(cfg.inputId).value = item.input;

        // 글자 수 카운터 갱신
        const countMap = {
            summary:   'summaryCount',
            translate: 'translateCount',
            youtube:   'youtubeCount'
        };
        const countEl = document.getElementById(countMap[item.type]);
        if (countEl) {
            countEl.textContent = item.input.length.toLocaleString() + '자';
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