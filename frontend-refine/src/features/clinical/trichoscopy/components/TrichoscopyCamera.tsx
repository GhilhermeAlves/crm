"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { format } from "date-fns";
import {
  Camera,
  Circle,
  Download,
  Keyboard,
  Loader2,
  RefreshCw,
  Save,
  Square,
  ZoomIn,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { useCaptureTrichoscopy } from "../hooks/useTrichoscopy";

type Props = {
  companyId: string;
  contactId: string;
  onSaved?: () => void;
};

type SaveTarget = "system" | "computer";

// Tricoscópios como o DH MAX entregam Full HD pelo USB; o navegador usa o máximo disponível.
const VIDEO_SIZE = { width: { ideal: 1920 }, height: { ideal: 1080 } };
const ZOOM_LEVELS = [1, 2, 4] as const;
const REGIONS = [
  "Frontal",
  "Vértex",
  "Temporal direita",
  "Temporal esquerda",
  "Parietal",
  "Occipital",
];
const NO_REGION = "none";
const SAVE_TARGET_KEY = "trichoscopy.saveTarget";
const DEVICE_KEY_KEY = "trichoscopy.captureKey";

function readStorage(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string | null) {
  try {
    if (value === null) window.localStorage.removeItem(key);
    else window.localStorage.setItem(key, value);
  } catch {
    // Preferência só do navegador; sem storage segue o padrão.
  }
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Recorte central do vídeo para o zoom digital. */
function drawZoomed(ctx: CanvasRenderingContext2D, video: HTMLVideoElement, zoom: number) {
  const w = video.videoWidth;
  const h = video.videoHeight;
  const sw = w / zoom;
  const sh = h / zoom;
  ctx.drawImage(video, (w - sw) / 2, (h - sh) / 2, sw, sh, 0, 0, w, h);
}

function isTypingTarget(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLElement &&
    (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName))
  );
}

/**
 * Captura pelo tricoscópio (câmera USB/UVC). A câmera liga ao montar e é
 * desligada ao desmontar, por isso o componente só é renderizado com o
 * diálogo de captura aberto.
 *
 * Foco e LEDs do aparelho são físicos; aqui ficam resolução máxima, zoom
 * digital, foto (sistema ou computador), vídeo (sempre no computador),
 * região do couro cabeludo e o botão de captura do aparelho (tecla aprendida).
 */
