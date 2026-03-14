'use strict';

window.addEventListener('DOMContentLoaded', function () {

    // 상태 변수 (상황에 따라 변할수 있는 변수)
    let currentRoomId = null;
    let currentModel  = 'alan-4.0';
    let isStreaming   = false;
    let activeEs      = null;
    let activeWriter  = null;
    let activeText    = '';
    let activeBubble  = null;

    // 상수(값이 고정된 변수)
    const aside          = document.getElementById('aside');
    const btnToggle      = document.getElementById('btnToggle');
    const btnNew         = document.getElementById('btnNew');
    const roomList       = document.getElementById('roomList');
    const empty          = document.getElementById('empty');
    const chatView       = document.getElementById('chatView');
    const messages       = document.getElementById('messages');
    const messagesWrap   = document.getElementById('messagesWrap');
    const userInput      = document.getElementById('userInput');
    const sendBtn        = document.getElementById('sendBtn');
    const emptyInput     = document.getElementById('emptyInput');
    const emptySendBtn   = document.getElementById('emptySendBtn');
    const btnSettings    = document.getElementById('btnSettings');
    const settingsMenu   = document.getElementById('settingsMenu');
    const toast          = document.getElementById('toast');
    const chatModelBtns  = document.querySelectorAll('#chatModelSelector > button');
    const emptyModelBtns = document.querySelectorAll('#emptyModelSelector > button');

    // 모델 선택
    function setModel(model) {
        currentModel = model;
        Array.prototype.forEach.call(
            document.querySelectorAll('#chatModelSelector > button, #emptyModelSelector > button'),
            function (btn) {
                btn.setAttribute('aria-pressed', btn.dataset.model === model ? 'true' : 'false');
            }
        );
    }

    Array.prototype.forEach.call(chatModelBtns, function (btn) {
        btn.addEventListener('click', function () { setModel(btn.dataset.model); });
    });

    Array.prototype.forEach.call(emptyModelBtns, function (btn) {
        btn.addEventListener('click', function () { setModel(btn.dataset.model); });
    });

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
    // theme: common.js와 동일한 방식으로 다크모드 토글
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

    // 빈 화면 / 채팅 뷰 전환
    function setView(view) {
        if (view === 'empty') {
            empty.classList.remove('hidden');
            chatView.classList.add('hidden');
            emptyInput.focus();
        } else {
            empty.classList.add('hidden');
            chatView.classList.remove('hidden');
        }
    }

    // 채팅방 목록 로드 GET /api/chat/rooms
    function loadRooms() {
        fetch('/api/chat/rooms')
            .then(function (r) { return r.json(); })
            .then(function (rooms) {
                roomList.innerHTML = '';

                if (!rooms || rooms.length === 0) {
                    const li = document.createElement('li');
                    li.className   = 'listEmpty';
                    li.textContent = '채팅방이 없습니다';
                    roomList.appendChild(li);
                    return;
                }

                rooms.forEach(function (room) {
                    roomList.appendChild(buildRoomItem(room));
                });
            })
            .catch(function () { showToast('채팅방 목록을 불러오지 못했습니다.', true); });
    }

    // 채팅방 아이템 DOM 생성
    function buildRoomItem(room) {
        const li   = document.createElement('li');
        const btn  = document.createElement('button');
        const edit = document.createElement('button');
        const del  = document.createElement('button');

        li.dataset.roomId = room.chatroomId;

        btn.textContent = room.chatTitle || '새 채팅';
        btn.className   = 'roomTitle';

        edit.className = 'btnEdit';
        edit.setAttribute('aria-label', '채팅방 제목 수정');

        del.className = 'btnDel';
        del.setAttribute('aria-label', '채팅방 삭제');

        btn.addEventListener('click', function () { openRoom(room.chatroomId); });

        edit.addEventListener('click', function (e) {
            e.stopPropagation();
            const current  = btn.textContent;
            const newTitle = prompt('새 채팅방 제목을 입력하세요', current);
            if (!newTitle || newTitle.trim() === current) return;
            updateRoomTitle(room.chatroomId, newTitle.trim(), btn);
        });

        del.addEventListener('click', function (e) {
            e.stopPropagation();
            const title = btn.textContent || '이 대화';
            if (!confirm('"' + title + '" 대화를 삭제하시겠습니까?')) return;
            deleteRoom(room.chatroomId, li);
        });

        li.appendChild(btn);
        li.appendChild(edit);
        li.appendChild(del);
        return li;
    }

    // 채팅방 열기 GET /api/chat/rooms/{chatroomId}/messages
    function openRoom(roomId) {
        currentRoomId = roomId;

        document.querySelectorAll('#roomList > li').forEach(function (li) {
            li.classList.toggle('active', li.dataset.roomId == roomId);
        });

        messages.innerHTML = '';
        setView('chat');

        fetch('/api/chat/rooms/' + roomId + '/messages')
            .then(function (r) { return r.json(); })
            .then(function (msgs) {
                (msgs || []).forEach(function (msg) {
                    appendMsg(msg.role, parseContent(msg.content));
                });
                scrollToBottom();
            })
            .catch(function () { showToast('메시지를 불러오지 못했습니다.', true); });
    }

    // DB 저장된 raw content에서 실제 텍스트 추출
    function parseContent(raw) {
        if (!raw) return '';

        const completeMatch = raw.match(/'type':\s*'complete'.*?'content':\s*'([^']*)'/);
        if (completeMatch) return completeMatch[1];

        let result = '';
        const regex = /'type':\s*'continue'.*?'content':\s*'([^']*)'/g;
        let match;
        while ((match = regex.exec(raw)) !== null) {
            result += match[1];
        }
        if (result) return result;

        return raw;
    }

    // 새 채팅 버튼
    btnNew.addEventListener('click', function () {
        currentRoomId = null;
        messages.innerHTML = '';
        document.querySelectorAll('#roomList > li').forEach(function (li) {
            li.classList.remove('active');
        });
        setView('empty');
    });

    // 채팅방 생성 POST /api/chat/rooms
    function createRoom(callback) {
        fetch('/api/chat/rooms', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ chatTitle: '새 채팅', model: currentModel })
        })
            .then(function (r) {
                if (!r.ok) return r.text().then(function (msg) { throw new Error(msg); });
                return r.json();
            })
            .then(function (room) { callback(room.chatroomId); })
            .catch(function (err) { showToast(err.message || '채팅방 생성 실패', true); });
    }

    // 채팅방 제목 수정 PATCH /api/chat/rooms/{chatroomId}/title
    function updateRoomTitle(roomId, title, btnEl) {
        fetch('/api/chat/rooms/' + roomId + '/title?chatTitle=' + encodeURIComponent(title), {
            method: 'PATCH',
            headers: { 'X-CSRF-TOKEN': getCsrfToken() }
        })
            .then(function (r) {
                if (!r.ok) throw new Error('제목 수정 실패');
                if (btnEl) btnEl.textContent = title;
            })
            .catch(function () { showToast('제목 수정 중 오류가 발생했습니다.', true); });
    }

    // 첫 메시지로 채팅방 제목 자동 설정 (30자 제한)
    function setAutoTitle(roomId, text) {
        const title = text.length > 30 ? text.substring(0, 30) : text;
        updateRoomTitle(roomId, title, null);
        setTimeout(loadRooms, 400);
    }

    // 채팅방 삭제 DELETE /api/chat/rooms/{chatroomId}
    function deleteRoom(roomId, li) {
        li.remove();
        if (currentRoomId == roomId) {
            currentRoomId = null;
            messages.innerHTML = '';
            setView('empty');
        }
        if (roomList.children.length === 0) {
            const emptyLi = document.createElement('li');
            emptyLi.className   = 'listEmpty';
            emptyLi.textContent = '채팅방이 없습니다';
            roomList.appendChild(emptyLi);
        }

        fetch('/api/chat/rooms/' + roomId, {
            method: 'DELETE',
            headers: { 'X-CSRF-TOKEN': getCsrfToken() }
        })
            .catch(function () {
                showToast('삭제 중 오류가 발생했습니다.', true);
                loadRooms();
            });
    }

    // 전송 버튼 / 멈춤 버튼 전환
    function setStopMode(active) {
        if (active) {
            sendBtn.classList.add('stop');
            sendBtn.setAttribute('aria-label', '생성 중지');
        } else {
            sendBtn.classList.remove('stop');
            sendBtn.setAttribute('aria-label', '전송');
        }
    }

    // 스트리밍 강제 중단
    function stopStreaming() {
        if (!isStreaming) return;
        if (activeEs)     { activeEs.close();   activeEs     = null; }
        if (activeWriter) { activeWriter.stop(); activeWriter = null; }
        if (activeBubble) {
            activeBubble.classList.remove('streaming');
            activeBubble.classList.remove('waiting');
        }

        isStreaming           = false;
        emptySendBtn.disabled = false;
        setStopMode(false);
        document.removeEventListener('keydown', onEscKey);
        loadRooms();

        if (activeText) {
            userInput.value = activeText;
            userInput.style.height = 'auto';
            userInput.style.height = userInput.scrollHeight + 'px';
            userInput.focus();
            activeText = '';
        }
    }

    // ESC 키로 스트리밍 중단
    function onEscKey(e) {
        if (e.key === 'Escape' && isStreaming) stopStreaming();
    }

    // 타이핑 큐 - 한 글자씩 출력 (20ms 간격)
    function typewriterQueue(bubble) {
        let queue    = [];
        let timer    = null;
        let done     = false;
        let onDoneCb = null;

        function flush() {
            if (queue.length === 0) {
                timer = null;
                if (done && onDoneCb) onDoneCb();
                return;
            }
            bubble.textContent += queue.shift();
            scrollToBottom();
            timer = setTimeout(flush, 20);
        }

        return {
            push: function (text) {
                for (let i = 0; i < text.length; i++) {
                    queue.push(text[i]);
                }
                if (!timer) flush();
            },
            finish: function (callback) {
                done     = true;
                onDoneCb = callback;
                if (!timer && queue.length === 0 && callback) callback();
            },
            stop: function () {
                queue = [];
                clearTimeout(timer);
                timer = null;
            }
        };
    }

    // SSE 스트리밍 메시지 전송 GET /api/chat/rooms/{chatroomId}/stream?content=...
    function sendMessage(text, roomId) {
        if (!text.trim() || isStreaming) return;

        isStreaming           = true;
        emptySendBtn.disabled = true;
        activeText            = text;
        setStopMode(true);

        appendMsg('user', text);
        scrollToBottom();

        const aiBubble = appendMsg('assistant', '');
        aiBubble.classList.add('streaming');
        aiBubble.classList.add('waiting');

        const url    = '/api/chat/rooms/' + roomId + '/stream?content=' + encodeURIComponent(text);
        const es     = new EventSource(url);
        const writer = typewriterQueue(aiBubble);

        activeEs     = es;
        activeWriter = writer;
        activeBubble = aiBubble;

        document.addEventListener('keydown', onEscKey);

        es.addEventListener('message', function (e) {
            try {
                const parsed = JSON.parse(e.data.replace(/'/g, '"'));
                if (parsed.type === 'continue' && parsed.data && parsed.data.content) {
                    aiBubble.classList.remove('waiting');
                    writer.push(parsed.data.content);
                }
            } catch (err) {
                aiBubble.classList.remove('waiting');
                writer.push(e.data);
            }
        });

        es.addEventListener('error', function () {
            if (activeEs !== es) return;
            es.close();
            activeEs     = null;
            activeWriter = null;
            activeBubble = null;
            activeText   = '';
            document.removeEventListener('keydown', onEscKey);

            writer.finish(function () {
                aiBubble.classList.remove('streaming');
                aiBubble.classList.remove('waiting');

                if (!aiBubble.textContent) {
                    aiBubble.textContent = '응답을 받지 못했습니다.';
                    aiBubble.classList.add('error');
                }

                isStreaming           = false;
                emptySendBtn.disabled = false;
                setStopMode(false);
                loadRooms();
            });
        });

        return es;
    }

    // 빈 화면에서 전송 (채팅방 자동 생성)
    function sendFromEmpty() {
        const text = emptyInput.value.trim();
        if (!text || isStreaming) return;
        emptyInput.value = '';

        createRoom(function (roomId) {
            loadRooms();
            openRoom(roomId);
            sendMessage(text, roomId);
            setAutoTitle(roomId, text);
        });
    }

    // 채팅 뷰에서 전송
    function sendFromChat() {
        const text = userInput.value.trim();
        if (!text || !currentRoomId || isStreaming) return;
        userInput.value = '';
        userInput.style.height = 'auto';
        sendMessage(text, currentRoomId);
    }

    sendBtn.addEventListener('click', function () {
        if (isStreaming) stopStreaming();
        else sendFromChat();
    });

    emptySendBtn.addEventListener('click', sendFromEmpty);

    userInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendFromChat(); }
    });

    emptyInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendFromEmpty(); }
    });

    // 메시지 버블 DOM 추가
    function appendMsg(role, content) {
        const row    = document.createElement('div');
        const fig    = document.createElement('figure');
        const bubble = document.createElement('div');

        row.className      = 'msg ' + role;
        fig.textContent    = role === 'user' ? 'U' : 'AI';
        fig.setAttribute('aria-hidden', 'true');
        bubble.textContent = content;

        row.appendChild(fig);
        row.appendChild(bubble);
        messages.appendChild(row);
        return bubble;
    }

    function scrollToBottom() {
        messagesWrap.scrollTop = messagesWrap.scrollHeight;
    }

    // textarea 자동 높이
    [userInput, emptyInput].forEach(function (el) {
        el.addEventListener('input', function () {
            el.style.height = 'auto';
            el.style.height = el.scrollHeight + 'px';
        });
    });

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
    loadRooms();
    setView('empty');
});