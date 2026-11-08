(function () {
    "use strict";

    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    var input = document.getElementById("file_1");
    if (!input) {
        return;
    }

    var box = document.getElementById("selected-files");
    var list = box.querySelector("ul");
    var error = document.getElementById("file-error");
    var maxSize = Number(input.dataset.maxSize);

    function sizeLabel(bytes) {
        return bytes < 1048576 ? Math.ceil(bytes / 1024) + "KB" : (bytes / 1048576).toFixed(1) + "MB";
    }

    function render() {
        var files = Array.from(input.files);
        var oversized = files.some(function (file) { return file.size > maxSize; });

        list.textContent = "";
        files.forEach(function (file) {
            var item = document.createElement("li");
            var info = document.createElement("div");
            var name = document.createElement("div");
            info.className = "file-info";
            name.className = "file-name";
            name.textContent = file.name + " [" + sizeLabel(file.size) + "]";
            info.appendChild(name);
            item.appendChild(info);
            list.appendChild(item);
        });
        box.hidden = files.length === 0;

        error.hidden = !oversized;
        error.textContent = oversized ? "10MB를 초과하는 파일이 있습니다. 파일을 다시 선택해 주세요." : "";
        input.setCustomValidity(oversized ? "10MB를 초과하는 파일이 있습니다." : "");
    }

    input.addEventListener("change", render);

    input.closest(".file-upload").addEventListener("drop", function (event) {
        if (event.dataTransfer && event.dataTransfer.files.length > 0) {
            input.files = event.dataTransfer.files;
            render();
        }
    });
})();
