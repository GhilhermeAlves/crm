// Sobe o build `output: "standalone"` localmente/no E2E, espelhando o Dockerfile:
// o standalone não inclui `.next/static` nem `public/`, que precisam ser
// copiados para ao lado do server.js antes de iniciar.
import { cpSync, existsSync } from "node:fs";

const root = ".next/standalone";
cpSync(".next/static", `${root}/.next/static`, { recursive: true });
if (existsSync("public")) cpSync("public", `${root}/public`, { recursive: true });

await import(`./../${root}/server.js`);
