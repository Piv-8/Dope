<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/html/cdn.html" %>
<%@ page isELIgnored="false" %>
<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Authentication</title>

  <link rel="stylesheet" href="<c:url value='/css/authenticationStyle.css'/>">
</head>
<body>

<canvas id="bgCanvas"></canvas>

<main class="auth-wrapper">
  <%@ include file="/jsp/toast.jsp" %>
  <div class="authentication-header" id="login-form">
    <!-- Theme Toggle Button -->
    <button id="theme-toggle" aria-label="Toggle theme" title="Switch dark/light mode">
      <i id="theme-icon" class="fas fa-moon"></i>
    </button>
    <h2>Login</h2>
    <c:if test="${not empty loginError}">
      <script>
        document.addEventListener("DOMContentLoaded", function () {
          const $toast = $('#liveToast');
          const $toastBody = $toast.find('.toast-body');
          const $toastHeader = $toast.find('.toast-header');
          const message = "<c:out value='${loginError}'/>";
          const $btnClose = $toast.find('.btn-close');
          if (message.trim() !== "") {
            $toastBody.contents().filter(function() {
              return this.nodeType === 3;
            }).first().replaceWith(message);
            $toastBody.find('.toast-progress').remove();
            const progress = $('<div class="toast-progress"></div>');
            $toastBody.append(progress);
            $toastHeader.find('.modal-title').replaceWith("Authentication failed!" + '\n');
            $toastHeader.css({
              'background-color': 'yellow',
              'color': 'black'
            });
            $btnClose.css({
              'color': 'black'
            })
            $toast.toast({ delay: 5000 }).toast('show');
          }
        });
      </script>
    </c:if>

  <form action="<%= request.getContextPath() %>/login" method="POST">
    <input type="hidden" name="action" value="login">
    <div class="authentication-form-group">
      <label for="username">Username:</label>
      <input type="text" class="form-control" id="username" name="username" required>
    </div>
    <div class="authentication-form-group">
      <label for="password">Password:</label>
      <input type="password" class="form-control" id="password" name="password" required>
    </div>
    <div class="authentication-form-group">
      <input class="hidden-sub" type="submit" id="hidden-submit">
    </div>
  </form>
  <div class="form-group">
    <button class= "btn btn-primary" id= "show_register_button" onclick="showReg()">Register</button>
    <button type="button" class="btn btn-primary" id="trigger-submit" onclick="document.getElementById('hidden-submit').click();">Login</button>
  </div>
</div>

<div class="authentication-header" id="register-form" style="display:none">
  <button id="theme-toggle-register" aria-label="Toggle theme" title="Switch dark/light mode">
    <i id="theme-icon-register" class="fas fa-moon"></i>
  </button>
  <h2>Register</h2>
  <div class="authentication-form-group">
    <label for="username">Username:</label>
    <input type="text" class="form-control" id="username_signup" required pattern="^[a-zA-Z0-9._\-]{3,}$" title="Username must be at least 3 characters and contain only letters, numbers, '.', '_', or '-'">
  </div>
  <div class="authentication-form-group">
    <label for="regPwd">Password:</label>
    <input type="password" class="form-control" id="regPwd" required pattern="^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$" title="Password must be at least 8 characters long and contain at least one letter and one number">
  </div>
  <div class="authentication-form-group">
    <label for="regPwdConfirm">Confirm Password:</label>
    <input type="password" class="form-control" id="regPwdConfirm" required>
  </div>
  <div class="authentication-form-group">
    <label for="name">Name:</label>
    <input type="text" class="form-control" id="name" required pattern=".*[A-Za-z].*" title="Name must contain at least one letter">
  </div>
  <div class="authentication-form-group">
    <label for="surname">Surname:</label>
    <input type="text" class="form-control" id="surname" required pattern=".*[A-Za-z].*" title="Surname must contain at least one letter">
  </div>
  <div class="birth-form-group">
    <label for="birth">Birth Date:</label>
    <input type="date" class="form-control" id="birth" required min="1900-01-01" max="2100-12-31">
  </div>
  <div class="form-group">
    <button class= "btn btn-primary" id= "register_button" onclick="registerUser(event)">Register</button>
    <button class= "btn btn-primary" id= "show_login_button" onclick="showLogin()">Login</button>
  </div>
</div>
</main>
<!-- JavaScript -->
<script>
  function showReg(){
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('register-form').style.display = 'block';
  }

  function showLogin(){
    document.getElementById('register-form').style.display = 'none';
    document.getElementById('login-form').style.display = 'block';
  }


  function registerUser(e){

    e.preventDefault();

    const username_dom = document.getElementById('username_signup');
    const password_dom = document.getElementById('regPwd');
    const name_dom = document.getElementById('name');
    const surname_dom = document.getElementById('surname');
    const birthDate_dom = document.getElementById('birth');

    const username = username_dom.value;
    const password = password_dom.value;
    const name = name_dom.value;
    const surname = surname_dom.value;
    const birthDate = birthDate_dom.value;

    if(!username_dom.reportValidity() || !password_dom.reportValidity() || !name_dom.reportValidity() || !surname_dom.reportValidity() || !birthDate_dom.reportValidity()){
      return;
    }

    const p1 = document.getElementById('regPwd').value;
    const p2 = document.getElementById('regPwdConfirm').value;
    if (p1 !== p2) {
      showToast("Password and confirmation password must match!");
      return;
    }

    var xhr = new XMLHttpRequest();
    xhr.open('POST', '<%= request.getContextPath() %>/api/user', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    let user = {user: {id_user: 1, username: username, password: password, name: name, surname: surname, birth_date: birthDate}};
    xhr.responseType = 'json';
    xhr.send(JSON.stringify(user));

    xhr.onreadystatechange = function() {
      if (xhr.readyState === 4) {
        if (xhr.status === 201) {
          showToast("Registered successfully!",1);
        } else if (xhr.status === 409) {
          showToast("User already exists!");
        } else {
          showToast("Internal error: registration failed",2);
        }
      }
    };
  }

  //For theme swap
  const toggleBtn = document.getElementById('theme-toggle');
  const toggleBtnRegister = document.getElementById('theme-toggle-register');
  const themeIcon = document.getElementById('theme-icon');
  const themeIconRegister = document.getElementById('theme-icon-register');
  const root = document.documentElement;

  function setThemeIcon(theme) {
    if (theme === 'dark') {
      themeIcon.classList.remove('fa-moon');
      themeIconRegister.classList.remove('fa-moon');
      themeIcon.classList.add('fa-sun');
      themeIconRegister.classList.add('fa-sun');
    } else {
      themeIcon.classList.remove('fa-sun');
      themeIconRegister.classList.remove('fa-sun');
      themeIcon.classList.add('fa-moon');
      themeIconRegister.classList.add('fa-moon');
    }
  }

  const savedTheme = localStorage.getItem('theme') || 'light';
  root.setAttribute('data-theme', savedTheme);
  setThemeIcon(savedTheme);

  toggleBtn.addEventListener('click', switchTheme);
  toggleBtnRegister.addEventListener('click', switchTheme);

  function switchTheme(){
    const current = root.getAttribute('data-theme') || 'light';
    const newTheme = current === 'light' ? 'dark' : 'light';
    root.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
    setThemeIcon(newTheme);
  }
</script>
<script type="text/javascript" src="<c:url value="/js/podman_canvas.js"/>"></script>
<script type="text/javascript" src="<c:url value="/js/rotate_canvas.js"/>"></script>
</body>
</html>
