/*
 * Fundo animado da tela de login — port em JS puro do VortexBackground.tsx da
 * landing (frontend-refine/src/components/brand/VortexBackground.tsx). Mantenha
 * os dois em sincronia. Um "funil" de fios brancos finos desce do topo,
 * estreita num pescoço e se abre num piso elíptico, girando devagar.
 * Respeita prefers-reduced-motion (quadro estático) e pausa com a aba oculta.
 * Sem efeito nas páginas que não têm o <canvas id="crm-vortex">.
 */
(function () {
  "use strict";

  var STRANDS = 260;
  var SEGMENTS = 110;
  var SPEED = 0.06; // rad/s

  function start() {
    var canvas = document.getElementById("crm-vortex");
    var ctx = canvas && canvas.getContext ? canvas.getContext("2d") : null;
    if (!canvas || !ctx) return;

    var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var strands = [];
    for (var i = 0; i < STRANDS; i++) {
      strands.push({
        angle: (i / STRANDS) * Math.PI * 2,
        dir: i % 2 === 0 ? 1 : -1, // duas famílias em sentidos opostos → trama cruzada
        alpha: 0.05 + Math.random() * 0.1,
      });
    }
    var stars = [];
    for (var j = 0; j < 70; j++) {
      stars.push({ x: Math.random(), y: Math.random() * 0.7, a: 0.15 + Math.random() * 0.45 });
    }

    var width = 0;
    var height = 0;
    var frame = 0;
    var running = true;

    function resize() {
      var dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }

    function draw(time) {
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = "#000";
      ctx.fillRect(0, 0, width, height);

      for (var s = 0; s < stars.length; s++) {
        ctx.fillStyle = "rgba(255,255,255," + stars[s].a + ")";
        ctx.fillRect(stars[s].x * width, stars[s].y * height, 1, 1);
      }

      var cx = width / 2;
      var topY = -height * 0.08;
      var neckY = height * 0.68;
      var floorY = height * 1.02;
      var topR = Math.max(width * 0.34, 260);
      var neckR = Math.max(width * 0.045, 28);
      var floorR = Math.max(width * 0.75, 420);
      var spin = time * SPEED;

      ctx.globalCompositeOperation = "lighter";
      ctx.lineWidth = 0.6;
      for (var n = 0; n < strands.length; n++) {
        var strand = strands[n];
        ctx.beginPath();
        for (var k = 0; k <= SEGMENTS; k++) {
          var t = k / SEGMENTS;
          var y = topY + (floorY - topY) * t;
          var r, squash, twist, u;
          if (y < neckY) {
            u = (neckY - y) / (neckY - topY); // 1 no topo → 0 no pescoço
            r = neckR + (topR - neckR) * Math.pow(u, 1.7);
            squash = 0.1;
            twist = (1 - u) * 2.4;
          } else {
            u = (y - neckY) / (floorY - neckY); // 0 no pescoço → 1 no piso
            r = neckR + (floorR - neckR) * Math.pow(u, 2.4);
            squash = 0.16;
            twist = 2.4 + u * 1.8;
          }
          var phi = strand.angle + strand.dir * (twist + spin);
          var x = cx + r * Math.cos(phi);
          var py = y + r * Math.sin(phi) * squash;
          if (k === 0) ctx.moveTo(x, py);
          else ctx.lineTo(x, py);
        }
        ctx.strokeStyle = "rgba(255,255,255," + strand.alpha + ")";
        ctx.stroke();
      }

      // vinheta para o card respirar e as bordas sumirem no preto
      ctx.globalCompositeOperation = "source-over";
      var vignette = ctx.createRadialGradient(cx, height * 0.45, height * 0.1, cx, height * 0.5, width * 0.75);
      vignette.addColorStop(0, "rgba(0,0,0,0)");
      vignette.addColorStop(1, "rgba(0,0,0,0.85)");
      ctx.fillStyle = vignette;
      ctx.fillRect(0, 0, width, height);
    }

    function loop(now) {
      if (!running) return;
      draw(now / 1000);
      frame = requestAnimationFrame(loop);
    }

    document.addEventListener("visibilitychange", function () {
      if (reduceMotion) return;
      running = !document.hidden;
      cancelAnimationFrame(frame);
      if (running) frame = requestAnimationFrame(loop);
    });

    if (typeof ResizeObserver !== "undefined") {
      new ResizeObserver(function () {
        resize();
        if (reduceMotion) draw(0);
      }).observe(canvas);
    } else {
      window.addEventListener("resize", function () {
        resize();
        if (reduceMotion) draw(0);
      });
    }
    resize();

    if (reduceMotion) draw(0);
    else frame = requestAnimationFrame(loop);
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", start);
  else start();
})();
