function showPopup(popupId) {
    var popup = document.getElementById(popupId);
    popup.classList.add("show");
}

function closePopup(popupId) {
    var popup = document.getElementById(popupId);
    popup.classList.remove("show");

    var form = popup.querySelector("form");
    form.reset();
}

function addFileInput(id) {
    const fileInputs = document.getElementById(id);
    const newInputDiv = document.createElement('div');
    const newInput = document.createElement('input');
    newInput.type = 'file';
    newInput.required = true;
    newInput.name = 'tests';
    newInput.accept = 'text/xml';
    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.textContent = 'Убрать';
    removeButton.addEventListener('click', function() {
        fileInputs.removeChild(newInputDiv);
    });
    newInputDiv.appendChild(newInput);
    newInputDiv.appendChild(removeButton);
    fileInputs.appendChild(newInputDiv);
}

function removeFileInput(inputDiv, id) {
    const fileInputs = document.getElementById(id);
    fileInputs.removeChild(inputDiv.parentElement);
}

function clearInputFile(id) {
    const fileInputs = document.getElementById(id);
    fileInputs.innerHTML = '';
}