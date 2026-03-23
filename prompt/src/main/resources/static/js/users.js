document.addEventListener("DOMContentLoaded", function () {
    const overlay = document.getElementById("user-detail-overlay");
    const closeBtn = document.getElementById("overlay-close-btn");
    const cancelBtn = document.getElementById("detail-cancel-btn");
    const saveBtn = document.getElementById("detail-save-btn");

    const targetUserIdInput = document.getElementById("detail-target-user-id");
    const planSelect = document.getElementById("detail-plan-select");

    const detailButtons = document.querySelectorAll(".detail-btn");
    const rowActionButtons = document.querySelectorAll(".action-buttons .btn-action:not(.detail-btn)");
    const actionButtons = document.querySelectorAll("#detail-action-buttons .btn-action");

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    let originalDetailData = null;
    let currentDetailData = null;
    let pendingAction = null;

    if (!overlay || !closeBtn || !cancelBtn || !saveBtn || !targetUserIdInput || !planSelect) {
        console.error("회원 상세 모달에 필요한 요소를 찾을 수 없습니다.");
        return;
    }

    function openOverlay() {
        overlay.classList.add("show");
        document.documentElement.classList.add("modal-open");
        document.body.classList.add("modal-open");
    }

    function closeOverlay() {
        overlay.classList.remove("show");
        document.documentElement.classList.remove("modal-open");
        document.body.classList.remove("modal-open");
        resetModalState();
    }

    function resetModalState() {
        pendingAction = null;
        originalDetailData = null;
        currentDetailData = null;
        targetUserIdInput.value = "";
        planSelect.value = "NORMAL";

        actionButtons.forEach(function (button) {
            button.classList.remove("selected-action");
            button.style.display = "";
        });
    }

    function fillText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value ?? "-";
        }
    }

    function clearActionButtonSelection() {
        actionButtons.forEach(function (button) {
            button.classList.remove("selected-action");
        });
    }

    function markSelectedActionButton(action) {
        clearActionButtonSelection();

        actionButtons.forEach(function (button) {
            if (button.dataset.action === action) {
                button.classList.add("selected-action");
            }
        });
    }

    function renderDetailStatusTexts(data) {
        fillText("detail-user-active", data.active ? "활성" : "탈퇴");
        fillText("detail-user-locked", data.locked ? "잠금" : "정상");
        fillText("detail-user-deleted", data.active ? "정상" : "탈퇴");
    }

    function updateActionButtonsByState(data) {
        const lockBtn = document.getElementById("detail-lock-btn");
        const withdrawBtn = document.getElementById("detail-withdraw-btn");

        if (lockBtn) {
            if (data.locked) {
                lockBtn.dataset.action = "UNLOCK";
                lockBtn.textContent = "잠금해제";
                lockBtn.className = "btn-action unlock-btn";
            } else {
                lockBtn.dataset.action = "LOCK";
                lockBtn.textContent = "잠금";
                lockBtn.className = "btn-action lock-btn";
            }
        }

        if (withdrawBtn) {
            if (data.active) {
                withdrawBtn.dataset.action = "WITHDRAW";
                withdrawBtn.textContent = "회원 탈퇴";
                withdrawBtn.className = "btn-action withdraw-btn";
            } else {
                withdrawBtn.dataset.action = "RESTORE";
                withdrawBtn.textContent = "복구";
                withdrawBtn.className = "btn-action restore-btn";
            }
        }

        if (pendingAction) {
            markSelectedActionButton(pendingAction);
        } else {
            clearActionButtonSelection();
        }
    }

    function fillUserDetail(data) {
        fillText("detail-user-id", data.id);
        fillText("detail-user-userid", data.userid);
        fillText("detail-user-email", data.email);
        fillText("detail-user-name", data.username ?? data.name);
        fillText("detail-user-createdAt", data.createdAt);
        fillText("detail-user-last-login", data.lastLoginAt);

        planSelect.value = data.planName ?? "NORMAL";

        fillText("detail-user-plan-start", data.planStartedAt);
        fillText("detail-user-plan-end", data.planExpiredAt);

        renderDetailStatusTexts(data);
        fillText("detail-user-status-updated", data.statusUpdatedAt);

        fillText("detail-user-chatrooms", data.chatRoomCount ?? 0);
        fillText("detail-user-messages", data.messageCount ?? 0);
        fillText("detail-user-images", data.imageCount ?? 0);
        fillText("detail-user-files", data.fileCount ?? 0);
        fillText("detail-user-last-activity", data.lastActivityAt);

        targetUserIdInput.value = data.id ?? "";

        updateActionButtonsByState(data);
    }

    async function fetchUserDetail(userId) {
        try {
            const response = await fetch(`/admin/api/users/${userId}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                throw new Error("회원 상세 조회 실패");
            }

            const result = await response.json();
            const data = result.data;

            if (!data) {
                throw new Error("회원 상세 데이터가 없습니다.");
            }

            originalDetailData = { ...data };
            currentDetailData = { ...data };
            pendingAction = null;

            fillUserDetail(currentDetailData);
            openOverlay();
        } catch (error) {
            alert(error.message || "회원 정보를 불러오지 못했습니다.");
        }
    }

    function getStatusConfirmMessage(action) {
        switch (action) {
            case "LOCK":
                return "이 회원을 잠금 처리하시겠습니까?";
            case "UNLOCK":
                return "이 회원의 잠금을 해제하시겠습니까?";
            case "WITHDRAW":
                return "이 회원을 탈퇴 처리하시겠습니까?";
            case "RESTORE":
                return "이 회원을 복구하시겠습니까?";
            default:
                return "상태를 변경하시겠습니까?";
        }
    }

    function renderRowStatus(row, data) {
        const statusCell = row.querySelector("td:nth-child(4) .status-badges");
        if (!statusCell) return;

        let html = "";

        if (data.active) {
            html += `<span class="status active">활성</span>`;
        } else {
            html += `<span class="status deleted">탈퇴</span>`;
        }

        if (data.locked) {
            html += `<span class="status locked">잠금</span>`;
        }

        statusCell.innerHTML = html;
    }

    function renderRowButtons(row, data) {
        const buttons = row.querySelectorAll(".action-buttons .btn-action");
        let lockBtn = null;
        let withdrawBtn = null;

        buttons.forEach(function (btn) {
            if (btn.classList.contains("detail-btn")) return;

            const action = btn.dataset.action;
            if (action === "LOCK" || action === "UNLOCK") {
                lockBtn = btn;
            } else if (action === "WITHDRAW" || action === "RESTORE") {
                withdrawBtn = btn;
            }
        });

        if (!lockBtn || !withdrawBtn) {
            const allButtons = row.querySelectorAll(".action-buttons .btn-action");
            lockBtn = allButtons[1];
            withdrawBtn = allButtons[2];
        }

        if (lockBtn) {
            lockBtn.dataset.userId = data.id;

            if (data.locked) {
                lockBtn.dataset.action = "UNLOCK";
                lockBtn.textContent = "잠금해제";
                lockBtn.className = "btn-action unlock-btn";
            } else {
                lockBtn.dataset.action = "LOCK";
                lockBtn.textContent = "잠금";
                lockBtn.className = "btn-action lock-btn";
            }
        }

        if (withdrawBtn) {
            withdrawBtn.dataset.userId = data.id;

            if (data.active) {
                withdrawBtn.dataset.action = "WITHDRAW";
                withdrawBtn.textContent = "탈퇴";
                withdrawBtn.className = "btn-action withdraw-btn";
            } else {
                withdrawBtn.dataset.action = "RESTORE";
                withdrawBtn.textContent = "복구";
                withdrawBtn.className = "btn-action restore-btn";
            }
        }
    }

    function updateRowView(row, data) {
        renderRowStatus(row, data);
        renderRowButtons(row, data);

        const planCell = row.children[2];
        if (planCell) {
            planCell.textContent = data.planName ?? "-";
        }
    }

    function findUserRow(userId) {
        return document.querySelector(`.detail-btn[data-user-id="${userId}"]`)?.closest("tr");
    }

    detailButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const userId = this.dataset.userId;

            if (!userId) {
                alert("회원 번호를 찾을 수 없습니다.");
                return;
            }

            fetchUserDetail(userId);
        });
    });

    rowActionButtons.forEach(function (button) {
        button.addEventListener("click", async function () {
            const userId = this.dataset.userId;
            const action = this.dataset.action;

            if (!userId || !action) {
                alert("회원 상태 변경 정보가 올바르지 않습니다.");
                return;
            }

            const confirmed = confirm(getStatusConfirmMessage(action));
            if (!confirmed) return;

            try {
                const response = await fetch(`/admin/api/users/${userId}/status`, {
                    method: "PATCH",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({
                        action: action
                    })
                });

                if (!response.ok) {
                    throw new Error("상태 변경 실패");
                }

                const row = findUserRow(userId);

                if (row) {
                    const updatedData = {
                        id: userId,
                        active: action === "WITHDRAW" ? false : action === "RESTORE" ? true : !row.querySelector(".status.deleted"),
                        locked: action === "LOCK" ? true : action === "UNLOCK" ? false : !!row.querySelector(".status.locked"),
                        planName: row.children[2]?.textContent?.trim() || "-"
                    };

                    if (action === "WITHDRAW") {
                        updatedData.locked = false;
                    }

                    updateRowView(row, updatedData);
                }

                alert("회원 상태가 변경되었습니다.");
            } catch (error) {
                alert(error.message || "상태 변경 중 오류가 발생했습니다.");
            }
        });
    });

    overlay.addEventListener("click", function (e) {
        if (e.target === overlay) {
            closeOverlay();
        }
    });

    closeBtn.addEventListener("click", function () {
        closeOverlay();
    });

    cancelBtn.addEventListener("click", function () {
        closeOverlay();
    });

    actionButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const action = this.dataset.action;
            let confirmMessage = "";

            switch (action) {
                case "LOCK":
                    confirmMessage = "이 회원을 잠금 처리하시겠습니까?";
                    break;
                case "UNLOCK":
                    confirmMessage = "이 회원의 잠금을 해제하시겠습니까?";
                    break;
                case "RESTORE":
                    confirmMessage = "이 회원을 복구하시겠습니까?";
                    break;
                case "WITHDRAW":
                    confirmMessage = "이 회원을 탈퇴 처리하시겠습니까?";
                    break;
                default:
                    confirmMessage = "상태를 변경하시겠습니까?";
            }

            const confirmed = confirm(confirmMessage);

            if (!confirmed || !currentDetailData) {
                return;
            }

            if (action === "LOCK") {
                currentDetailData.locked = true;
                pendingAction = "LOCK";
            } else if (action === "UNLOCK") {
                currentDetailData.locked = false;
                pendingAction = "UNLOCK";
            } else if (action === "WITHDRAW") {
                currentDetailData.active = false;
                pendingAction = "WITHDRAW";
            } else if (action === "RESTORE") {
                currentDetailData.active = true;
                pendingAction = "RESTORE";
            }

            renderDetailStatusTexts(currentDetailData);
            updateActionButtonsByState(currentDetailData);
            markSelectedActionButton(pendingAction);

            alert("저장 버튼을 눌러야 최종 반영됩니다.");
        });
    });

    saveBtn.addEventListener("click", async function () {
        const userId = targetUserIdInput.value;

        if (!userId) {
            alert("선택된 회원이 없습니다.");
            return;
        }

        const selectedPlan = planSelect.value;
        const originalPlan = originalDetailData ? originalDetailData.planName : null;

        const changedPlan = selectedPlan !== originalPlan;
        const changedAction = pendingAction !== null;

        if (!changedPlan && !changedAction) {
            alert("변경된 내용이 없습니다.");
            return;
        }

        if (pendingAction === "WITHDRAW") {
            const withdrawConfirmed = confirm(
                "정말 회원 탈퇴를 최종 처리하시겠습니까?\n이 작업은 일반 상태 변경보다 더 주의가 필요합니다."
            );

            if (!withdrawConfirmed) {
                return;
            }
        }

        try {
            if (changedPlan) {
                const planResponse = await fetch(`/admin/api/users/${userId}/plan`, {
                    method: "PATCH",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({
                        planName: selectedPlan
                    })
                });

                if (!planResponse.ok) {
                    throw new Error("플랜 변경 실패");
                }
            }

            if (changedAction) {
                const actionResponse = await fetch(`/admin/api/users/${userId}/status`, {
                    method: "PATCH",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({
                        action: pendingAction
                    })
                });

                if (!actionResponse.ok) {
                    throw new Error("상태 변경 실패");
                }
            }

            alert("회원 정보가 저장되었습니다.");
            await refreshDetailAfterSave(userId);
            closeOverlay();
        } catch (error) {
            alert(error.message || "저장 중 오류가 발생했습니다.");
        }
    });

    async function refreshDetailAfterSave(userId) {
        try {
            const response = await fetch(`/admin/api/users/${userId}`, {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                throw new Error("저장 후 회원 상세 재조회 실패");
            }

            const result = await response.json();
            const data = result.data;

            if (!data) {
                throw new Error("저장 후 회원 데이터가 없습니다.");
            }

            originalDetailData = { ...data };
            currentDetailData = { ...data };
            pendingAction = null;

            fillUserDetail(currentDetailData);

            const row = findUserRow(userId);
            if (row) {
                updateRowView(row, data);
            }
        } catch (error) {
            alert(error.message || "저장 후 화면 갱신에 실패했습니다.");
        }
    }
});