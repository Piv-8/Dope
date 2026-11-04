<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<html data-theme="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1"> <!-- ESSENZIALE per responsive -->

  <%@ include file="/html/cdn.html" %>

  <link rel="stylesheet" type="text/css" href="<c:url value='/protected/css/imageDetails.css' />">

  <title>Details of Image
    <c:if test="${!empty tag}">
      <c:out value="${tag}"/>
    </c:if>
  </title>
</head>

<body data-id-image="${id_image}"
      data-tag="${tag}"
      data-containerfile-url="<c:url value='/api/protected/containerfile/${id_image}' />"
      data-download-url="<c:url value='/api/protected/exportImage/${id_image}' />">
<%@ include file="/jsp/navbar.jsp" %>

<div class="container py-3">

  <div class="row align-items-center mb-3 px-3 px-md-5">
    <!-- Info repo + image -->
    <div class="col-12 image-info mb-3">
      <div class="repo-author">
        <h2 class="repo-and-image d-inline">
            <span class="repo-name">
              <c:if test="${!empty repo.name}">
                <c:out value="${repo.name}"/>
              </c:if>
            </span>
            /
            <span class="image-name">
              <c:if test="${!empty tag}">
                <c:out value="${tag}"/>
              </c:if>
            </span>

          <!-- Delete Button -->
          <button type="button" class="btn btn-sm btn-outline-danger ms-3" id="deleteButton">
            <i class="bi bi-trash"></i> Delete
          </button>
        </h2>
      </div>
      <h3 class="author-info">
        Author:
        <c:if test="${!empty user.username}">
          <c:out value="${user.username}"/>
        </c:if>
      </h3>
    </div>

    <!-- Pulsanti -->
    <div class="row mt-3">
      <div class="col-12 d-flex flex-wrap gap-2 justify-content-center">
      <a href="<c:url value="/protected/jsp/Containerfile.jsp?id_image=${id_image}&tag_image=${tag}&repo=${repo.name}"/>"
         class="btn btn-sm btn-primary">Create from this</a>
      <a href="<c:url value='/protected/'/>container?id_image=${id_image}"
         class="btn btn-sm btn-secondary">Test</a>
      <button onclick="downloadImage()"
              class="btn btn-sm btn-success">Download Image</button>
      </div>
    </div>
  </div>

  <div class="container-body">
    <div class="logs-row row">
      <section class="log-section col-12 col-md-6 mb-4">
        <h5 class="mb-3">Description</h5>
        <pre class="form-control">
          <c:if test="${!empty description}">
            <c:out value="${description}"/>
          </c:if>
        </pre>
      </section>

      <section class="log-section col-12 col-md-6 mb-4">
        <h5 class="mb-3">Containerfile</h5>
        <pre id="containerfile_area" class="form-control"></pre>
      </section>
    </div>

    <div class="mb-5">
      <h5 class="mb-3">Layers</h5>
      <div id="img_layers" class="layers-list">
        <c:choose>
          <c:when test="${!empty layers}">
            <ul class="list-group">
              <c:forEach var="layer" items="${layers}">
                <li class="list-group-item d-flex justify-content-between align-items-center">
                  <span class="form-control">
                    <i class="bi bi-stack me-2"></i>
                    <!--${repo.name}/${tag} (ID: ${layer.ID_Layer})-->
                    Layer: ${layer.ID_Layer}
                  </span>
                  <span class="badge layer-size" data-size="${layer.size}">Loading...</span>
                </li>
              </c:forEach>
            </ul>
          </c:when>
          <c:otherwise>
            <div class="fst-italic"><em class="no-layers">No layers found</em></div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>

</div>
<script>
  const imageId = ${id_image};
  const contextPath = "${pageContext.request.contextPath}";
</script>
<script type="text/javascript" src="<c:url value='/protected/js/imageDetails.js'/>"></script>

</body>
</html>
