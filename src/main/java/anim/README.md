# Guía para correr esta fucking animación

Necesitás tener node y pnpm
Necesitás instalar [ffmpeg](https://www.gyan.dev/ffmpeg/builds)

Parándote en animator, corré:
```bash
pnpm install
```

Luego correr:
```bash
pnpm exec node main.js ../<sim_file> -S <S> -L <L> --video-width 1200 --video-fps 20
```