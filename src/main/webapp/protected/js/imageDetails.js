// Handle image download functionality using data attributes
function downloadImage() {
    const tag = document.body.dataset.tag;
    const downloadUrl = document.body.dataset.downloadUrl;

    if (!tag || !downloadUrl) {
        console.error("Missing tag or download URL");
        return;
    }

    fetch(downloadUrl)
        .then(response => {
            if (!response.ok) throw new Error("Download failed");
            return response.blob();
        })
        .then(blob => {
            const link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = `${tag}.tar`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        })
        .catch(error => {
            console.error("Error downloading image:", error);
        });
}

//Function for delete an Image
function  deleteImage() {
    if (!confirm("Are you sure you want to delete this image?")) return;

    const xhr = new XMLHttpRequest();
    xhr.open('DELETE', `${contextPath}/api/protected/image/`+imageId, true);
    xhr.setRequestHeader('Content-Type', 'application/json');

    xhr.onload = function () {
        if (xhr.status === 200) {
            showToast("Image deleted successfully.",1);
            window.location.href = `${contextPath}`;
        } else {
            try {
                const err = JSON.parse(xhr.responseText);
                showToast("Error deleting image: " + (err.message.message || "Deletion failed."),2);
            } catch (e) {
                showToast("Error deleting image: Deletion failed.",2);
            }
        }
    };

    xhr.onerror = function () {
        showToast("Network error while attempting to delete the image.",2);
    };

    xhr.send();
}

// Fetch containerfile content and display it in the designated <pre> tag
document.addEventListener("DOMContentLoaded", function () {
    const containerfile = document.getElementById("containerfile_area");
    const containerfileUrl = document.body.dataset.containerfileUrl;
    const button = document.getElementById('deleteButton');

    if (!containerfile || !containerfileUrl) {
        console.error("Missing containerfile element or URL");
        return;
    }

    fetch(containerfileUrl, {
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error("Failed to fetch containerfile");
            return response.json();
        })
        .then(data => {
            const cfile = data.containerfile;
            let areatext = `NAME ${cfile.name}\nFROM ${cfile.from}\n`;

            for (let key in cfile.json_obj) {
                areatext += `${key.toUpperCase()} ${cfile.json_obj[key]}\n`;
            }

            areatext += `CMD ${cfile.cmd}\n`;
            containerfile.textContent = areatext;
        })
        .catch(error => {
            console.error("Error fetching containerfile:", error);
        });

    if (button) {
        button.addEventListener('click', deleteImage);
    }

    // Format .layer-size values using filesize.js
    document.querySelectorAll('.layer-size').forEach(el => {
        const sizeInBytes = parseInt(el.dataset.size, 10);
        if (!isNaN(sizeInBytes)) {

            el.textContent = filesize(sizeInBytes, {base: 2}); // Usa base 2 per 1024 MB
        } else {
            el.textContent = "N/A";
            console.warn("Invalid size value for element:", el);
        }
    });

});
