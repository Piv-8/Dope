const canvas = document.getElementById('bgCanvas');
const ctx    = canvas.getContext('2d');

const DRAWING_WIDTH  = 50;
const DRAWING_HEIGHT = 50;

// Scale factor
const SCALE_RATIO = 0.9;
let scale_factor

// Resize canvas to exactly cover the viewport in device pixels
function resizeCanvas() {
    canvas.width  = window.innerWidth;
    canvas.height = window.innerHeight;

    const minDim = Math.min(canvas.width, canvas.height);
    //Make the scale factor such that it's always the defined scale ratio
    scale_factor = (minDim * SCALE_RATIO) / DRAWING_WIDTH;
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();  // initial sizing

// Rotation state
let angle = 0;

// Main animation loop
function animate() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.translate(canvas.width / 2, canvas.height / 2);
    ctx.scale(scale_factor,scale_factor);
    ctx.rotate(angle);
    ctx.translate(-DRAWING_WIDTH / 2, - DRAWING_HEIGHT / 2);
    drawPodman(ctx);
    ctx.restore();
    angle += 0.0007;  // radians per frame (~0.03°)
    requestAnimationFrame(animate);
}

// Kick off the loop
animate();