export function TrichoscopyCamera({ companyId, contactId, onSaved }: Props) {
  const uid = useId();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const previewRef = useRef<string | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const recordFrameRef = useRef<number | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const [devices, setDevices] = useState<MediaDeviceInfo[]>([]);
  const [deviceId, setDeviceId] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [starting, setStarting] = useState(false);
  const [resolution, setResolution] = useState<string | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [blob, setBlob] = useState<Blob | null>(null);
  const [notes, setNotes] = useState("");
  const [region, setRegion] = useState(NO_REGION);
  const [zoom, setZoom] = useState<number>(1);
  const [recording, setRecording] = useState(false);
  const [recordStart, setRecordStart] = useState<number | null>(null);
  const [elapsed, setElapsed] = useState(0);
  const [saveTarget, setSaveTarget] = useState<SaveTarget>("system");
  const [captureKey, setCaptureKey] = useState<string | null>(null);
  const [learningKey, setLearningKey] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const capture = useCaptureTrichoscopy(companyId, contactId);

  // O loop de gravação lê o zoom atual sem reiniciar o gravador.
  const zoomRef = useRef(zoom);
  useEffect(() => {
    zoomRef.current = zoom;
  }, [zoom]);

  useEffect(() => {
    setSaveTarget(readStorage(SAVE_TARGET_KEY) === "computer" ? "computer" : "system");
    setCaptureKey(readStorage(DEVICE_KEY_KEY));
  }, []);

  const chooseSaveTarget = (value: SaveTarget) => {
    setSaveTarget(value);
    writeStorage(SAVE_TARGET_KEY, value);
  };

  const stopStream = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setStreaming(false);
  }, []);

  const clearPreview = useCallback(() => {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = null;
    setPreview(null);
    setBlob(null);
  }, []);

  const loadDevices = useCallback(async () => {
    if (!navigator.mediaDevices?.enumerateDevices) {
      setError("Este navegador não permite acessar câmeras.");
      return;
    }
    try {
      const inputs = (await navigator.mediaDevices.enumerateDevices()).filter(
        (device) => device.kind === "videoinput",
      );
      setDevices(inputs);
      setDeviceId((current) => {
        if (current && inputs.some((d) => d.deviceId === current)) return current;
        const preferred = inputs.find((d) => /usb|microscope|scope|derm|dh/i.test(d.label));
        return preferred?.deviceId ?? inputs[0]?.deviceId ?? "";
      });
    } catch {
      setError("Não foi possível listar as câmeras.");
    }
  }, []);

  const startStream = useCallback(
    async (id: string) => {
      if (!navigator.mediaDevices?.getUserMedia) return;
      setError(null);
      setStarting(true);
      stopStream();
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: id ? { deviceId: { exact: id }, ...VIDEO_SIZE } : VIDEO_SIZE,
          audio: false,
        });
        streamRef.current = stream;
        const settings = stream.getVideoTracks()[0]?.getSettings();
        setResolution(
          settings?.width && settings?.height ? `${settings.width}×${settings.height}` : null,
        );
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => undefined);
        }
        setStreaming(true);
        // Os nomes das câmeras só aparecem depois da permissão concedida.
        void loadDevices();
      } catch {
        setError("Não foi possível acessar a câmera. Verifique a permissão do navegador.");
      } finally {
        setStarting(false);
      }
    },
    [loadDevices, stopStream],
  );

  const stopRecordingLoop = () => {
    if (recordFrameRef.current !== null) cancelAnimationFrame(recordFrameRef.current);
    recordFrameRef.current = null;
  };

  useEffect(() => {
    void loadDevices();
    return () => {
      if (recorderRef.current?.state === "recording") recorderRef.current.stop();
      stopRecordingLoop();
      stopStream();
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    };
  }, [loadDevices, stopStream]);

  useEffect(() => {
    if (!preview) void startStream(deviceId);
    // Só religa ao trocar de câmera; voltar da prévia chama startStream direto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceId]);

  useEffect(() => {
    if (!recordStart) return;
    const timer = setInterval(() => setElapsed(Math.floor((Date.now() - recordStart) / 1000)), 500);
    return () => clearInterval(timer);
  }, [recordStart]);

  const regionLabel = region === NO_REGION ? null : region;
  const fileBase = () =>
    `tricoscopia-${regionLabel ? `${regionLabel.toLowerCase().replace(/\s+/g, "-")}-` : ""}${format(
      new Date(),
      "yyyyMMdd-HHmmss",
    )}`;

  const takePhoto = useCallback(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !streaming || preview) return;
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    drawZoomed(ctx, video, zoom);
    canvas.toBlob(
      (result) => {
        if (!result) return;
        const url = URL.createObjectURL(result);
        previewRef.current = url;
        setBlob(result);
        setPreview(url);
        stopStream();
      },
      "image/jpeg",
      0.92,
    );
  }, [preview, stopStream, streaming, zoom]);

  const startRecording = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || typeof MediaRecorder === "undefined") {
      setError("Este navegador não permite gravar vídeo.");
      return;
    }
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    // Grava o que aparece na tela (com o zoom), desenhando o vídeo no canvas.
    const draw = () => {
      drawZoomed(ctx, video, zoomRef.current);
      recordFrameRef.current = requestAnimationFrame(draw);
    };
    draw();
    const mimeType = ["video/webm;codecs=vp9", "video/webm;codecs=vp8", "video/webm"].find((type) =>
      MediaRecorder.isTypeSupported(type),
    );
    const recorder = new MediaRecorder(
      canvas.captureStream(30),
      mimeType ? { mimeType, videoBitsPerSecond: 8_000_000 } : undefined,
    );
    chunksRef.current = [];
    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) chunksRef.current.push(event.data);
    };
    recorder.onstop = () => {
      stopRecordingLoop();
      const recorded = new Blob(chunksRef.current, { type: "video/webm" });
      chunksRef.current = [];
      if (recorded.size > 0) {
        downloadBlob(recorded, `${fileBase()}.webm`);
        toast.success("Vídeo salvo no computador (pasta Downloads).");
      }
    };
    recorder.start(1000);
    recorderRef.current = recorder;
    setRecording(true);
    setRecordStart(Date.now());
    setElapsed(0);
  };

  const stopRecording = () => {
    if (recorderRef.current?.state === "recording") recorderRef.current.stop();
    recorderRef.current = null;
    setRecording(false);
    setRecordStart(null);
  };

  // Botão de captura do aparelho: muitos tricoscópios USB enviam uma tecla.
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (learningKey) {
        event.preventDefault();
        setCaptureKey(event.code);
        writeStorage(DEVICE_KEY_KEY, event.code);
        setLearningKey(false);
        toast.success(`Botão do aparelho configurado (${event.code}).`);
        return;
      }
      if (!captureKey || event.code !== captureKey || isTypingTarget(event.target)) return;
      event.preventDefault();
      takePhoto();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [captureKey, learningKey, takePhoto]);

  const retake = () => {
    clearPreview();
    void startStream(deviceId);
  };

  const composedNotes = () =>
    [regionLabel && `Região: ${regionLabel}`, notes.trim()].filter(Boolean).join(" — ") ||
    undefined;

  const save = () => {
    if (!blob) return;
    if (saveTarget === "computer") {
      downloadBlob(blob, `${fileBase()}.jpg`);
      toast.success("Foto salva no computador (pasta Downloads).");
      clearPreview();
      setNotes("");
      void startStream(deviceId);
      return;
    }
    capture.mutate(
      { file: blob, notes: composedNotes() },
      {
        onSuccess: () => {
          clearPreview();
          setNotes("");
          onSaved?.();
        },
      },
    );
  };

  const formatElapsed = `${String(Math.floor(elapsed / 60)).padStart(2, "0")}:${String(
    elapsed % 60,
  ).padStart(2, "0")}`;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-2">
        <div className="min-w-56 flex-1 space-y-1">
          <Label htmlFor={`${uid}-device`}>Câmera / tricoscópio</Label>
          <Select
            value={deviceId}
            onValueChange={setDeviceId}
            disabled={devices.length === 0 || recording}
          >
            <SelectTrigger id={`${uid}-device`}>
              <SelectValue placeholder="Nenhuma câmera encontrada" />
            </SelectTrigger>
            <SelectContent>
              {devices.map((device, index) => (
                <SelectItem key={device.deviceId || index} value={device.deviceId}>
                  {device.label || `Câmera ${index + 1}`}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button
          type="button"
          variant="outline"
          onClick={() => void loadDevices()}
          disabled={recording}
        >
          <RefreshCw className="mr-2 h-4 w-4" /> Atualizar dispositivos
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-1">
          <Label htmlFor={`${uid}-region`}>Região do couro cabeludo</Label>
          <Select value={region} onValueChange={setRegion}>
            <SelectTrigger id={`${uid}-region`}>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NO_REGION}>Não informar</SelectItem>
              {REGIONS.map((r) => (
                <SelectItem key={r} value={r}>
                  {r}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label htmlFor={`${uid}-target`}>Salvar fotos</Label>
          <Select value={saveTarget} onValueChange={(v) => chooseSaveTarget(v as SaveTarget)}>
            <SelectTrigger id={`${uid}-target`}>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="system">No sistema (histórico do paciente)</SelectItem>
              <SelectItem value="computer">No meu computador</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="relative mx-auto aspect-video w-full max-w-[calc(70vh*16/9)] overflow-hidden rounded-lg bg-black">
        {preview ? (
          // eslint-disable-next-line @next/next/no-img-element -- prévia local (blob:)
          <img
            src={preview}
            alt="Prévia da captura tricoscópica"
            className="h-full w-full object-contain"
          />
        ) : (
          <video
            ref={videoRef}
            className="h-full w-full object-contain transition-transform"
            style={{ transform: `scale(${zoom})` }}
            playsInline
            muted
          />
        )}
        {starting && (
          <div className="absolute inset-0 flex items-center justify-center text-white">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        )}
        {recording && (
          <div className="absolute left-3 top-3 flex items-center gap-1.5 rounded bg-black/60 px-2 py-1 text-xs font-medium text-white">
            <span className="h-2 w-2 animate-pulse rounded-full bg-red-500" /> REC {formatElapsed}
          </div>
        )}
        {resolution && !preview && (
          <div className="absolute bottom-3 right-3 rounded bg-black/60 px-2 py-1 text-xs text-white">
            {resolution}
            {zoom > 1 ? ` · zoom ${zoom}×` : ""}
          </div>
        )}
      </div>
      <canvas ref={canvasRef} className="hidden" />

      {!preview && (
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1" role="group" aria-label="Zoom digital">
            <ZoomIn className="mr-1 h-4 w-4 text-muted-foreground" />
            {ZOOM_LEVELS.map((level) => (
              <Button
                key={level}
                type="button"
                size="sm"
                variant={zoom === level ? "default" : "outline"}
                className="h-8 px-2.5"
                aria-pressed={zoom === level}
                onClick={() => setZoom(level)}
              >
                {level}×
              </Button>
            ))}
          </div>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            className={cn("gap-1.5", learningKey && "text-primary")}
            onClick={() => setLearningKey((v) => !v)}
          >
            <Keyboard className="h-4 w-4" />
            {learningKey
              ? "Aperte o botão do aparelho…"
              : captureKey
                ? `Botão do aparelho: ${captureKey}`
                : "Configurar botão do aparelho"}
          </Button>
          {captureKey && !learningKey && (
            <Button
              type="button"
              size="sm"
              variant="ghost"
              className="text-muted-foreground"
              onClick={() => {
                setCaptureKey(null);
                writeStorage(DEVICE_KEY_KEY, null);
              }}
            >
              Remover
            </Button>
          )}
        </div>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      {preview && (
        <div className="space-y-1">
          <Label htmlFor={`${uid}-notes`}>Observações (opcional)</Label>
          <Textarea
            id={`${uid}-notes`}
            value={notes}
            maxLength={1900}
            placeholder="Achados, comparação com exames anteriores…"
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        {preview ? (
          <>
            <Button type="button" onClick={save} disabled={capture.isPending}>
              {capture.isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : saveTarget === "computer" ? (
                <Download className="mr-2 h-4 w-4" />
              ) : (
                <Save className="mr-2 h-4 w-4" />
              )}
              {saveTarget === "computer" ? "Salvar no computador" : "Salvar imagem"}
            </Button>
            <Button type="button" variant="outline" onClick={retake} disabled={capture.isPending}>
              <RefreshCw className="mr-2 h-4 w-4" /> Tirar novamente
            </Button>
          </>
        ) : (
          <>
            <Button
              type="button"
              onClick={takePhoto}
              disabled={starting || !streaming || recording}
            >
              <Camera className="mr-2 h-4 w-4" /> Capturar foto
            </Button>
            {recording ? (
              <Button type="button" variant="destructive" onClick={stopRecording}>
                <Square className="mr-2 h-4 w-4" /> Parar gravação
              </Button>
            ) : (
              <Button
                type="button"
                variant="outline"
                onClick={startRecording}
                disabled={starting || !streaming}
              >
                <Circle className="mr-2 h-4 w-4 fill-red-500 text-red-500" /> Gravar vídeo
              </Button>
            )}
          </>
        )}
      </div>
      {!preview && (
        <p className="text-xs text-muted-foreground">
          Foco e iluminação são ajustados no próprio aparelho (rolo e roda dos LEDs). Vídeos são
          salvos no computador, na pasta Downloads.
        </p>
      )}
    </div>
  );
}
