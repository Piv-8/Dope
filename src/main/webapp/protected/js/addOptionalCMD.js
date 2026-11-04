document.addEventListener("DOMContentLoaded", () =>
{
    document.getElementById("optionalCMD_button").addEventListener("click", toggle_OptionalCMD);
    console.log("Event listener added to optionalCMD_button.");

    document.querySelectorAll('#optionalCMD .tag').forEach(optCMD_List =>
    {
        optCMD_List.addEventListener("click", function ()
        {
            console.log("Event listener added to optionalCMD_button.");

            const optCMD = this.textContent.trim();
            console.log("Added Command: ", optCMD);

            add_OptionalCMD(optCMD);
        });
    });
});


function toggle_OptionalCMD()
{
    var div = document.getElementById("optionalCMD");
    if (div.classList.contains("d-none"))
    {
        div.classList.remove("d-none");
        div.classList.add("d-flex");
    }
    else
    {
        div.classList.remove("d-flex");
        div.classList.add("d-none");
    }
}

function add_OptionalCMD(optCMD) {
    if (!optCMD) {
        alert("Select a type of command to add");
        return;
    }

    const optCMD_div = document.getElementById('optionalCMD_div');

    const optCMD_form = document.createElement('div');
    optCMD_form.className = "form-group optional-cmd-container";

    const optCMD_label = document.createElement('label');
    optCMD_label.textContent = `${optCMD}:`;
    optCMD_label.setAttribute('for', `${optCMD}ID`);

    // Container per input e bottone X
    const inputContainer = document.createElement('div');
    inputContainer.className = 'optional-input-container';

    const optCMD_input = document.createElement('input');
    optCMD_input.type = 'text';
    optCMD_input.className = 'form-control'
    optCMD_input.name = `${optCMD.toLowerCase()}`;
    optCMD_input.id = `${optCMD}ID`;
    optCMD_input.pattern="^.+$";
    optCMD_input.title="The " + optCMD + " command of the containerfile must contain at least 1 characters";
    optCMD_input.required = true;

    // Bottone X per rimuovere il comando
    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.className = 'remove-cmd-btn';
    removeBtn.innerHTML = '×';
    removeBtn.title = `Remove ${optCMD} command`;

    // Event listener per rimuovere il comando
    removeBtn.addEventListener('click', function() {
        optCMD_form.remove();
    });

    inputContainer.appendChild(optCMD_input);
    inputContainer.appendChild(removeBtn);

    optCMD_form.appendChild(optCMD_label);
    optCMD_form.appendChild(inputContainer);

    optCMD_div.appendChild(optCMD_form);

    toggle_OptionalCMD();
}

