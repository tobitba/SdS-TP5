/**
 * animate_silo_large.js
 *
 * Escala la animación para que sea más grande pero mantenga proporciones.
 *
 * Uso ejemplo:
 *   node animate_silo_large.js -i data.txt -o out --width 1.0 --height 2.0 --scale 300
 */

import fs from "fs";
import { createCanvas } from "canvas";
import { spawn } from "child_process";
import { Command } from "commander";
import path from "path";

const program = new Command();

program
    .requiredOption('-i, --input <file>', 'Archivo .txt con datos')
    .option('-o, --outdir <dir>', 'Directorio de salida', 'out')
    .option('--width <num>', 'Ancho del silo W (unidades)', parseFloat, 1.0)
    .option('--height <num>', 'Altura del silo L (unidades)', parseFloat, 2.0)
    .option('--opening <num>', 'Abertura del piso D (unidades)', parseFloat, 0.2)
    .option('--A <num>', 'Amplitud vibración del piso', parseFloat, 0.02)
    .option('--w0 <num>', 'Frecuencia angular w0 (rad/s)', parseFloat, 400)
    .option('--fps <num>', 'Frames por segundo', parseInt, 30)
    .option('--scale <num>', 'Píxeles por unidad', parseFloat, 900) // <--- ajusta aquí para tamaño final
    .option('--apply-floor-to-particles', 'Aplicar desplazamiento del piso a partículas', false)
    .option('--frames-only', 'Solo generar PNGs', false)
    .option('--particle-scale <num>', 'Factor multiplicativo para el radio de partículas', parseFloat, 1.0)
    .option('--supersample <num>', 'Factor de supersampling para suavizado', parseFloat, 1.0)
    .parse(process.argv);

const opts = program.opts();

// crear directorio de salida
if (!fs.existsSync(opts.outdir)) fs.mkdirSync(opts.outdir, { recursive: true });

// ==================== Parse Input ====================
function parseInputFile(pathFile) {
  const text = fs.readFileSync(pathFile, 'utf8');
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
  const frames = [];
  let i = 0;
  const headerRegex = /^([+\-]?\d*\.?\d+(?:[eE][+\-]?\d+)?)\s*-\s*(\d+)$/;

  while (i < lines.length) {
    const headerMatch = lines[i].match(headerRegex);
    if (!headerMatch) { i++; continue; }
    const t = parseFloat(headerMatch[1]);
    const flowtotal = parseInt(headerMatch[2], 10);
    i++;
    const particles = [];
    while (i < lines.length && !headerRegex.test(lines[i])) {
      const parts = lines[i].split(',').map(s => s.trim());
      if (parts.length >= 5) {
        const [xs, ys, vxs, vys, rs] = parts;
        const x = parseFloat(xs), y = parseFloat(ys);
        const vx = parseFloat(vxs), vy = parseFloat(vys);
        const r = parseFloat(rs);
        if (![x,y,vx,vy,r].some(Number.isNaN)) particles.push({x,y,vx,vy,r});
      }
      i++;
    }
    frames.push({ t, flowtotal, particles });
  }
  return frames;
}

// ==================== Transformación mundo -> canvas ====================
function worldToCanvas(x, y, opts) {
  const margin = 20 * opts.supersample;
  const canvasW = Math.round(opts.width * opts.scale * opts.supersample) + margin * 2;
  const canvasH = Math.round(opts.height * opts.scale * opts.supersample) + margin * 2;
  const cx = margin + x * opts.scale * opts.supersample;
  const cy = margin + (opts.height - y) * opts.scale * opts.supersample;
  return { cx, cy, canvasW, canvasH };
}

