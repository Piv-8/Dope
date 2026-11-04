document.getElementById("create_Btn").addEventListener("click", () => check_Validation("second_Form"));
console.log("Event listener added to create_Btn.");

console.log("Event listener added to sub_button.");

function createContainerfile() {
    var xhr = new XMLHttpRequest();
    const url = "http://localhost:8081/dope-webapp/api/protected/image";

    let data_img = {image:{}};
    data_img.image.id_image = 1;
    data_img.image.tag = document.getElementById("Img_Tag").value;
    data_img.image.repo = parseInt(document.getElementById("selectedRepoId").value);
    data_img.image.description = document.getElementById("Img_DESCRIPTION").value;
    data_img.image.status = "building";

    console.log("Img_TAG threshold: {", data_img.image.tag, "}.");
    console.log("Request URL: {", url, "}.");

    xhr.open('POST', url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.responseType = 'json';

    xhr.onload = function() {
        if (xhr.status === 201) {
            console.log('Image created:', xhr.response);
            sendContainerfile(xhr.response.image.id_image);
        } else {
            console.error('Error in the creation of the Image:', xhr.status);
            hideLoadingModal();
            manageError(xhr);
        }
    };

    xhr.onerror = function() {
        console.error('Network Error!');
        hideLoadingModal();
        showToast('Network Error during Image creation',2);
    };

    console.log(JSON.stringify(data_img));
    xhr.send(JSON.stringify(data_img));
    console.log("HTTP POST request sent.");
}

function sendContainerfile(id_image) {
    var xhr = new XMLHttpRequest();
    const url = "http://localhost:8081/dope-webapp/api/protected/buildimage";

    let data_cf = {containerfile:{}};

    // Mandatory parameters
    data_cf.containerfile.name = document.getElementById("NAMEID").value;
    if (typeof idImage !== "undefined" && idImage !== "")
        data_cf.containerfile.from = "localhost/" + idImage;
    else
        data_cf.containerfile.from = document.getElementById("FROMID").value;
    data_cf.containerfile.cmd = document.getElementById("CMDID").value;

    data_cf.containerfile.json_obj = {};

    // Optional parameters
    let CMD_list = ["run", "label", "expose", "env", "entrypoint", "workdir"];

    CMD_list.forEach(cmd => {
        param = document.getElementsByName(cmd);
        if(param.length !== 0) {
            let values = [];
            param.forEach(val => {
                values.push(val.value);
            });
            data_cf.containerfile.json_obj[cmd] = values;
        }
    });

    data_cf.containerfile.image_built = id_image;

    console.log("CF_Name threshold: {", data_cf.containerfile.name, "}.");
    console.log("Request URL: {", url, "}.");

    xhr.open('POST', url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.responseType = 'json';
    xhr.withCredentials = true;

    xhr.onload = function() {


        if (xhr.status === 201) {
            hideLoadingModal();
            console.log('Containerfile created:', xhr.response);
            const id_image_built = xhr.response.containerfile.image_built;
            window.location.href = 'http://localhost:8081/dope-webapp/protected/showImage?id_image=' + encodeURIComponent(id_image_built);
        } else {
            hideLoadingModal();
            console.error('Error in the creation of the Containerfile:', xhr.status);
            manageError(xhr);
        }
    };

    xhr.onerror = function() {
        console.error('Network Error!');
        hideLoadingModal();
        showToast('Network Error during the creation of the Containerfile',2);
    };

    console.log(JSON.stringify(data_cf));
    xhr.send(JSON.stringify(data_cf));
    console.log("HTTP POST request sent.");
}

function manageError(xhr) {
    console.log('xhr.response:', xhr.response.message);

    let msg = 'Unknown error';
    try {
        const payload = xhr.response.message;
        msg = `${payload['error-code']} Error: ${payload['message']}`;
    } catch (e) {
        msg = "Error " + xhr.status + ' ' + xhr.statusText;
    }

    hideLoadingModal(() => {
        showToast(msg,2);
    });
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

