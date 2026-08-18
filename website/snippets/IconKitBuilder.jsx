export const IconKitBuilder = () => {
  const WORKER_URL = "https://yumebox-iconkit.yumeyuka.moe";
  // Mintlify snippets support one directly imported component per file.
  const densities = [
    { name: "mdpi", size: 48, foreground: 108 },
    { name: "hdpi", size: 72, foreground: 162 },
    { name: "xhdpi", size: 96, foreground: 216 },
    { name: "xxhdpi", size: 144, foreground: 324 },
    { name: "xxxhdpi", size: 192, foreground: 432 },
  ];

  function clip(context, size, shape) {
    context.beginPath();
    if (shape === "circle")
      context.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
    else if (shape === "rounded")
      context.roundRect(0, 0, size, size, size * 0.22);
    else context.rect(0, 0, size, size);
    context.clip();
  }
  const [image, setImage] = useState();
  const [zipReady, setZipReady] = useState(
    typeof window !== "undefined" && Boolean(window.JSZip),
  );
  const [color, setColor] = useState("#ffffff");
  const [colorInput, setColorInput] = useState("#ffffff");
  const [shape, setShape] = useState("rounded");
  const [shapeOpen, setShapeOpen] = useState(false);
  const [crop, setCrop] = useState(false);
  const [cropOpen, setCropOpen] = useState(false);
  const [padding, setPadding] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState();
  const canvasRef = useRef(null);
  const fileInputRef = useRef(null);
  const drag = useRef();
  const movedRef = useRef(false);
  const swatches = ["#ffffff", "#f4f1ee", "#d8d1c8", "#242322"];

  useEffect(() => {
    const ready = () => setZipReady(true);
    window.addEventListener("icon-kit-jszip-ready", ready);
    return () => window.removeEventListener("icon-kit-jszip-ready", ready);
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !image) return;
    const context = canvas.getContext("2d");
    if (!context) return;
    const size = 512;
    const available = size * (1 - padding * 2);
    const scale = (crop ? Math.max : Math.min)(
      available / image.naturalWidth,
      available / image.naturalHeight,
    );
    const width = image.naturalWidth * scale * zoom;
    const height = image.naturalHeight * scale * zoom;
    context.clearRect(0, 0, size, size);
    context.save();
    clip(context, size, shape);
    context.fillStyle = color;
    context.fillRect(0, 0, size, size);
    context.drawImage(
      image,
      (size - width) / 2 + offset.x * size,
      (size - height) / 2 + offset.y * size,
      width,
      height,
    );
    context.restore();
  }, [image, color, shape, crop, padding, zoom, offset]);

  function chooseFile(file) {
    if (!file?.type.startsWith("image/"))
      return setError("请选择 PNG、JPG 或 WebP 图片。");
    const url = URL.createObjectURL(file);
    const next = new Image();
    next.onload = () => {
      setImage(next);
      setZoom(1);
      setOffset({ x: 0, y: 0 });
      setError(undefined);
      URL.revokeObjectURL(url);
    };
    next.src = url;
  }

  function draw(source, size, foreground = true, extraPadding = 0) {
    const canvas = document.createElement("canvas");
    canvas.width = size;
    canvas.height = size;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("无法生成 PNG");
    const available = size * (1 - (padding + extraPadding) * 2);
    const scale = (crop ? Math.max : Math.min)(
      available / source.naturalWidth,
      available / source.naturalHeight,
    );
    const width = source.naturalWidth * scale * zoom;
    const height = source.naturalHeight * scale * zoom;
    context.save();
    clip(context, size, shape);
    context.fillStyle = color;
    context.fillRect(0, 0, size, size);
    if (foreground)
      context.drawImage(
        source,
        (size - width) / 2 + offset.x * size,
        (size - height) / 2 + offset.y * size,
        width,
        height,
      );
    context.restore();
    return canvas;
  }

  function blob(canvas) {
    return new Promise((resolve, reject) =>
      canvas.toBlob(
        (value) => (value ? resolve(value) : reject(new Error("无法生成 PNG"))),
        "image/png",
      ),
    );
  }

  async function bundle() {
    if (!image) throw new Error("请先上传一张图标图片。");
    if (!window.JSZip) throw new Error("压缩组件尚未加载，请稍后重试。");
    const zip = new window.JSZip();
    for (const density of densities) {
      zip.file(
        `res/mipmap-${density.name}/ic_launcher.png`,
        await blob(draw(image, density.size)),
      );
      zip.file(
        `res/mipmap-${density.name}/ic_launcher_adaptive_fore.png`,
        await blob(draw(image, density.foreground, true, 0.15)),
      );
      const background = document.createElement("canvas");
      background.width = density.foreground;
      background.height = density.foreground;
      const backgroundContext = background.getContext("2d");
      if (!backgroundContext) throw new Error("无法生成 PNG");
      backgroundContext.fillStyle = color;
      backgroundContext.fillRect(0, 0, background.width, background.height);
      zip.file(
        `res/mipmap-${density.name}/ic_launcher_adaptive_back.png`,
        await blob(background),
      );
    }
    zip.file(
      "res/mipmap-anydpi-v26/ic_launcher.xml",
      '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n  <background android:drawable="@mipmap/ic_launcher_adaptive_back"/>\n  <foreground android:drawable="@mipmap/ic_launcher_adaptive_fore"/>\n</adaptive-icon>',
    );
    zip.file(
      "manifest.json",
      JSON.stringify({ format: "android-asset-studio-launcher-icon-v1" }),
    );
    zip.file("play_store_512.png", await blob(draw(image, 512)));
    zip.file("1024.png", await blob(draw(image, 1024)));
    return zip.generateAsync({
      type: "blob",
      compression: "DEFLATE",
      compressionOptions: { level: 6 },
    });
  }

  async function download() {
    try {
      const url = URL.createObjectURL(await bundle());
      const link = document.createElement("a");
      link.href = url;
      link.download = "YumeBox-IconKit.zip";
      link.click();
      URL.revokeObjectURL(url);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "生成 ZIP 失败。");
    }
  }

  async function submit() {
    try {
      setBusy(true);
      setError(undefined);
      const form = new FormData();
      form.append("bundle", await bundle(), "YumeBox-IconKit-icons.zip");
      const response = await fetch(`${WORKER_URL}/v1/jobs`, {
        method: "POST",
        body: form,
      });
      if (!response.ok) throw new Error(await response.text());
      const job = await response.json();
      if (!job.issueUrl) throw new Error("创建构建 Issue 失败，请重试。");
      window.location.assign(job.issueUrl);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "提交失败，请重试。");
    } finally {
      setBusy(false);
    }
  }

  function pointerDown(event) {
    if (!image) return;
    movedRef.current = false;
    drag.current = {
      x: event.clientX,
      y: event.clientY,
      ox: offset.x,
      oy: offset.y,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  }
  function pointerMove(event) {
    if (!drag.current) return;
    if (
      Math.abs(event.clientX - drag.current.x) > 4 ||
      Math.abs(event.clientY - drag.current.y) > 4
    )
      movedRef.current = true;
    const size = event.currentTarget.getBoundingClientRect().width;
    setOffset({
      x: drag.current.ox + (event.clientX - drag.current.x) / size,
      y: drag.current.oy + (event.clientY - drag.current.y) / size,
    });
  }

  function openFilePicker() {
    if (!movedRef.current) fileInputRef.current?.click();
    movedRef.current = false;
  }

  function changeZoom(value) {
    const next = Number(value);
    if (Number.isFinite(next)) setZoom(Math.max(0.5, Math.min(3, next / 100)));
  }

  function changePadding(value) {
    const next = Number(value);
    if (Number.isFinite(next)) setPadding(Math.max(0, Math.min(35, next)) / 100);
  }

  function updateColor(value) {
    const next = value.startsWith("#") ? value : `#${value}`;
    if (!/^#[0-9a-f]{0,6}$/i.test(next)) return;
    setColorInput(next);
    if (/^#[0-9a-f]{6}$/i.test(next)) setColor(next.toLowerCase());
  }

  const buildLabel = busy
    ? "创建中..."
    : "创建并打开构建 Issue";
  const positiveStatus = busy ? "正在保存图标包并打开 GitHub Issue。" : undefined;
  return (
    <div className="not-prose my-6 w-full text-zinc-950 dark:text-white">
      <div className="grid grid-cols-1 items-start gap-8 md:grid-cols-2">
        <div className="min-w-0">
          <div
            className={`relative aspect-square w-full max-w-sm touch-none overflow-hidden rounded-xl border border-dashed border-zinc-950/25 bg-zinc-50 dark:border-white/25 dark:bg-white/5 ${image ? "cursor-grab active:cursor-grabbing" : ""}`}
            onPointerDown={pointerDown}
            onPointerMove={pointerMove}
            onPointerUp={() => {
              drag.current = undefined;
            }}
            onPointerCancel={() => {
              drag.current = undefined;
              movedRef.current = false;
            }}
            onClick={openFilePicker}
          >
            {image ? (
              <canvas ref={canvasRef} width={512} height={512} className="block h-full w-full" />
            ) : (
              <div className="flex h-full w-full cursor-pointer flex-col items-center justify-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
                <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" x2="12" y1="3" y2="15" />
                </svg>
                <strong className="font-medium text-zinc-700 dark:text-zinc-200">上传图标</strong>
                <span className="text-xs">PNG、JPG 或 WebP</span>
              </div>
            )}
            <input ref={fileInputRef} className="sr-only" type="file" accept="image/png,image/jpeg,image/webp" onClick={(event) => event.stopPropagation()} onChange={(event) => { chooseFile(event.target.files?.[0]); event.target.value = ""; }} />
          </div>
        </div>
        <div className="min-w-0 space-y-5">
          <div className="space-y-2">
            <p className="m-0 text-sm font-medium">背景色</p>
            <div className="flex items-center gap-2">
              {swatches.map((swatch) => <button type="button" key={swatch} aria-label={`背景色 ${swatch}`} onClick={() => { setColor(swatch); setColorInput(swatch); }} style={{ backgroundColor: swatch }} className={`h-7 w-7 shrink-0 rounded-md border border-zinc-950/20 ${color === swatch ? "ring-2 ring-zinc-950 ring-offset-2 dark:ring-white dark:ring-offset-zinc-950" : ""}`} />)}
              <input value={colorInput} maxLength={7} onChange={(event) => updateColor(event.target.value)} onBlur={() => { if (!/^#[0-9a-f]{6}$/i.test(colorInput)) setColorInput(color); }} className="h-9 min-w-0 flex-1 rounded-lg border border-zinc-950/20 bg-transparent px-3 font-mono text-sm outline-none dark:border-white/20" />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 border-t border-zinc-950/10 pt-5 dark:border-white/15 sm:grid-cols-2">
            <div className="min-w-0 space-y-2">
              <p className="m-0 text-sm font-medium">形状</p>
              <div className="relative">
                <button type="button" aria-haspopup="listbox" aria-expanded={shapeOpen} onClick={() => setShapeOpen(!shapeOpen)} className="flex h-10 w-full items-center justify-between rounded-lg border border-zinc-950/15 bg-white px-3 text-sm shadow-sm transition hover:border-zinc-950/35 dark:border-white/20 dark:bg-zinc-900">
                  <span>{shape === "square" ? "方形" : shape === "circle" ? "圆形" : "圆角"}</span>
                  <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={`lucide lucide-chevron-down h-4 w-4 text-zinc-500 transition-transform duration-200 ${shapeOpen ? "rotate-180" : ""}`}><path d="m6 9 6 6 6-6" /></svg>
                </button>
                {shapeOpen && <><div className="fixed inset-0 z-10" onClick={() => setShapeOpen(false)} /><div role="listbox" className="absolute z-20 mt-1 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white p-1 shadow-lg dark:border-white/20 dark:bg-zinc-900">
                  {[{ value: "square", label: "方形" }, { value: "rounded", label: "圆角" }, { value: "circle", label: "圆形" }].map((option) => <button type="button" role="option" aria-selected={option.value === shape} key={option.value} onClick={() => { setShape(option.value); setShapeOpen(false); }} className={`flex min-h-9 w-full items-center justify-between rounded-md px-2.5 text-left text-sm transition ${option.value === shape ? "bg-zinc-100 font-medium dark:bg-white/15" : "hover:bg-zinc-50 dark:hover:bg-white/5"}`}><span>{option.label}</span>{option.value === shape ? <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-check h-4 w-4"><path d="M20 6 9 17l-5-5" /></svg> : <span className="h-4 w-4" />}</button>)}
                </div></>}
              </div>
            </div>
            <div className="min-w-0 space-y-2">
              <p className="m-0 text-sm font-medium">裁剪方式</p>
              <div className="relative">
                <button type="button" aria-haspopup="listbox" aria-expanded={cropOpen} onClick={() => setCropOpen(!cropOpen)} className="flex h-10 w-full items-center justify-between rounded-lg border border-zinc-950/15 bg-white px-3 text-sm shadow-sm transition hover:border-zinc-950/35 dark:border-white/20 dark:bg-zinc-900">
                  <span>{crop ? "填充裁剪" : "完整显示"}</span>
                  <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={`lucide lucide-chevron-down h-4 w-4 text-zinc-500 transition-transform duration-200 ${cropOpen ? "rotate-180" : ""}`}><path d="m6 9 6 6 6-6" /></svg>
                </button>
                {cropOpen && <><div className="fixed inset-0 z-10" onClick={() => setCropOpen(false)} /><div role="listbox" className="absolute z-20 mt-1 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white p-1 shadow-lg dark:border-white/20 dark:bg-zinc-900">
                  {[{ value: false, label: "完整显示" }, { value: true, label: "填充裁剪" }].map((option) => <button type="button" role="option" aria-selected={option.value === crop} key={option.label} onClick={() => { setCrop(option.value); setCropOpen(false); }} className={`flex min-h-9 w-full items-center justify-between rounded-md px-2.5 text-left text-sm transition ${option.value === crop ? "bg-zinc-100 font-medium dark:bg-white/15" : "hover:bg-zinc-50 dark:hover:bg-white/5"}`}><span>{option.label}</span>{option.value === crop ? <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-check h-4 w-4"><path d="M20 6 9 17l-5-5" /></svg> : <span className="h-4 w-4" />}</button>)}
                </div></>}
              </div>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 border-t border-zinc-950/10 pt-5 dark:border-white/15">
            <div className="min-w-0 space-y-2"><p className="m-0 text-sm font-medium">缩放</p><div className="flex h-10 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white shadow-sm dark:border-white/20 dark:bg-zinc-900"><button type="button" aria-label="减少缩放" onClick={() => changeZoom(Math.round(zoom * 100) - 10)} className="flex h-full w-10 shrink-0 items-center justify-center border-r border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-minus"><path d="M5 12h14" /></svg></button><input value={`${Math.round(zoom * 100)}%`} inputMode="numeric" aria-label="缩放百分比" onChange={(event) => changeZoom(event.target.value.replace("%", ""))} className="h-full min-w-0 flex-1 border-0 bg-transparent px-1 text-center text-sm font-medium outline-none" /><button type="button" aria-label="增加缩放" onClick={() => changeZoom(Math.round(zoom * 100) + 10)} className="flex h-full w-10 shrink-0 items-center justify-center border-l border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-plus"><path d="M5 12h14" /><path d="M12 5v14" /></svg></button></div></div>
            <div className="min-w-0 space-y-2"><p className="m-0 text-sm font-medium">留白</p><div className="flex h-10 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white shadow-sm dark:border-white/20 dark:bg-zinc-900"><button type="button" aria-label="减少留白" onClick={() => changePadding(Math.round(padding * 100) - 5)} className="flex h-full w-10 shrink-0 items-center justify-center border-r border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-minus"><path d="M5 12h14" /></svg></button><input value={`${Math.round(padding * 100)}%`} inputMode="numeric" aria-label="留白百分比" onChange={(event) => changePadding(event.target.value.replace("%", ""))} className="h-full min-w-0 flex-1 border-0 bg-transparent px-1 text-center text-sm font-medium outline-none" /><button type="button" aria-label="增加留白" onClick={() => changePadding(Math.round(padding * 100) + 5)} className="flex h-full w-10 shrink-0 items-center justify-center border-l border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-plus"><path d="M5 12h14" /><path d="M12 5v14" /></svg></button></div></div>
          </div>
          <div className="border-t border-zinc-950/10 pt-5 dark:border-white/15">
            <button type="button" disabled={!image || busy || !zipReady} onClick={submit} className="h-10 w-full rounded-lg bg-zinc-950 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-40 dark:bg-white dark:text-zinc-950">{buildLabel}</button>
          </div>
          {positiveStatus && <div role="status" aria-live="polite" className="flex items-center gap-2 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-600 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-400"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={`shrink-0 ${busy ? "animate-spin" : ""}`}><path d={busy ? "M21 12a9 9 0 1 1-6.219-8.56" : "M20 6 9 17l-5-5"} /></svg><span>{positiveStatus}</span></div>}
          {error && <div role="alert" className="rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-600 dark:border-red-500/30 dark:bg-red-500/15 dark:text-red-400">{error}</div>}
        </div>
      </div>
    </div>
  );
};
