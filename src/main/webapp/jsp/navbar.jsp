<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/jsp/toast.jsp" %>
<canvas id="bgCanvas"></canvas>
<script type="text/javascript" src="<c:url value="/js/podman_canvas.js"/>"></script>
<script type="text/javascript" src="<c:url value="/js/bounce_canvas.js"/>"></script>
<link rel="stylesheet" href="<c:url value='/protected/css/navbar.css'/>">
<div class="navbar">
    <div>
    <a href="/dope-webapp" title="Go to the homepage" style="font-weight: bold;">Dope WebApp</a>
        <button id="theme-toggle" aria-label="Toggle theme" title="Switch dark/light mode">
            <i id="theme-icon" class="fas fa-moon"></i>
        </button>
    </div>
    <div style="float: right; margin-left: 10px;">
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
            Welcome, ${sessionScope.user}!
             <a href="/dope-webapp/logout">Logout</a>
            </c:when>
        </c:choose>
    </div>
</div>
<!-- script that switches the dark/light theme -->
<script>
    const toggleBtn = document.getElementById('theme-toggle');
    const themeIcon = document.getElementById('theme-icon');
    const root = document.documentElement;

    function setThemeIcon(theme) {
        if (theme === 'dark') {
            themeIcon.classList.remove('fa-moon');
            themeIcon.classList.add('fa-sun');
        } else {
            themeIcon.classList.remove('fa-sun');
            themeIcon.classList.add('fa-moon');
        }
    }

    function updateCloseButtons(theme) {
        // Select all close buttons on the page
        const closeButtons = document.querySelectorAll('.btn-close');

        closeButtons.forEach(btn => {
            if (theme === 'dark') {
                // Add the white variant
                btn.classList.add('btn-close-white');
            } else {
                // Remove the white variant
                btn.classList.remove('btn-close-white');
            }
        });
    }

    const savedTheme = localStorage.getItem('theme') || 'light';
    root.setAttribute('data-theme', savedTheme);
    setThemeIcon(savedTheme);
    updateCloseButtons(savedTheme);

    toggleBtn.addEventListener('click', () => {
        const current = root.getAttribute('data-theme') || 'light';
        const newTheme = current === 'light' ? 'dark' : 'light';
        root.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        setThemeIcon(newTheme);
        updateCloseButtons(newTheme);
    });
</script>
<!-- DYNAMIC padding-Top -->
<script>
    window.addEventListener('load', () => {
        const navbar = document.querySelector('.navbar');
        if (navbar) {
            const navbarHeight = navbar.offsetHeight;
            document.body.style.paddingTop = navbarHeight + 'px';
        }
    });
</script>