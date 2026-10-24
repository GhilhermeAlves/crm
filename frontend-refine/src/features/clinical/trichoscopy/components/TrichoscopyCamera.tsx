"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { Camera, Loader2, RefreshCw, Save } from "lucide-react";
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
import { useCaptureTrichoscopy } from "../hooks/useTrichoscopy";

type Props = {
  companyId: string;
  contactId: string;
  onSaved?: () => void;
};

const VIDEO_SIZE = { width: { ideal: 1280 }, height: { ideal: 720 } };

/**
 * Captura pelo tricoscópio (câmera USB/UVC). A câmera liga ao montar e é
 * desligada ao desmontar, por isso o componente só é renderizado com o
 * diálogo de captura aberto.
 */
export function TrichoscopyCamera({ companyId, contactId, onSaved }: Props) {
  const uid = useId();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const previewRef = useRef<string | null>(null);

  const [devices, setDevices] = useState<MediaDeviceInfo[]>([]);
  const [deviceId, setDeviceId] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [starting, setStarting] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [blob, setBlob] = useState<Blob | null>(null);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);

  const capture = useCaptureTrichoscopy(companyId, contactId);

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
        const preferred = inputs.find((d) => /usb|microscope|scope|derm/i.test(d.label));
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

  useEffect(() => {
    void loadDevices();
    return () => {
      stopStream();
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    };
  }, [loadDevices, stopStream]);

  useEffect(() => {
    if (!preview) void startStream(deviceId);
    // Só religa ao trocar de câmera; voltar da prévia chama startStream direto.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceId]);

  const takePhoto = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    canvas.getContext("2d")?.drawImage(video, 0, 0, canvas.width, canvas.height);
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
  };

  const retake = () => {
    clearPreview();
    void startStream(deviceId);
  };

  const save = () => {
    if (!blob) return;
    capture.mutate(
      { file: blob, notes: notes.trim() || undefined },
      {
        onSuccess: () => {
          clearPreview();
          setNotes("");
          onSaved?.();
        },
      },
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-2">
        <div className="min-w-56 flex-1 space-y-1">
          <Label htmlFor={`${uid}-device`}>Câmera / tricoscópio</Label>
          <Select value={deviceId} onValueChange={setDeviceId} disabled={devices.length === 0}>
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
        <Button type="button" variant="outline" onClick={() => void loadDevices()}>
          <RefreshCw className="mr-2 h-4 w-4" /> Atualizar dispositivos
        </Button>
      </div>

      <div className="relative aspect-video w-full overflow-hidden rounded-lg bg-black">
        {preview ? (
          // eslint-disable-next-line @next/next/no-img-element -- prévia local (blob:)
          <img
            src={preview}
            alt="Prévia da captura tricoscópica"
            className="h-full w-full object-contain"
          />
        ) : (
          <video ref={videoRef} className="h-full w-full object-contain" playsInline muted />
        )}
        {starting && (
          <div className="absolute inset-0 flex items-center justify-center text-white">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        )}
      </div>
      <canvas ref={canvasRef} className="hidden" />

      {error && <p className="text-sm text-destructive">{error}</p>}

      {preview && (
        <div className="space-y-1">
          <Label htmlFor={`${uid}-notes`}>Observações (opcional)</Label>
          <Textarea
            id={`${uid}-notes`}
            value={notes}
            maxLength={2000}
            placeholder="Região, achados, comparação com exames anteriores…"
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
              ) : (
                <Save className="mr-2 h-4 w-4" />
              )}
              Salvar imagem
            </Button>
            <Button type="button" variant="outline" onClick={retake} disabled={capture.isPending}>
              <RefreshCw className="mr-2 h-4 w-4" /> Tirar novamente
            </Button>
          </>
        ) : (
          <Button type="button" onClick={takePhoto} disabled={starting || !streaming}>
            <Camera className="mr-2 h-4 w-4" /> Capturar foto
          </Button>
        )}
      </div>
    </div>
  );
}
