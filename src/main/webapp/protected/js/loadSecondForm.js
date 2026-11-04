document.addEventListener("DOMContentLoaded", () =>
{
    document.getElementById("next_Btn").addEventListener("click", () => check_Validation("first_Form"));
    console.log("Event listener added to next_Btn.");

    document.getElementById("back_Btn").addEventListener("click", load_FirstForm);
    console.log("Event listener added to back_Btn.");

    document.getElementById("second_Step").addEventListener("click", () => check_Validation("first_Form"));
    console.log("Event listener added to second_Step.");

    document.getElementById("first_Step").addEventListener("click", load_FirstForm);
    console.log("Event listener added to first_Step.");
});

function check_Validation(form_id)
{
    const form = document.querySelector(`#${form_id}`);

    const selectedRepoField = document.getElementById('selectedRepoId');
    const repoFeedback = document.getElementById('repoFeedback');

    if(form_id === "second_Form")
    {
        if (!selectedRepoField.value) {
            repoFeedback.style.display = 'block';
            selectedRepoField.setCustomValidity('Please select a repository.');
            form.reportValidity();
            return;
        } else {
            repoFeedback.style.display = 'none';
            selectedRepoField.setCustomValidity('');
        }
    }

    for (const field of form.elements)
    {
        if (field.tagName !== 'INPUT' && field.tagName !== 'TEXTAREA') continue;
        if (field.disabled || field.readOnly || field.type === 'button') continue;

        field.setCustomValidity('');

        if (field.value.trim() === '')
            field.setCustomValidity(`${field.title}`);
    }

    if (form.checkValidity())
    {
        switch(form_id)
        {
            case "first_Form":
                load_SecondForm();
                break;
            case "second_Form":
                check_Validation("third_Form")
                break;
            case "third_Form":
                showLoadingModal();
                createContainerfile();
                break;
        }
    }
    else
        form.reportValidity();
}

function load_SecondForm()
{
    const first_Pg = document.getElementById("first_Pg");
    const second_Pg = document.getElementById("second_Pg");

    first_Pg.classList.remove("d-block");
    first_Pg.classList.add("d-none");

    second_Pg.classList.remove("d-none");
    second_Pg.classList.add("d-block");
}

function load_FirstForm()
{
    const first_Pg = document.getElementById("first_Pg");
    const second_Pg = document.getElementById("second_Pg");

    second_Pg.classList.remove("d-block");
    second_Pg.classList.add("d-none");

    first_Pg.classList.remove("d-none");
    first_Pg.classList.add("d-block");
}