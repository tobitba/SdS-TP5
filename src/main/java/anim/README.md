# Guía para correr esta fucking animación

Necesitás tener node y pnpm
Necesitás instalar [ffmpeg](https://www.gyan.dev/ffmpeg/builds)

Parándote en animator, corré:
```bash
pnpm install
```

Luego correr:
```bash
node main.js -i ../../../../output.txt -o out --width 0.2 --height 0.7 --opening 0.03 --A 0.0015 --w0 400 --fps 30
```