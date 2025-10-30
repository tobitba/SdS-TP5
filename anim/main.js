/**
 * animate_silo.js
 *
 * Requisitos:
 *   npm install canvas commander
 *   ffmpeg (opcional, para compilar video/gif)
 *
 * Uso:
 *   node animate_silo.js -i data.txt -o outdir --width 1.0 --height 2.0 --opening 0.2 --A 0.02 --w0 30 --fps 30
 *
 * Explicación rápida de parámetros (ver commander help):
 *   --width (W)       : ancho del silo (unidades del archivo)
 *   --height (L)      : altura del silo (unidades del archivo)
 *   --opening (D)     : ancho de la abertura en el centro del piso (unidades del archivo)
 *   --A               : amplitud de vibración del piso (misma unidad que y)
 *   --w0              : frecuencia angular (rad/s) de la vibración
 *   --scale           : píxeles por unidad (para la resolución del canvas)
 *   --apply-floor-to-particles : si true, suma el desplazamiento del piso a las posiciones y de las partículas (false por defecto)
 *   --frames-only     : solo genera PNGs, no compila con ffmpeg
 *
 * Formato de entrada:
 *   tiempo(float) - flowtotal(long)
 *   x,y,vx,vy,r
 *   ...
 *
 */

import fs from "fs";
import { createCanvas } from "canvas";
import { spawn } from "child_process";
import readline from "readline";
import { Command } from "commander";
import path from "path";

const program = new Command();

program
    .requiredOption('-i, --input <file>', 'Archivo .txt con datos')
    .option('-o, --outdir <dir>', 'Directorio de salida', 'out')
    .option('--width <num>', 'Ancho del silo W (unidades del archivo)', parseFloat, 1.0)
    .option('--height <num>', 'Altura del silo L (unidades del archivo)', parseFloat, 2.0)
    .option('--opening <num>', 'Abertura del piso D (unidades del archivo)', parseFloat, 0.2)
    .option('--A <num>', 'Amplitud de vibración del piso (misma unidad que y)', parseFloat, 0.02)
    .option('--w0 <num>', 'Frecuencia angular w0 (rad/s)', parseFloat, 30.0)
    .option('--fps <num>', 'Frames por segundo para la salida', parseInt, 30)
    .option('--scale <num>', 'Píxeles por unidad (escala)', parseFloat, 400)
    .option('--apply-floor-to-particles', 'Aplicar desplazamiento del piso también a partículas', false)
    .option('--frames-only', 'Solo generar PNGs, no compilar con ffmpeg', false)
    .option('--particle-scale <num>', 'Factor multiplicativo para el radio de dibujo (por si los radios son muy pequeños)', parseFloat, 1.0)
    .parse(process.argv);

const opts = program.opts();

// create outdir
if (!fs.existsSync(opts.outdir)) fs.mkdirSync(opts.outdir, { recursive: true });

/**
 * Parser del archivo de entrada.
 * Lee todo en memoria (si el archivo es enorme podrías adaptarlo a streaming),
 * y produce un array de frames:
 *   frames = [{ t: Number, flowtotal: Number, particles: [{x,y,vx,vy,r}, ...] }, ...]
 */
function parseInputFile(pathFile) {
  const text = fs.readFileSync(pathFile, 'utf8');
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
  const frames = [];
  let i = 0;
  const headerRegex = /^([+\-]?\d*\.?\d+(?:[eE][+\-]?\d+)?)\s*-\s*(\d+)$/;

  while (i < lines.length) {
    const headerMatch = lines[i].match(headerRegex);
    if (!headerMatch) {
      // si hay líneas basura, intenta saltarlas
      console.warn(`Línea ${i+1} no parece header, se salta: "${lines[i]}"`);
      i++;
      continue;
    }
    const t = parseFloat(headerMatch[1]);
    const flowtotal = parseInt(headerMatch[2], 10);
    i++;

    const particles = [];
    // leer hasta siguiente header o fin
    while (i < lines.length && !headerRegex.test(lines[i])) {
      // formato: x,y,vx,vy,r
      const parts = lines[i].split(',').map(s => s.trim());
      if (parts.length >= 5) {
        const [xs, ys, vxs, vys, rs] = parts;
        const x = parseFloat(xs);
        const y = parseFloat(ys);
        const vx = parseFloat(vxs);
        const vy = parseFloat(vys);
        const r = parseFloat(rs);
        if (![x,y,vx,vy,r].some(Number.isNaN)) {
          particles.push({ x, y, vx, vy, r });
        } else {
          console.warn(`Línea ${i+1} con datos no numéricos: "${lines[i]}"`);
        }
      } else {
        console.warn(`Línea ${i+1} con formato inesperado: "${lines[i]}"`);
      }
      i++;
    }

    frames.push({ t, flowtotal, particles });
  }

  return frames;
}

