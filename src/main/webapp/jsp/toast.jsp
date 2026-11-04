<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<link rel="stylesheet" href="<c:url value='/css/toast.css'/>">
<div class="toast-container">
    <div id="liveToast" class="toast" data-delay="3000">
        <div class="toast-header">
            <strong class="modal-title"></strong> <!-- "Title" of the toast -->
            <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
        <div class="toast-body">
            <!-- message dynamically inserted -->
            <div class="toast-progress"></div>
        </div>
    </div>
</div>
<script>
    function showToast(message, type) {
        const toastEl = document.getElementById('liveToast');
        const toastBody = toastEl.querySelector('.toast-body');
        const progress = toastEl.querySelector('.toast-progress');
        const toastHeader = toastEl.querySelector('.toast-header');
        toastBody.firstChild.nodeValue = message + '\n';
        if (progress) progress.remove();
        const newProgress = document.createElement('div');
        newProgress.classList.add('toast-progress');
        toastBody.appendChild(newProgress);
        if(type === 1) {
            toastHeader.firstChild.nodeValue = "Success!" + '\n';
            toastHeader.style.backgroundColor = 'green';
            toastHeader.style.color = 'white';
        } else if(type === 2) {
            toastHeader.firstChild.nodeValue = "Error!" + '\n';
            toastHeader.style.backgroundColor = 'red';
        } else {
            toastHeader.firstChild.nodeValue = "Attention!" + '\n';
            toastHeader.style.backgroundColor = 'yellow';
            toastHeader.style.color = 'black';
        }
        const toast = bootstrap.Toast.getOrCreateInstance(toastEl);
        toast.show();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-message]').forEach(function (el) {
            el.addEventListener('click', function () {
                const message = el.getAttribute('data-message');
                const type = el.getAttribute('data-type');
                showToast(message, type);
            });
        });
    });
</script>