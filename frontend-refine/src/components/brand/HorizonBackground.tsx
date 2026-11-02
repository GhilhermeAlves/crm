/**
 * Fundo da página inicial: um "horizonte" luminoso — arco de luz na base, halo
 * cônico girando devagar e grade em perspectiva sumindo no preto.
 * Só CSS (sem canvas nem JS): a única animação é um transform de rotação, que
 * roda na GPU. Respeita prefers-reduced-motion.
 */
export function HorizonBackground({ className = "" }: { className?: string }) {
  return (
    <div aria-hidden="true" className={`pointer-events-none overflow-hidden ${className}`}>
      {/* estrelas: pontos fixos via gradientes repetidos */}
      <div
        className="absolute inset-0 opacity-60"
        style={{
          backgroundImage:
            "radial-gradient(1px 1px at 20px 30px, rgba(255,255,255,.7), transparent), radial-gradient(1px 1px at 140px 90px, rgba(255,255,255,.5), transparent), radial-gradient(1px 1px at 90px 160px, rgba(255,255,255,.35), transparent)",
          backgroundSize: "220px 200px",
          maskImage: "linear-gradient(to bottom, black 0%, transparent 70%)",
        }}
      />

      {/* halo cônico girando atrás do horizonte */}
      <div className="absolute left-1/2 top-[62%] aspect-square w-[160vmax] -translate-x-1/2">
        <div
          className="h-full w-full animate-[spin_40s_linear_infinite] rounded-full opacity-40 blur-3xl motion-reduce:animate-none"
          style={{
            background:
              "conic-gradient(from 0deg, transparent 0deg, rgba(255,255,255,.35) 40deg, transparent 90deg, transparent 180deg, rgba(255,255,255,.25) 230deg, transparent 280deg)",
          }}
        />
      </div>

      {/* grade em perspectiva no "chão" */}
      <div
        className="absolute inset-x-0 bottom-0 h-[45%]"
        style={{
          backgroundImage:
            "linear-gradient(rgba(255,255,255,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.08) 1px, transparent 1px)",
          backgroundSize: "64px 64px",
          transform: "perspective(400px) rotateX(60deg)",
          transformOrigin: "bottom",
          maskImage: "linear-gradient(to top, black 10%, transparent 90%)",
        }}
      />

      {/* arco do horizonte: planeta escuro com borda iluminada */}
      <div className="absolute left-1/2 top-[68%] aspect-square w-[180vmax] -translate-x-1/2 rounded-full bg-black shadow-[0_-2px_0_0_rgba(255,255,255,.55),0_-30px_120px_10px_rgba(255,255,255,.18)]" />

      {/* vinheta para o texto respirar */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_30%,rgba(0,0,0,.85)_100%)]" />
    </div>
  );
}
