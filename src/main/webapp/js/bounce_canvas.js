const canvas = document.getElementById('bgCanvas');
const ctx    = canvas.getContext('2d');

// Dimensions of the vector drawing (in its own coordinate space)
const DRAWING_WIDTH  = 50;
const DRAWING_HEIGHT = 50;

// Scale factor
const SCALE_RATIO = 0.60;
let scale_factor

let x, y;
let vx = 0.5, vy = 0.3;
let angle = 0;
const ANGULAR_SPEED = 0.002; // radians per frame

function resizeCanvas() {
    canvas.width  = window.innerWidth;
    canvas.height = window.innerHeight;

    const minDim = Math.min(canvas.width, canvas.height);
    scale_factor = (minDim * SCALE_RATIO) / DRAWING_WIDTH;

    if (x === undefined || y === undefined) {
        // place at a random in-bounds start
        x = Math.random() * (canvas.width  - DRAWING_WIDTH * scale_factor);
        y = Math.random() * (canvas.height - DRAWING_HEIGHT * scale_factor);
    } else {
        // clamp to new size if user resized window
        x = Math.min(Math.max(x, 0), canvas.width  - DRAWING_WIDTH * scale_factor);
        y = Math.min(Math.max(y, 0), canvas.height - DRAWING_HEIGHT * scale_factor);
    }
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();

// Main loop: update pos & angle, bounce, then draw
function animate() {
    // Move x
    x += vx;
    y += vy;
    angle += ANGULAR_SPEED;

    // Bounce off edges
    if (x <= 0) {
        x = 0;
        vx = Math.abs(vx);
    } else if (x + DRAWING_WIDTH * scale_factor >= canvas.width) {
        x = canvas.width - DRAWING_WIDTH * scale_factor;
        vx = -Math.abs(vx);
    }
    if (y <= 0) {
        y = 0;
        vy = Math.abs(vy);
    } else if (y + DRAWING_HEIGHT * scale_factor >= canvas.height) {
        y = canvas.height - DRAWING_HEIGHT * scale_factor;
        vy = -Math.abs(vy);
    }

    // Clear and set up transform:
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();

    // Rotation stuff
    const cx = x + (DRAWING_WIDTH * scale_factor)  / 2;
    const cy = y + (DRAWING_HEIGHT * scale_factor) / 2;
    ctx.translate(cx, cy);
    ctx.rotate(angle);
    // Scale and center rotation
    ctx.scale(scale_factor, scale_factor);
    ctx.translate(-DRAWING_WIDTH / 2, -DRAWING_HEIGHT / 2);

    drawPodman(ctx);
    ctx.restore();

    requestAnimationFrame(animate);
}

animate();