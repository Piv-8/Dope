// On load handle buttons
const createBtn      = document.getElementById('createBtn');
const startBtn       = document.getElementById('startBtn');
const killBtn        = document.getElementById('killBtn');
const runBtn                      = document.getElementById('runBtn');
const openCommitBtn  = document.getElementById('openCommitBtn');
const statusLabel    = document.getElementById('status');

// Form Validation
(function() {
    'use strict';

    // Wait until the DOM is fully loaded
    document.addEventListener('DOMContentLoaded', function() {
        var form = document.getElementById('commitForm');
        form.classList.add('was-validated');
        var commitModalEl = document.getElementById('commitModal');
        var bootstrapModal = bootstrap.Modal.getOrCreateInstance(commitModalEl);

        form.addEventListener('submit', function(event) {
            event.preventDefault();
            event.stopPropagation();

            // Validate repo selection
            var repoSelected = !!document.querySelector('[name="selectedRepoId"]').value;
            var repoFeedback = document.getElementById('repoFeedback');
            if (!repoSelected) {
                repoFeedback.style.display = 'block';
            } else {
                repoFeedback.style.display = 'none';
            }

            // Check form validity
            if (form.checkValidity() && repoSelected) {
                // Mark as validated (optional, to satisfy Visual consistency)
                form.classList.remove('was-validated');

                // Call existing commit behavior
                commitAction();

                // Hide Bootstrap modal when done
                bootstrapModal.hide();

                // Reset form for next use (optional)
                form.reset();
            } else {
                // Add Bootstrap validation styling
                form.classList.add('was-validated');
            }
        }, false);
    });
})();


function demuxStdout(data) {
    let offset = 0;
    let result = '';
    const decoder = new TextDecoder();

    while (offset + 8 <= data.length) {
        // Read header
        const streamType = data[offset];               // 0x01 or 0x02
        const length = (data[offset+4] << 24)
            | (data[offset+5] << 16)
            | (data[offset+6] <<  8)
            |  data[offset+7];
        // Extract payload
        const start = offset + 8;
        const end = start + length;
        if (end > data.length) break;  // malformed
        result += decoder.decode(data.subarray(start, end));
        offset = end;
    }

    return result;
}


let loadingModalStartTime = null;

function showLoadingModal() {
    const div = document.getElementById("loadingModal");
    var backdrop = document.getElementById("loadingBackdrop");

    if (div.classList.contains("d-none")) {
        div.classList.remove("d-none");
        div.classList.add("d-flex");

        backdrop.classList.remove("d-none");
        backdrop.classList.add("d-block");

        loadingModalStartTime = Date.now();
    }
}

function hideLoadingModal(callbackAfterHide) {
    const div = document.getElementById("loadingModal");
    var backdrop = document.getElementById("loadingBackdrop")

    if (!div.classList.contains("d-flex")) {
        if (typeof callbackAfterHide === "function") callbackAfterHide();
        return;
    }

    const elapsed = Date.now() - loadingModalStartTime;
    const remaining = Math.max(2000 - elapsed, 0); // minimo 2 secondi

    setTimeout(() => {
        if(div.classList.contains("d-flex")) {
            div.classList.remove("d-flex");
            div.classList.add("d-none");

            backdrop.classList.remove("d-block");
            backdrop.classList.add("d-none");
        }
        if (typeof callbackAfterHide === "function") callbackAfterHide();
    }, remaining);
}

/**
 * Very simple ANSI → HTML converter.
 * Only handles a few codes;
 */
function ansiToHtml(text) {
    // Map SGR parameters to open/close tags
    const SGR = {
        '0': '</span>',                     // reset
        '1': '<span style="font-weight:bold">',
        '30': '<span style="color:black">',
        '31': '<span style="color:red">',
        '32': '<span style="color:green">',
        '33': '<span style="color:yellow">',
        '34': '<span style="color:blue">',
        '35': '<span style="color:magenta">',
        '36': '<span style="color:cyan">',
        '37': '<span style="color:white">'
    };

    return text
        // Replace CSI sequences like "\x1b[31m"
        .replace(/\x1b\[(\d+)m/g, (_, code) => SGR[code] || '')
        // Escape any remaining "<" for safety
        .replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/**
 * Given a raw Uint8Array, render it into a container.
 */
function handleTerminalOutput(rawData, container) {
    const arr = new Uint8Array(rawData.length);
    for(let i = 0; i < rawData.length; i++){
        arr[i] = rawData.charCodeAt(i);
    }
    const plain = demuxStdout(arr);
    const withAnsi = ansiToHtml(plain);
    // Convert newlines to <br> and preserve spacing
    container.innerHTML = withAnsi.replace(/\n/g, '<br>');
}

function setContainerCreated(created) {
    if (created) {
        // Switch create → secondary
        createBtn.classList.remove('btn-success');
        createBtn.classList.add   ('btn-secondary');

        // Enable start, kill, commit
        [ startBtn, killBtn, openCommitBtn].forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('btn-secondary');
            btn.classList.add   ('btn-primary');
        });

        document.getElementById('isContainerCreated').value = '1';

    } else {
        // Back to initial state
        createBtn.classList.remove('btn-secondary');
        createBtn.classList.add   ('btn-success');

        [ startBtn, killBtn, openCommitBtn, runBtn ].forEach(btn => {
            btn.disabled = true;
            btn.classList.remove('btn-primary');
            btn.classList.add   ('btn-secondary');
        });

        statusLabel.textContent = 'Not Yet Created';
        document.getElementById('isContainerCreated').value = '0';
    }
}

