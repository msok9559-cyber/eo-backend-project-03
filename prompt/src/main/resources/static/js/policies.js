document.addEventListener('DOMContentLoaded', () => {
    const editButtons = document.querySelectorAll('.btn-edit');

    const planModal = document.getElementById('plan-modal');
    const modalCloseBtn = document.getElementById('modal-close-btn');
    const modalCancelBtn = document.getElementById('modal-cancel-btn');
    const modalSaveBtn = document.getElementById('modal-save-btn');
    const modalBackdrop = planModal.querySelector('.modal-backdrop');

    const modalPlanId = document.getElementById('modal-plan-id');
    const modalPlanName = document.getElementById('modal-plan-name');
    const modalDailyChatLimit = document.getElementById('modal-daily-chat-limit');
    const modalImageUploadLimit = document.getElementById('modal-image-upload-limit');
    const modalFileUploadLimit = document.getElementById('modal-file-upload-limit');
    const modalFileSizeLimit = document.getElementById('modal-file-size-limit');
    const modalTokenLimit = document.getElementById('modal-token-limit');
    const modalPrice = document.getElementById('modal-price');

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function openModal() {
        planModal.classList.remove('hidden');
        document.documentElement.classList.add('modal-open');
        document.body.classList.add('modal-open');
    }

    function closeModal() {
        planModal.classList.add('hidden');
        document.documentElement.classList.remove('modal-open');
        document.body.classList.remove('modal-open');
    }

    function fillModal(button) {
        modalPlanId.value = button.dataset.planId;
        modalPlanName.value = button.dataset.planName;
        modalDailyChatLimit.value = button.dataset.dailyChatLimit;
        modalImageUploadLimit.value = button.dataset.imageUploadLimit;
        modalFileUploadLimit.value = button.dataset.fileUploadLimit;
        modalFileSizeLimit.value = button.dataset.fileSizeLimit;
        modalTokenLimit.value = button.dataset.tokenLimit;
        modalPrice.value = button.dataset.price;
    }

    function getRequestBody() {
        return {
            dailyChatLimit: Number(modalDailyChatLimit.value),
            imageUploadLimit: Number(modalImageUploadLimit.value),
            fileUploadLimit: Number(modalFileUploadLimit.value),
            fileSizeLimit: Number(modalFileSizeLimit.value),
            tokenLimit: Number(modalTokenLimit.value),
            price: Number(modalPrice.value)
        };
    }

    editButtons.forEach((button) => {
        button.addEventListener('click', () => {
            fillModal(button);
            openModal();
        });
    });

    modalCloseBtn.addEventListener('click', closeModal);
    modalCancelBtn.addEventListener('click', closeModal);
    modalBackdrop.addEventListener('click', closeModal);

    modalSaveBtn.addEventListener('click', async () => {
        const planId = modalPlanId.value;
        const requestBody = getRequestBody();

        try {
            const response = await fetch(`/admin/api/plans/${planId}`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error('플랜 저장에 실패했습니다.');
            }

            alert('플랜 정보가 수정되었습니다.');
            closeModal();
            location.reload();
        } catch (error) {
            console.error(error);
            alert('저장 중 오류가 발생했습니다.');
        }
    });
});