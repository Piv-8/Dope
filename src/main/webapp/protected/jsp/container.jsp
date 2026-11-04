<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TEST Image: </title>

    <%@ include file="/html/cdn.html" %>

    <link rel="stylesheet" href="<c:url value='/protected/css/containerStyle.css' />">

</head>
<body>
<%@ include file="/jsp/navbar.jsp" %>
<div class="container">
    <div class="container-header">
        <header>
            <h2>TEST Image: <c:if test="${!empty tag}">
                <c:out value="${tag}"/></c:if></h2>
            <p>Status: <span id="status">Not Yet Created</span></p>
        </header>
    </div>

    <input type="hidden" id="isContainerCreated" name="isContainerCreated" value="0">

    <div class="container-body">

        <label for="commandInput">Command:</label><br>

        <div class="form-group">
                <input type="text" class="form-control" id="commandInput" placeholder="Insert command here"/>
                <button class="btn btn-primary" id="runBtn" onclick="executeCommand()">RUN</button>
        </div>

        <div class="logs-row">
            <section id="commandlog-section" class="log-section">
                <label for="commandLOG">Command LOG</label>
                <pre id="commandLOG" class="form-control" placeholder="Here will be displayed the Command logs" disabled ></pre>
            </section>

            <section id="containerlog-section" class="log-section">
                <label for="containerLOG">Container LOG</label>
                <pre id="containerLOG" class="form-control" placeholder="Here will be displayed the Container logs" disabled></pre>
            </section>
        </div>

        <div class="containerButtons">
        <section id="action-buttons">
            <button class="btn btn-success btn-floating" type="button" title="Create" id="createBtn" onclick="createContainer()">
                <i class="bi bi-plus"></i>
            </button>
            <button class="btn btn-secondary btn-floating" type="button" title="Start" id="startBtn" onclick="startAction()">
                <i class="bi bi-play"></i>
            </button>
            <button class="btn btn-secondary btn-floating" type="button" title="Kill" id="killBtn" onclick="killAction()">
                <i class="bi bi-stop"></i>
            </button>
        </section>
        </div>

        <div class="d-flex justify-content-center commit-button">
    		<button type="button" class="btn btn-primary" data-bs-toggle="modal" id="openCommitBtn" data-bs-target="#commitModal">
    			Commit
    		</button>
        </div>

		<!-- Commit Modal -->
		<div class="modal fade" id="commitModal" tabindex="-1" aria-labelledby="commitModalLabel" data-bs-backdrop="false">
			<div class="modal-dialog modal-dialog-scrollable">
				<div class="modal-content">
                    <form id="commitForm" class="needs-validation" novalidate>
					    <div class="modal-header">
					    	<h5 class="modal-title" id="commitModalLabel">Commit to image</h5>
					    	<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
					    </div>
					    <div class="modal-body">
                            <label for="commitTag" class="form-label">TAG</label>
                            <br>
                            <input
                                    type="text"
                                    class="form-control"
                                    id="commitTag"
                                    name="tag"
                                    placeholder="TAG"
                                    required
                            >
                            <div class="invalid-feedback">
                                Please enter a valid tag
                            </div>

    				        <!-- Include the repository selector button here, note id: "selectedRepoId" -->
                            <%@ include file="/protected/jsp/repo_modal.jsp"%>


                            <label for="imageDescription" class="form-label">Description</label>
                            <br>
                            <textarea
                                    class="form-control"
                                    id="imageDescription"
                                    name="description"
                                    placeholder="Description"
                                    rows="3"
                                    required
                            ></textarea>
                            <div class="invalid-feedback">
                                A description is required.
                            </div>


					    </div>
					    <div class="modal-footer">
					    	<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
					    	<input type="submit" class="btn btn-primary" id="commitButton" value="Commit">
					    </div>
                    </form>
				</div>
			</div>
		</div>

        <!-- Loading Modal -->
        <div class="modal-backdrop d-none" id="loadingBackdrop"></div>
        <div class="modal loading-modal d-none" id="loadingModal" tabindex="-1" aria-labelledby="loadingModalLabel" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-body">
                        <div class="custom-spinner loading-spinner mx-auto"></div>
                        <div class="loading-text">We are committing your Image</div>
                        <div class="loading-subtext">This process may take some minute ...</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- For debug purposes -->
        <div class="form-group" hidden>
            <section>
                <label for="debug">DEBUG</label>
                <textarea disable id="debug" class="form-control" placeholder="Here will be displayed Debug massages"></textarea>
            </section>
        </div>

    </div>
</div>

<script type="text/javascript">
    const container_url = "<c:url value="/api/"/>protected/container";
    const image_run = <c:if test="${!empty id_image}"><c:out value="${id_image}"/></c:if>;
    const user_instance =  <c:choose><c:when test="${!empty id_user}"><c:out value="${id_user}"/></c:when><c:otherwise>1</c:otherwise></c:choose>;
</script>

<script type="text/javascript" src="<c:url value="/protected/js/container.js"/>"></script>

<script>setContainerCreated(false);</script>

</body>
</html>
