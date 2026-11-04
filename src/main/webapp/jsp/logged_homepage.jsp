<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html data-theme="light">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1"> <!-- ESSENZIALE per responsive -->

  <%@ include file="/html/cdn.html" %>

  <link rel="stylesheet" href="<c:url value='/protected/css/loggedHomepage.css' />">

  <title>My Repositories</title>
</head>


<body>
<%@ include file="/jsp/navbar.jsp" %>

<div class="container py-3">

  <div class="container-header">
    <h1>Available Repositories</h1>

    <div class="repoButtons">
      <!-- Button for creating repo -->
      <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#exampleModal">
        New Repository
      </button>
      <!-- Button for creating image -->
      <a href="<c:url value="/protected/jsp/Containerfile.jsp"/>" class="btn btn-primary">New Image</a>
    </div>
  </div>

  <!-- List repositories -->
  <div class="repo-list">
    <c:forEach var="repo" items="${repositories}">
      <div class="repo-card">
        <div class="repo-header">
          <strong class="repo-name">${repo.name}</strong>
          <c:choose>
            <c:when test="${repo.visibility == 'public'}">
              <span class="repo-badge public">${repo.visibility}</span>
            </c:when>
            <c:otherwise>
              <span class="repo-badge private">${repo.visibility}</span>
            </c:otherwise>
          </c:choose>
        </div>
        <div class="repo-actions">
          <!-- Expand repository button -->
          <form method="get" action="index.html" class="d-inline">
            <input type="hidden" name="action" value="getImages" />
            <input type="hidden" name="repoId" value="${repo.ID_repo}" />
            <button type="submit" class="btn btn-outline btn-sm-custom">
              Expand
            </button>
          </form>
          <!-- Delete repository button -->
          <c:if test="${sessionScope.userId == repo.owner_ID}">
            <form method="post" action="index.html" class="d-inline" onsubmit="return confirm('Are you sure you want to delete this repository?');">
              <input type="hidden" name="action" value="deleteRepository" />
              <input type="hidden" name="repoId" value="${repo.ID_repo}" />
              <button type="submit" class="btn btn-outline-danger btn-sm-custom">
                <i class="bi bi-trash"></i>
              </button>
            </form>
          </c:if>
        </div>

        <!-- List images -->
        <c:if test="${param.repoId == repo.ID_repo}">
          <ul class="list-group">
            <c:choose>
              <c:when test="${not empty images}">
                <c:forEach var="img" items="${images}">
                  <li class="list-group-item-image">
                    <a class="repo-link" href="/dope-webapp/protected/showImage?id_image=${img.ID_image}">
                        ${img.tag} (${img.status}) - ${img.description}
                    </a>

                    <!-- Delete image form -->
                    <c:if test="${sessionScope.userId == repo.owner_ID}">
                      <form method="post" action="index.html" class="d-inline ms-2" onsubmit="return confirm('Are you sure you want to delete this image?');">
                        <input type="hidden" name="action" value="deleteImage" />
                        <input type="hidden" name="imgId" value="${img.ID_image}" />
                        <input type="hidden" name="repoId" value="${repo.ID_repo}" />
                        <button type="submit" class="btn btn-outline-danger btn-sm-custom" title="Delete Image">
                          <i class="bi bi-trash"></i>
                        </button>
                      </form>
                    </c:if>
                  </li>
                </c:forEach>
              </c:when>
              <c:otherwise>
                <li class="list-group-item-image"><em class="no-images">No images found</em></li>
              </c:otherwise>
            </c:choose>
          </ul>
        </c:if>
      </div>
    </c:forEach>
  </div>
  </div>
</div>

<!-- Modal -->
<div class="modal fade" id="exampleModal" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered custom-modal">
    <div class="modal-content">
      <form action="${pageContext.request.contextPath}/index.html" method="post">
        <div class="modal-header">
          <h5 class="modal-title" id="exampleModalLabel">Create Repository</h5>
          <button type="button" class="btn-close btn-close-custom" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <!-- Radio buttons -->
          <div class="form-check">
            <input class="form-check-input" type="radio" name="visibility" value="public" id="publicRadio" required>
            <label class="form-check-label" for="publicRadio">Public</label>
          </div>
          <div class="form-check mb-3">
            <input class="form-check-input" type="radio" name="visibility" value="private" id="privateRadio">
            <label class="form-check-label" for="privateRadio">Private</label>
          </div>

          <!-- Text input -->
          <div class="mb-3 d-flex justify-content-center">
            <label for="repoName" class="form-label">Repository Name</label>
            <input type="text" class="form-control" id="repoName" name="repo" required>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="submit" class="btn btn-primary">Create</button>
        </div>
      </form>
    </div>
  </div>
</div>

<script>
  const modal = document.getElementById('exampleModal');

  modal.addEventListener('show.bs.modal', function () {
    setTimeout(() => {
      const backdrop = document.querySelector('.modal-backdrop');
      if (backdrop) {
        backdrop.classList.add('blur');
      }
    }, 10);
  });

  modal.addEventListener('hidden.bs.modal', function () {
    const backdrop = document.querySelector('.modal-backdrop');
    if (backdrop) {
      backdrop.classList.remove('blur');
    }
  });
</script>

<c:if test="${not empty toastMessage}">
<script>
  document.addEventListener("DOMContentLoaded", function() {
    showToast("${toastMessage}",1);
  });
</script>
</c:if>

</body>
</html>
