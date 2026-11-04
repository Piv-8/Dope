<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html lang="en" data-theme="light">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>ContainerFile Creation</title>

        <%@ include file="/html/cdn.html" %>

        <link rel="stylesheet" href="<c:url value='/protected/css/containerfile.css'/>">

    </head>

    <body>
    <%@ include file="/jsp/navbar.jsp" %>
    <%@ include file="/jsp/toast.jsp" %>

    <div class="container d-block" id="first_Pg">
        <div class="container-header">
            <h1>Create your Image</h1>

            <div class="stepper">
                <div class="step">
                    <div class="step-circle">1</div>
                    <div class="step-line inactive"></div>
                </div>
                <div class="step">
                    <div class="step-circle inactive" id="second_Step">2</div>
                </div>
            </div>
        </div>

        <div class="container-body">
            <div class="section">
                <h2 class="section-title"> Image Configuration </h2>
                <form id="first_Form">
                <div class="form-group">
                    <label for="NAMEID">Name:</label>
                    <input type="text" id="NAMEID" class="form-control" placeholder="Insert the name of your Image" pattern="^.+$" title="The Name of the containerfile must contain at least 1 characters" required>
                </div>

                <div class="form-group">
                    <label for="FROMID">FROM:</label>
                    <c:choose>
                        <c:when test="${not empty param.id_image}">
                            <input id="FROMID" name="from" type="text" class="form-control"
                                   value="${param.repo}/${param.tag_image}" readonly/>
                        </c:when>
                        <c:otherwise>
                            <input id="FROMID" name="from" type="text" class="form-control" placeholder="Es. ubuntu:22.04" pattern="^.+$" title="The FROM of the containerfile must contain at least 1 characters" required>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="form-group">
                    <label for="CMDID">CMD:</label>
                    <input type="text" id="CMDID" class="form-control" placeholder="Es. /bin/bash" pattern="^.+$" title="The CMD of the containerfile must contain at least 1 characters" required>
                </div>

                <div id="optionalCMD_div"></div>

                <div class="add-cmd">
                    <label>Add command:</label>
                    <button type="button" class="add-btn" id="optionalCMD_button">+</button>

                    <div class="optionalCMD d-none" id="optionalCMD">
                        <div class="command-tags">
                            <div class="tag">RUN</div>
                            <div class="tag">LABEL</div>
                            <div class="tag">EXPOSE</div>
                            <div class="tag">ENV</div>
                            <div class="tag">ENTRYPOINT</div>
                            <div class="tag">WORKDIR</div>
                        </div>
                    </div>
                </div>
                </form>
                <div class="footer">
                    <button type="button" class="btn btn-primary" id="next_Btn">Next</button>
                </div>
            </div>
        </div>
    </div>

    <div class="container d-none" id="second_Pg">
        <div class="container-header">
            <h1>Create your Image</h1>

            <div class="stepper">
                <div class="step">
                    <div class="step-circle completed" id="first_Step">1</div>
                    <div class="step-line"></div>
                </div>
                <div class="step">
                    <div class="step-circle">2</div>
                </div>
            </div>
        </div>

        <div class="container-body">
            <div class="section">
                <h2 class="section-title">Repository Information</h2>
                <form id="second_Form">
                    <%@ include file="/protected/jsp/repo_modal.jsp"%>

                    <div class="form-group">
                        <label for="Img_Tag">[Name:TAG]:</label>
                        <input type="text" id="Img_Tag" class="form-control" pattern="^.+$" title="The Name:TAG of the Image must contain at least 1 characters" required>
                    </div>
                </form>
            </div>

            <div class="section">
                <h2 class="section-title">Image Description:</h2>
                <form id="third_Form">
                    <div class="form-group">
                        <label for="Img_DESCRIPTION">Description:</label>
                        <textarea class="form-control" id="Img_DESCRIPTION" title="The Description of the Image must contain at least 1 characters" required> </textarea>
                    </div>
                </form>
                <div class="footer">
                    <button class="btn btn-secondary" id="back_Btn">Back</button>
                    <button class="btn btn-primary" id="create_Btn">Create</button>
                </div>
            </div>
        </div>
    </div>


    <!-- Modale di Loading -->
    <div class="modal-backdrop d-none" id="loadingBackdrop"></div>
    <div class="modal loading-modal d-none" id="loadingModal" tabindex="-1" aria-labelledby="loadingModalLabel" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-body">
                    <div class="custom-spinner loading-spinner mx-auto"></div>
                    <div class="loading-text">We are building your Image</div>
                    <div class="loading-subtext">This process may take some minute ...</div>
                </div>
            </div>
        </div>
    </div>

    <script> const idImage = "${param.id_image}"; </script>
    <script type="text/javascript" src="<c:url value="/protected/js/addOptionalCMD.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/protected/js/loadSecondForm.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/protected/js/createContainerFile.js"/>"></script>

    </body>
</html>