// ==================== Dibujar un frame ====================
async function drawFrameToCanvas(frame, idx, opts) {
  const W = opts.width, L = opts.height, D = opts.opening;
  const A = opts.A, w0 = opts.w0, t = frame.t;
  const applyFloorToParticles = opts.applyFloorToParticles;
  const particleRadiusScale = opts.particleScale || 1.0;
  const supersample = opts.supersample || 1.0;

  const floorOffset = A * Math.sin(w0 * t);
  const floorY = 0 + floorOffset;

  const margin = 20 * supersample;
  const canvasWpx = Math.round(W * opts.scale * supersample) + margin * 2;
  const canvasHpx = Math.round(L * opts.scale * supersample) + margin * 2;
  const canvas = createCanvas(canvasWpx, canvasHpx);
  const ctx = canvas.getContext('2d');

  ctx.antialias = 'subpixel';
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';

  // fondo
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvasWpx, canvasHpx);

  // paredes
  const wallThickness = Math.max(2, Math.round(8 * (opts.scale / 400) * supersample));
  ctx.fillStyle = '#cccccc';
  ctx.fillRect(margin - wallThickness, 0, wallThickness, canvasHpx);
  ctx.fillRect(canvasWpx - margin, 0, wallThickness, canvasHpx);

  // piso
  const leftSegmentWorldW = (W - D)/2.0;
  const rightSegmentWorldX = (W + D)/2.0;
  const yf_canvas = worldToCanvas(0, floorY, opts).cy;
  const floorHeightPx = Math.max(2, Math.round(6 * (opts.scale/400) * supersample));
  ctx.fillStyle = '#888888';
  ctx.fillRect(worldToCanvas(0,0,opts).cx, yf_canvas - floorHeightPx/2,
      Math.round(leftSegmentWorldW * opts.scale * supersample), floorHeightPx);
  ctx.fillRect(worldToCanvas(rightSegmentWorldX,0,opts).cx, yf_canvas - floorHeightPx/2,
      Math.round(leftSegmentWorldW * opts.scale * supersample), floorHeightPx);

  // techo
  ctx.strokeStyle = '#999999';
  ctx.lineWidth = 1 * supersample;
  ctx.strokeRect(margin - wallThickness, margin,
      canvasWpx - 2*(margin - wallThickness),
      canvasHpx - margin*2);

  // partículas
  for (const p of frame.particles) {
    const py = applyFloorToParticles ? p.y + floorOffset : p.y;
    const { cx, cy } = worldToCanvas(p.x, py, opts);
    const rpx = Math.max(1, p.r * opts.scale * particleRadiusScale * supersample);

    const grad = ctx.createRadialGradient(cx, cy, rpx*0.2, cx, cy, rpx);
    if (py < 0) { grad.addColorStop(0,'rgba(255,0,0,0.45)'); grad.addColorStop(1,'rgba(200,0,0,0.6)'); }
    else { grad.addColorStop(0,'rgba(30,144,255,0.9)'); grad.addColorStop(1,'rgba(30,144,255,0.4)'); }
    ctx.fillStyle = grad;
    ctx.beginPath(); ctx.arc(cx, cy, rpx, 0, Math.PI*2); ctx.fill();
    ctx.strokeStyle = 'rgba(10,10,10,0.4)';
    ctx.lineWidth = 0.5 * supersample; ctx.stroke();
  }

  // etiquetas
  ctx.fillStyle = '#000000';
  ctx.font = `${Math.max(12, Math.round(14*(opts.scale/400)*supersample))}px Sans`;
  ctx.textBaseline = 'top';
  ctx.fillText(`t=${t.toFixed(3)} s  flowtotal=${frame.flowtotal}`, margin+6,6);
  ctx.fillStyle = '#333333';
  ctx.fillText(`A=${A}, w0=${w0}`, margin+6, canvasHpx-20);

  // guardar PNG
  const fname = path.join(opts.outdir, `frame${String(idx).padStart(5,'0')}.png`);
  const out = fs.createWriteStream(fname);
  const stream = canvas.createPNGStream();
  stream.pipe(out);

  return new Promise((resolve,reject)=>{out.on('finish',()=>resolve(fname)); out.on('error',reject);});
}

// ==================== Loop principal ====================
(async()=>{
  try {
    const frames = parseInputFile(opts.input);
    if (frames.length===0){ console.error('No se detectaron frames.'); process.exit(1);}
    console.log(`Frames parseados: ${frames.length}`);

    for(let i=0;i<frames.length;i++){
      process.stdout.write(`Generando frame ${i+1}/${frames.length}...\r`);
      await drawFrameToCanvas(frames[i], i+1, opts);
    }
    console.log('\nPNG frames generados en:', opts.outdir);

    if(opts.framesOnly){ console.log('--frames-only activado.'); return; }

    const outVideo = path.join(opts.outdir,'silo_animation.mp4');
    console.log('Compilando video con ffmpeg...');
    await new Promise((resolve,reject)=>{
      const args=['-y','-framerate',String(opts.fps),'-i',path.join(opts.outdir,'frame%05d.png'),
        '-c:v','libx264','-crf','18','-preset','slow','-pix_fmt','yuv420p',outVideo];
      const ff=spawn('ffmpeg',args,{stdio:'inherit'});
      ff.on('close',code=>code===0?resolve():reject(new Error(`ffmpeg terminó con código ${code}`)));
    });
    console.log('Video generado en:', outVideo);

  } catch(err){ console.error('Error:',err); process.exit(1);}
})();