function worldToCanvas(x, y, opts) {
  // origen mundo: (0,0) en la esquina inferior izquierda del silo
  // canvas: (0,0) es esquina superior izquierda. Mapear y = 0 -> canvasHeight - marginBottom
  const margin = 20; // px
  const canvasW = Math.round(opts.width * opts.scale) + margin * 2;
  const canvasH = Math.round(opts.height * opts.scale) + margin * 2;

  const cx = margin + x * opts.scale;
  const cy = margin + (opts.height - y) * opts.scale; // invertir eje Y
  return { cx, cy, canvasW, canvasH };
}

function drawFrameToCanvas(frame, idx, opts) {
  const W = opts.width;
  const L = opts.height;
  const D = opts.opening;
  const A = opts.A;
  const w0 = opts.w0;
  const t = frame.t;
  const applyFloorToParticles = opts.applyFloorToParticles;
  const particleRadiusScale = opts.particleScale || 1.0;

  // compute floor displacement (vertical)
  const floorOffset = A * Math.sin(w0 * t); // en unidades del mundo
  // floor Y position base (antes de vibrar) será 0 (suponemos piso en y = 0)
  const floorY = 0 + floorOffset;

  // build a canvas sized to W x L with margins
  const margin = 20;
  const canvasWpx = Math.round(W * opts.scale) + margin * 2;
  const canvasHpx = Math.round(L * opts.scale) + margin * 2;
  const canvas = createCanvas(canvasWpx, canvasHpx);
  const ctx = canvas.getContext('2d');

  // background
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvasWpx, canvasHpx);

  // draw silo walls (simple rectangles)
  ctx.fillStyle = '#cccccc'; // walls color
  const wallThickness = Math.max(2, Math.round(8 * (opts.scale / 400))); // adaptivo

  // left wall - draw as vertical rectangle covering full height
  ctx.fillRect(margin - wallThickness, 0, wallThickness, canvasHpx);
  // right wall
  ctx.fillRect(canvasWpx - margin, 0, wallThickness, canvasHpx);

  // draw floor as two rectangles (left and right segments) leaving opening in center
  const leftFloorX_world = 0;
  const rightFloorX_world = W;
  const leftSegmentWorldW = (W - D) / 2.0;
  const rightSegmentWorldX = (W + D) / 2.0;

  // transform Y
  const yf_canvas = worldToCanvas(0, floorY, opts).cy;

  const floorHeightPx = Math.max(2, Math.round(6 * (opts.scale / 400)));

  ctx.fillStyle = '#888888';
  // left floor segment
  const leftX_px = worldToCanvas(leftFloorX_world, 0, opts).cx;
  const leftW_px = Math.round(leftSegmentWorldW * opts.scale);
  ctx.fillRect(leftX_px, yf_canvas - floorHeightPx / 2, leftW_px, floorHeightPx);

  // right floor segment
  const rightX_px = worldToCanvas(rightSegmentWorldX, 0, opts).cx;
  const rightW_px = Math.round(leftSegmentWorldW * opts.scale);
  ctx.fillRect(rightX_px, yf_canvas - floorHeightPx / 2, rightW_px, floorHeightPx);

  // draw top border / roof (optional)
  ctx.strokeStyle = '#999999';
  ctx.lineWidth = 1;
  ctx.strokeRect(margin - wallThickness, margin, canvasWpx - 2 * (margin - wallThickness), canvasHpx - margin * 2);

  // draw particles
  for (const p of frame.particles) {
    const py = applyFloorToParticles ? p.y + floorOffset : p.y;
    const { cx, cy } = worldToCanvas(p.x, py, opts);
    const rpx = Math.max(1, p.r * opts.scale * particleRadiusScale);

    // avoid drawing particles outside bounds (optionally)
    ctx.beginPath();
    ctx.arc(cx, cy, rpx, 0, Math.PI * 2);
    // color by whether particle is above opening and near it (simple heuristic)
    // si y < 0 (por debajo del piso) lo dibujamos semitransparente
    if (py < 0) {
      ctx.fillStyle = 'rgba(255,0,0,0.45)';
      ctx.fill();
      ctx.strokeStyle = 'rgba(200,0,0,0.6)';
      ctx.lineWidth = 0.5;
      ctx.stroke();
    } else {
      ctx.fillStyle = 'rgba(30,144,255,0.9)';
      ctx.fill();
      ctx.strokeStyle = 'rgba(10,10,10,0.4)';
      ctx.lineWidth = 0.5;
      ctx.stroke();
    }
  }

  // draw a label with time and flowtotal
  ctx.fillStyle = '#000000';
  ctx.font = `${Math.max(12, Math.round(14 * (opts.scale / 400)))}px Sans`;
  ctx.textBaseline = 'top';
  const label = `t=${t.toFixed(3)} s  flowtotal=${frame.flowtotal}`;
  ctx.fillText(label, margin + 6, 6);

  // draw a tiny legend for floor amplitude and w0
  ctx.fillStyle = '#333333';
  const info = `A=${A}, w0=${w0}`;
  ctx.fillText(info, margin + 6, canvasHpx - 20);

  // save PNG
  const fname = path.join(opts.outdir, `frame${String(idx).padStart(5, '0')}.png`);
  const out = fs.createWriteStream(fname);
  const stream = canvas.createPNGStream();
  stream.pipe(out);

  return new Promise((resolve, reject) => {
    out.on('finish', () => resolve(fname));
    out.on('error', reject);
  });
}