function handleErrorResponse(xhr) {
    let msg = 'Unknown error';
    const payload = JSON.parse(xhr.response).message;
    if(payload['error-code'] !== null) {
        msg = `${payload['error-code']}: ${payload['error-details']}`;
    }else{
        msg = `Error ${xhr.status} ${xhr.responseText}`;
    }
    showToast(msg,2);
}

function createContainer() {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', container_url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    const container = {
        instance_id: 1,
        status: 'created',
        image_run: image_run,
        user_instance: user_instance
    };
    const body = JSON.stringify({ container });

    xhr.onload = function () {
        if (xhr.status === 201) {
            setContainerCreated(true);
            document.getElementById('debug').innerHTML = xhr.responseText;
            setInterval(updateStatus, 1500);
            setInterval(updateContainerLog, 1500);
            showToast("Container created, you can now start/stop commit",1);
        } else {
            setContainerCreated(false);
            handleErrorResponse(xhr);
        }
    };

    xhr.onerror = function () {
        showToast('Network error while creating container.',2);
        setContainerCreated(false);
    };

    xhr.send(body);
}

function startAction() {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${container_url}/start`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            console.log('Container successfully started');
            runBtn.disabled = false;
            runBtn.classList.replace('btn-secondary', 'btn-primary');
            document.getElementById('debug').innerHTML = xhr.responseText;
            showToast("container started, you can now run commands while container is running",1);
        } else {
            handleErrorResponse(xhr);
        }
        updateStatus();
    };

    xhr.onerror = function () {
        showToast('Network error while starting container.',2);
    };

    xhr.send();
}

function killAction() {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${container_url}/kill`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            document.getElementById('debug').innerHTML = xhr.responseText;
            runBtn.disabled = true;
            runBtn.classList.replace('btn-primary', 'btn-secondary');
        } else {
            handleErrorResponse(xhr);
        }
        updateStatus();
    };

    xhr.onerror = function () {
        showToast('Network error while killing container.',2);
    };

    xhr.send();
}

function commitAction() {
    showLoadingModal();
    const tag = document.getElementById('commitTag').value;
    const repo = document.getElementById('selectedRepoId').value;
    const description = document.getElementById('imageDescription').value;

    if (!tag || !repo) {
        showToast('Please specify both commit tag and repository.');
        return;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${container_url}/commit`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    const image = { image: { id_image: 1, tag, repo: parseInt(repo, 10), description, status: 'ready' } };


    xhr.send(JSON.stringify(image));

    xhr.onload = function () {
        if (xhr.status === 201) {
            document.getElementById('debug').innerHTML = xhr.responseText;
            hideLoadingModal();
            showToast('Image committed successfully.',1);
        } else {
            handleErrorResponse(xhr);
            hideLoadingModal();
        }
    };


    xhr.onerror = function () {
        showToast('Network error while committing image.',2);
    };

}

function executeCommand() {
    const cmdInput = document.getElementById('commandInput').value;
    if (!cmdInput) {
        showToast('Please enter a command to execute.');
        return;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${container_url}/exec?cmd=${encodeURIComponent(cmdInput)}`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            document.getElementById('debug').innerHTML = xhr.responseText;
            const payload = JSON.parse(xhr.responseText);
            handleTerminalOutput(payload.message.message, document.getElementById('commandLOG'));
        } else {
            handleErrorResponse(xhr);
        }
        updateContainerLog();
    };

    xhr.onerror = function () {
        showToast('Network error while executing command.',2);
    };

    xhr.send();
}

function updateContainerLog() {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${container_url}/log`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            const payload = JSON.parse(xhr.responseText);
            handleTerminalOutput(payload.message.message, document.getElementById('containerLOG'));
        } else {
            handleErrorResponse(xhr);
        }
    };

    xhr.onerror = function () {
        showToast('Network error while retrieving logs.',2);
    };

    xhr.send();
}

function updateStatus() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', container_url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            const payload = JSON.parse(xhr.responseText);
            document.getElementById('status').innerHTML = payload.container.status;
            if(payload.container.status !== 'running') {
                runBtn.disabled = true;
                runBtn.classList.replace('btn-primary', 'btn-secondary');
            }else{
                runBtn.disabled = false;
                runBtn.classList.replace('btn-secondary', 'btn-primary');
            }
            setContainerCreated(true);
        } else {
            setContainerCreated(false);
            handleErrorResponse(xhr);
        }
    };

    xhr.onerror = function () {
        showToast('Network error while retrieving status.',2);
        setContainerCreated(false);
    };

    xhr.send();
}

//Draggable buttons
document.addEventListener('DOMContentLoaded', () => {
    const bubble = document.querySelector('.containerButtons');
    let startX = 0, startY = 0, offsetX = 0, offsetY = 0;

    bubble.addEventListener('pointerdown', evt => {
        // Don’t initiate drag on the buttons themselves
        if (evt.target.closest('button')) return;

        bubble.setPointerCapture(evt.pointerId);
        const { clientX, clientY } = evt;
        startX = clientX - offsetX;
        startY = clientY - offsetY;
        evt.preventDefault();
    });

    bubble.addEventListener('pointermove', evt => {
        if (!bubble.hasPointerCapture(evt.pointerId)) return;
        offsetX = evt.clientX - startX;
        offsetY = evt.clientY - startY;
        bubble.style.transform = `translate(${offsetX}px, ${offsetY}px)`;
    });

    bubble.addEventListener('pointerup', evt => {
        if (bubble.hasPointerCapture(evt.pointerId)) {
            bubble.releasePointerCapture(evt.pointerId);
        }
    });
});