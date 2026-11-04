<%--suppress XmlPathReference --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>

<input type="hidden" id="selectedRepoId" name="selectedRepoId" value="" required>

<!-- Button to trigger modal -->
    <label for="selectRepoBtn">Repository </label>
    <br>
    <button type="button"
            id="selectRepoBtn"
            class="btn btn-primary"
            data-bs-toggle="modal"
            data-bs-target="#repoModal">
        Select
    </button>
    <div class="invalid-feedback" id="repoFeedback" style="display:none;">
        Please select a repository.
    </div>
    <span id="confirmText" style="display: none;" class="text-success ml-2"></span>
    <span id="confirmIcon"
          class="text-success ml-2"
          style="font-size: 1.5rem; display: none;">
      &#10004;
    </span>
    <br>




<!-- Repo Modal -->
<div class="modal fade" id="repoModal" tabindex="-1" role="dialog"
     aria-labelledby="repoModalLabel" aria-hidden="true" data-bs-backdrop="false">
    <div class="modal-dialog modal-dialog-centered modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="repoModalLabel">Choose a Repository</h5>
                <button type="button" class="btn-close" onclick="$('#repoModal').modal('hide')" 
                        aria-label="Close">
                </button>
            </div>
            <div class="modal-body" id="repo-list">
                <!-- Buttons will be injected here -->
                <p class="text-center text-muted">Loading…</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary"
                        onclick="$('#repoModal').modal('hide')">Close</button>
            </div>
        </div>
    </div>
</div>

<script>
    function loadRepositories() {
        var $list = $('#repo-list');
        $list.html('<p class="text-center text-muted">Loading…</p>');

        $.ajax({
            url: '<c:url value="/api/protected/repository"/>',
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                $list.empty();
                if (!data.length) {
                    $list.html('<p class="text-center">No repositories found. Create one from the homepage</p>');
                    return;
                }

                data.forEach(function(entry) {
                    var repo = entry.repository;
                    // Create a button for each repo
                    var $btn = $('<button/>', {
                        type: 'button',
                        class: 'btn btn-primary m-1 repo-btn',
                        text: repo.name + ' (ID: ' + repo.id_repo + ')'
                    }).data('repo-id', repo.id_repo);

                    $list.append($btn);
                });

                // Attach click handler to each dynamically generated button
                $list.find('.repo-btn').on('click', function(){
                    var selected = $(this).data('repo-id');
                    // 1) set the hidden input
                    $('#selectedRepoId').val(selected);
                    // 2) disable the main button and show the checkmark
                    $('#confirmIcon').show();
					// Show confirm text as the repo name
					$('#confirmText').text($(this).text());
					$('#confirmText').show();
                    // 3) close the modal
                    $('#repoModal').modal('hide');
                    // Validate repo selection
                    var repoSelected = !!document.querySelector('[name="selectedRepoId"]').value;
                    var repoFeedback = document.getElementById('repoFeedback');
                    if (!repoSelected) {
                        repoFeedback.style.display = 'block';
                    } else {
                        repoFeedback.style.display = 'none';
                    }
                });
            },
            error: function(xhr, status, err) {
                $list.html(
                    '<div class="alert alert-danger" role="alert">' +
                    'Error loading repositories: ' + err +
                    '</div>'
                );
            }
        });
    }

    $(function(){
        $('#repoModal').on('show.bs.modal', function(){
            loadRepositories();
        });
    });
</script>