(async () => {
  try {
    const frames = parseInputFile(opts.input);
    if (frames.length === 0) {
      console.error('No se detectaron frames en el archivo de entrada.');
      process.exit(1);
    }
    console.log(`Frames parseados: ${frames.length}`);

    // guardar cada frame
    for (let i = 0; i < frames.length; i++) {
      process.stdout.write(`Generando frame ${i+1}/${frames.length}...\r`);
      await drawFrameToCanvas(frames[i], i + 1, {
        width: opts.width,
        height: opts.height,
        opening: opts.opening,
        A: opts.A,
        w0: opts.w0,
        scale: opts.scale,
        applyFloorToParticles: !!opts.applyFloorToParticles,
        particleScale: opts.particleScale || 1.0,
        outdir: opts.outdir // 🔧 Agregado para evitar undefined
      });
    }
    console.log('\nPNG frames generados en:', opts.outdir);

    if (opts.framesOnly) {
      console.log('--frames-only activado; no se compila video con ffmpeg.');
      return;
    }

    // compilar video con ffmpeg
    // Requiere ffmpeg instalado y accesible en PATH.
    const outVideo = path.join(opts.outdir, 'silo_animation.mp4');
    // comando: ffmpeg -y -framerate <fps> -i frame%05d.png -c:v libx264 -pix_fmt yuv420p out.mp4
    console.log('Compilando video con ffmpeg...');
    await new Promise((resolve, reject) => {
      const args = [
        '-y',
        '-framerate', String(opts.fps),
        '-i', path.join(opts.outdir, 'frame%05d.png'),
        '-c:v', 'libx264',
        '-pix_fmt', 'yuv420p',
        outVideo
      ];
      const ff = spawn('ffmpeg', args, { stdio: 'inherit' });
      ff.on('close', (code) => {
        if (code === 0) resolve();
        else reject(new Error(`ffmpeg finalizó con código ${code}`));
      });
    });

    console.log('Vídeo generado en:', outVideo);
  } catch (err) {
    console.error('Error:', err);
    process.exit(1);
  }
})();
