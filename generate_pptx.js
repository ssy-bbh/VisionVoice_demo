const pptxgen = require("pptxgenjs");
const fs = require("fs");
const path = require("path");
\nconst pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13.33 x 7.5 inches\npres.author = "Song Yaobohan";\npres.title = "VisionVoice";\n\n// ─── Color Palette ───
const C = {\ndarkBg:   "0A1628",\nnavy:     "0D3B66",\nteal:     "00B4D8",\ngreen:    "00E5A0",\naccent:   "F77F00",\nlightBg:  "F8FAFC",\ncardBg:   "FFFFFF",\ntext:     "1E293B",\nsubtext:  "64748B",\nwhite:    "FFFFFF",\ndivider:  "E2E8F0",\nlightTeal:"E0F7FA",\nlightNavy:"EBF0F7",\nred:      "EF4444",\norange:   "F59E0B",\npurple:   "8B5CF6",\n};\n\nconst PIC = "C:\\Users\\Dell\\Desktop\\Hayden\\毕设文件夹\\ca2_en\\pic";\n\n// ─── Helpers ───
function addFooter(slide, num, total) {\nslide.addText(`${num} / ${total}`, {\nx: 11.5, y: 7.0, w: 1.5, h: 0.35,\nfontSize: 9, color: C.subtext, align: "right", fontFace: "Arial",\n});
}\n\nfunction addTopBar(slide, color) {\nslide.addShape(pres.ShapeType.rect, {\nx: 0, y: 0, w: 13.33, h: 0.06, fill: { color: color || C.teal },\n});
}\n\nfunction addChapterBadge(slide, text) {\nslide.addShape(pres.ShapeType.roundRect, {\nx: 0.5, y: 0.2, w: 3.8, h: 0.45,\nrectRadius: 0.08, fill: { color: C.teal },\nshadow: { type: "outer", blur: 4, offset: 1, color: "000000", opacity: 0.15 },\n});
  slide.addText(text, {\nx: 0.5, y: 0.2, w: 3.8, h: 0.45,\nfontSize: 11, fontFace: "Arial", bold: true, color: C.white,\nalign: "center", valign: "middle",\n});
}\n\nfunction addCard(slide, x, y, w, h, opts) {\nslide.addShape(pres.ShapeType.roundRect, {\nx, y, w, h, rectRadius: 0.12,\nfill: { color: opts && opts.bg || C.cardBg },\nshadow: { type: "outer", blur: 6, offset: 2, color: "000000", opacity: 0.08 },\nline: opts && opts.border ? { color: opts.border, width: 1.5 } : undefined,\n});
}\n\nfunction flowArrow(slide, x, y, label, color) {\nslide.addShape(pres.ShapeType.roundRect, {\nx, y, w: 2.1, h: 1.2, rectRadius: 0.1, fill: { color: color || C.teal },\n});
  slide.addText(label, {\nx, y, w: 2.1, h: 1.2,\nfontSize: 12, fontFace: "Arial", bold: true, color: C.white,\nalign: "center", valign: "middle", lineSpacingMultiple: 1.2,\n});
}\n\nconst TOTAL = 24;\n\n// ═══════════════════════════════════════════════
// SLIDE 1 — Title
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.darkBg };\ns.addShape(pres.ShapeType.rect, { x: 0, y: 0, w: 13.33, h: 0.08, fill: { color: C.teal } });
  s.addShape(pres.ShapeType.rect, { x: 0, y: 7.42, w: 13.33, h: 0.08, fill: { color: C.green } });
  s.addShape(pres.ShapeType.ellipse, { x: 10.5, y: 0.5, w: 3.5, h: 3.5, fill: { color: "0D3B66" }, line: { color: C.teal, width: 1.5, dashType: "dash" } });
  s.addShape(pres.ShapeType.ellipse, { x: -1.0, y: 5.0, w: 3.0, h: 3.0, fill: { color: "0D3B66" }, line: { color: C.green, width: 1, dashType: "dash" } });
  s.addText("VisionVoice", { x: 1.0, y: 1.5, w: 11.3, h: 1.6, fontSize: 52, fontFace: "Arial", bold: true, color: C.white, align: "center" });
  s.addText("An English Assisted Learning Application Based on\nYOLO On-Device Object Detection and Wav2Vec2 Phoneme Feedback", {\nx: 1.5, y: 3.2, w: 10.3, h: 1.2, fontSize: 18, fontFace: "Arial", color: C.teal, align: "center", lineSpacingMultiple: 1.3,\n});
  s.addShape(pres.ShapeType.rect, { x: 4.5, y: 4.6, w: 4.3, h: 0.04, fill: { color: C.teal } });
  s.addText("Song Yaobohan", { x: 1.0, y: 4.9, w: 11.3, h: 0.5, fontSize: 20, fontFace: "Arial", color: C.white, align: "center" });
  s.addText("Wuhan University  |  Supervisors: Prof. Rajesh  &  Prof. Wu Xiaoping", {\nx: 1.0, y: 5.5, w: 11.3, h: 0.45, fontSize: 14, fontFace: "Arial", color: C.subtext, align: "center",\n});
  s.addText("2026", { x: 1.0, y: 6.2, w: 11.3, h: 0.4, fontSize: 13, fontFace: "Arial", color: "475569", align: "center" });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 2 — Contents
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s);
  addFooter(s, 2, TOTAL);
  s.addText("Contents", { x: 0.6, y: 0.35, w: 5, h: 0.6, fontSize: 28, fontFace: "Arial", bold: true, color: C.navy });
  const chapters = [\n{ num: "01", title: "Introduction", desc: "Research Background & Motivation", color: C.teal },\n{ num: "02", title: "Related Technologies", desc: "YOLO, Wav2Vec2, AR", color: C.navy },\n{ num: "03", title: "System Design", desc: "Requirements & Architecture", color: C.purple },\n{ num: "04", title: "Core Algorithms", desc: "Detection · Speech · Rendering", color: C.accent },\n{ num: "05", title: "Experiments", desc: "Performance & Evaluation", color: C.green },\n{ num: "06", title: "Conclusion", desc: "Innovations & Future Work", color: C.red },\n];\nchapters.forEach((ch, i) => {\nconst col = i % 3, row = Math.floor(i / 3);
    const x = 0.6 + col * 4.15, y = 1.3 + row * 2.8;\naddCard(s, x, y, 3.85, 2.3, { border: ch.color });
    s.addShape(pres.ShapeType.roundRect, { x: x + 0.25, y: y + 0.25, w: 0.7, h: 0.7, rectRadius: 0.12, fill: { color: ch.color } });
    s.addText(ch.num, { x: x + 0.25, y: y + 0.25, w: 0.7, h: 0.7, fontSize: 20, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
    s.addText(ch.title, { x: x + 1.15, y: y + 0.3, w: 2.5, h: 0.55, fontSize: 17, fontFace: "Arial", bold: true, color: C.text, valign: "middle" });
    s.addText(ch.desc, { x: x + 0.25, y: y + 1.15, w: 3.4, h: 0.9, fontSize: 12, fontFace: "Arial", color: C.subtext, lineSpacingMultiple: 1.2 });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 3 — Research Background
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s);
  addChapterBadge(s, "CHAPTER 1 · INTRODUCTION");
  addFooter(s, 3, TOTAL);
  s.addText("Research Background & Motivation", { x: 0.6, y: 0.85, w: 8, h: 0.55, fontSize: 24, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 5.8, 5.2);
  s.addText("Two Core Pain Points", { x: 0.8, y: 1.8, w: 5.2, h: 0.45, fontSize: 16, fontFace: "Arial", bold: true, color: C.red });
  [\n{ icon: "🔒", title: "Detachment from Context", desc: "Traditional tools are decontextualized — learners must shift attention away from the physical object, breaking immersion and memory encoding." },\n{ icon: "🔇", title: "Coarse-Grained Feedback", desc: "Translation software provides only flat text, lacking phoneme-level pronunciation correction or multimodal associations." },\n].forEach((p, i) => {\nconst yy = 2.5 + i * 2.1;\ns.addShape(pres.ShapeType.roundRect, { x: 0.9, y: yy, w: 5.0, h: 1.7, rectRadius: 0.1, fill: { color: "FEF2F2" }, line: { color: C.red, width: 1 } });
    s.addText(p.icon + "  " + p.title, { x: 1.1, y: yy + 0.15, w: 4.6, h: 0.4, fontSize: 14, fontFace: "Arial", bold: true, color: C.text });
    s.addText(p.desc, { x: 1.1, y: yy + 0.6, w: 4.6, h: 0.95, fontSize: 11, fontFace: "Arial", color: C.subtext, lineSpacingMultiple: 1.25 });
  });
\naddCard(s, 6.7, 1.6, 6.0, 5.2);
  s.addText("VisionVoice Solution", { x: 7.0, y: 1.8, w: 5.4, h: 0.45, fontSize: 16, fontFace: "Arial", bold: true, color: C.teal });
  [\n{ icon: "📸", title: "AR Object Detection", desc: "YOLOv8n identifies real-world objects in real-time, anchoring vocabulary to physical context." },\n{ icon: "🗣️", title: "Phoneme-Level Feedback", desc: "Wav2Vec2 decodes user pronunciation at phoneme granularity, enabling precise correction." },\n{ icon: "📶", title: "Fully Offline", desc: "Both visual and auditory inference run on-device — zero network dependency, full privacy." },\n].forEach((p, i) => {\nconst yy = 2.5 + i * 1.45;\ns.addShape(pres.ShapeType.roundRect, { x: 7.1, y: yy, w: 5.2, h: 1.2, rectRadius: 0.1, fill: { color: C.lightTeal }, line: { color: C.teal, width: 1 } });
    s.addText(p.icon + "  " + p.title, { x: 7.3, y: yy + 0.1, w: 4.8, h: 0.35, fontSize: 13, fontFace: "Arial", bold: true, color: C.navy });
    s.addText(p.desc, { x: 7.3, y: yy + 0.5, w: 4.8, h: 0.6, fontSize: 10.5, fontFace: "Arial", color: C.subtext, lineSpacingMultiple: 1.2 });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 4 — Theoretical Foundation
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s);
  addChapterBadge(s, "CHAPTER 1 · INTRODUCTION");
  addFooter(s, 4, TOTAL);
  s.addText("Theoretical Foundation: Multisensory Closed-Loop", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 24, fontFace: "Arial", bold: true, color: C.navy });
\n[\n{ title: "Visual Channel", color: "3B82F6", steps: ["Real-World Object", "→ YOLO Detection", "→ BBox + Label"] },\n{ title: "Semantic Channel", color: "22C55E", steps: ["IPA Transcription", "→ CMU Dict Lookup", "→ Example Sentences"] },\n{ title: "Auditory Channel", color: C.accent, steps: ["User Pronunciation", "→ Wav2Vec2 ASR", "→ ARPAbet Phonemes"] },\n].forEach((ch, i) => {\nconst x = 0.5 + i * 4.2;\naddCard(s, x, 1.6, 3.8, 4.8, { border: ch.color });
    s.addShape(pres.ShapeType.roundRect, { x: x + 0.2, y: 1.8, w: 3.4, h: 0.5, rectRadius: 0.08, fill: { color: ch.color } });
    s.addText(ch.title, { x: x + 0.2, y: 1.8, w: 3.4, h: 0.5, fontSize: 14, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
    ch.steps.forEach((st, j) => {\ns.addText(st, { x: x + 0.4, y: 2.6 + j * 0.55, w: 3.0, h: 0.45, fontSize: 12, fontFace: "Arial", color: C.text });
    });
  });
\ns.addShape(pres.ShapeType.roundRect, { x: 4.2, y: 4.2, w: 4.9, h: 1.0, rectRadius: 0.15, fill: { color: C.navy }, shadow: { type: "outer", blur: 8, offset: 2, color: "000000", opacity: 0.2 } });
  s.addText("VisionVoice System\nContext → Cognition → Pronunciation", { x: 4.2, y: 4.2, w: 4.9, h: 1.0, fontSize: 14, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.2 });
  s.addText("\"When visual and auditory stimuli are synchronously activated, they stimulate deeper meaning construction than single-modal input.\"\n— Mayer, Multimedia Learning Theory", {\nx: 0.6, y: 6.5, w: 12, h: 0.7, fontSize: 10.5, fontFace: "Arial", italic: true, color: C.subtext, align: "center", lineSpacingMultiple: 1.2,\n});
}\n\n// ═══════════════════════════════════════════════
// SLIDE 5 — System Architecture
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s);
  addChapterBadge(s, "CHAPTER 3 · SYSTEM DESIGN");
  addFooter(s, 5, TOTAL);
  s.addText("Four-Tier Layered Architecture", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 24, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ name: "Presentation Layer", desc: "AR Camera · Gyroscope Parallax · Laser Shader · Learning Dashboard", color: C.teal, y: 1.6 },\n{ name: "Application Logic Layer", desc: "Object Detection Pipeline · Pronunciation Assessment · NW Alignment · State Machine", color: C.navy, y: 2.95 },\n{ name: "Inference Engine Layer", desc: "TFLite (YOLOv8n) · ONNX Runtime (Wav2Vec2) · INT8 Quantized Models", color: C.purple, y: 4.3 },\n{ name: "Data & Sensor Layer", desc: "Room ORM · CMU Dict · CameraX · MEMS Gyroscope · Microphone", color: C.accent, y: 5.65 },\n].forEach((l) => {\ns.addShape(pres.ShapeType.roundRect, { x: 0.6, y: l.y, w: 12.1, h: 1.15, rectRadius: 0.1, fill: { color: l.color }, shadow: { type: "outer", blur: 4, offset: 2, color: "000000", opacity: 0.15 } });
    s.addText(l.name, { x: 0.9, y: l.y + 0.1, w: 3.5, h: 0.45, fontSize: 16, fontFace: "Arial", bold: true, color: C.white });
    s.addText(l.desc, { x: 0.9, y: l.y + 0.55, w: 11.5, h: 0.45, fontSize: 11.5, fontFace: "Arial", color: "E0E7FF" });
  });
  [2.75, 4.1, 5.45].forEach(yy => s.addText("▼", { x: 6.0, y: yy, w: 1.3, h: 0.3, fontSize: 16, color: C.subtext, align: "center" }));
}\n\n// ═══════════════════════════════════════════════
// SLIDE 6 — Chapter 4 Divider
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.darkBg };\ns.addShape(pres.ShapeType.rect, { x: 0, y: 0, w: 13.33, h: 0.08, fill: { color: C.accent } });
  s.addText("04", { x: 0.6, y: 1.0, w: 3, h: 1.5, fontSize: 72, fontFace: "Arial", bold: true, color: C.accent });
  s.addShape(pres.ShapeType.rect, { x: 0.6, y: 2.6, w: 3.0, h: 0.05, fill: { color: C.accent } });
  s.addText("Core Algorithm\nImplementation &\nOn-Device Deployment", { x: 0.6, y: 2.9, w: 6, h: 2.2, fontSize: 30, fontFace: "Arial", bold: true, color: C.white, lineSpacingMultiple: 1.2 });
  [\n{ title: "Visual Perception", items: "YOLO · INT8 · Zero-GC · O(N) Parse", color: C.teal },\n{ title: "Speech Evaluation", items: "Wav2Vec2 · NW Alignment · VAD · CMU Pruning", color: C.green },\n{ title: "AR Rendering", items: "Shader · PorterDuff · Gyroscope Parallax", color: C.purple },\n].forEach((m, i) => {\ns.addShape(pres.ShapeType.roundRect, { x: 7.5, y: 1.8 + i * 1.7, w: 5.2, h: 1.35, rectRadius: 0.12, fill: { color: "111D30" }, line: { color: m.color, width: 1.5 } });
    s.addText(m.title, { x: 7.8, y: 1.9 + i * 1.7, w: 4.6, h: 0.5, fontSize: 16, fontFace: "Arial", bold: true, color: m.color });
    s.addText(m.items, { x: 7.8, y: 2.4 + i * 1.7, w: 4.6, h: 0.5, fontSize: 11, fontFace: "Arial", color: C.subtext });
  });
  addFooter(s, 6, TOTAL);
}\n\n// ═══════════════════════════════════════════════
// SLIDE 7 — YOLOv8n: Dataset & Fine-Tuning
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CHAPTER 4 · VISUAL PERCEPTION");
  addFooter(s, 7, TOTAL);
  s.addText("YOLOv8n: Dataset Selection & Fine-Tuning", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 5.9, 2.8, { border: C.red });
  s.addShape(pres.ShapeType.roundRect, { x: 0.7, y: 1.75, w: 1.8, h: 0.4, rectRadius: 0.06, fill: { color: C.red } });
  s.addText("❌ LVIS Failure", { x: 0.7, y: 1.75, w: 1.8, h: 0.4, fontSize: 11, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
  s.addText("1,203 categories → extreme long-tail distribution\nLow-frequency classes: nearly zero recall on mobile\nFP32 model: 22.4 MB — too large for edge\nFeature dilution: too many irrelevant classes dilute common object features", {\nx: 0.9, y: 2.3, w: 5.2, h: 1.9, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.5, bullet: { type: "bullet" },\n});
\naddCard(s, 6.8, 1.6, 5.9, 2.8, { border: C.green });
  s.addShape(pres.ShapeType.roundRect, { x: 7.0, y: 1.75, w: 2.0, h: 0.4, rectRadius: 0.06, fill: { color: C.green } });
  s.addText("✅ COCO 40-Class", { x: 7.0, y: 1.75, w: 2.0, h: 0.4, fontSize: 11, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
  s.addText("40 common educational objects (cup, book, phone...)\nBalanced distribution: each class ≥ 1,200 images\nINT8 quantized: only 6.3 MB\nFine-tuned recall: 92.1% (standard lighting)", {\nx: 7.2, y: 2.3, w: 5.2, h: 1.9, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.5, bullet: { type: "bullet" },\n});
\naddCard(s, 0.5, 4.7, 12.2, 2.3);
  s.addText("Fine-Tuning Strategy", { x: 0.8, y: 4.85, w: 4, h: 0.4, fontSize: 15, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ t: "COCO 80\n→ 40 Classes", c: C.teal },\n{ t: "Transfer\nLearning", c: C.navy },\n{ t: "INT8\nQuantization", c: C.purple },\n{ t: "6.3 MB\nFinal Model", c: C.green },\n].forEach((st, i) => {\nconst xx = 0.9 + i * 3.0;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.4, w: 2.2, h: 1.2, rectRadius: 0.1, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 5.4, w: 2.2, h: 1.2, fontSize: 13, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.2 });
    if (i < 3) s.addText("→", { x: xx + 2.2, y: 5.55, w: 0.8, h: 0.9, fontSize: 24, color: C.subtext, align: "center", valign: "middle" });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 8 — INT8 Quantization
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CHAPTER 4 · VISUAL PERCEPTION");
  addFooter(s, 8, TOTAL);
  s.addText("INT8 Quantization: From FP32 to 6.3 MB", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 6.2, 3.0);
  s.addText("Affine Quantization Formula", { x: 0.8, y: 1.75, w: 5.5, h: 0.4, fontSize: 15, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("q = clamp( round( x / S ) + Z,  -128, 127 )", { x: 0.8, y: 2.3, w: 5.8, h: 0.6, fontSize: 18, fontFace: "Courier New", bold: true, color: C.teal, align: "center" });
  s.addText("S = (xmax - xmin) / (qmax - qmin)\nZ = round(qmin - xmin / S)\n\nDequantization:  x̂ = S × (q - Z)", { x: 0.8, y: 3.0, w: 5.8, h: 1.5, fontSize: 12, fontFace: "Courier New", color: C.text, lineSpacingMultiple: 1.4 });
\naddCard(s, 7.0, 1.6, 5.7, 3.0);
  s.addText("Model Compression Results", { x: 7.3, y: 1.75, w: 5.0, h: 0.4, fontSize: 15, fontFace: "Arial", bold: true, color: C.navy });
  s.addTable([\n[{ text: "Metric", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "FP32", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "INT8", options: { bold: true, color: C.white, fill: { color: C.navy } } }],\n[{ text: "Model Size" }, { text: "22.4 MB" }, { text: "6.3 MB", options: { color: C.green, bold: true } }],\n[{ text: "Inference" }, { text: "~120 ms" }, { text: "< 45 ms", options: { color: C.green, bold: true } }],\n[{ text: "Accuracy" }, { text: "89.5%" }, { text: "92.1%*", options: { color: C.teal, bold: true } }],\n[{ text: "Memory BW" }, { text: "4× higher" }, { text: "1× (baseline)", options: { color: C.green, bold: true } }],\n], { x: 7.2, y: 2.3, w: 5.3, fontSize: 11, fontFace: "Arial", color: C.text, border: { type: "solid", pt: 0.5, color: C.divider }, colW: [1.6, 1.5, 1.8], rowH: [0.4, 0.4, 0.4, 0.4, 0.4], autoPage: false });
  s.addText("*After domain fine-tuning on educational objects", { x: 7.2, y: 4.35, w: 5.0, h: 0.25, fontSize: 9, fontFace: "Arial", italic: true, color: C.subtext });
\naddCard(s, 0.5, 4.9, 12.2, 2.1);
  s.addText("Key Insight", { x: 0.8, y: 5.05, w: 3, h: 0.4, fontSize: 15, fontFace: "Arial", bold: true, color: C.accent });
  s.addText("INT8 quantization maps FP32 weights to 8-bit integers using scale (S) and zero-point (Z). This reduces model size by 4× and cuts memory bandwidth — the primary bottleneck on mobile SoCs. Fine-tuning after quantization recovers and exceeds original accuracy by focusing on the 40-class educational domain.", { x: 0.8, y: 5.5, w: 11.5, h: 1.3, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 9 — Zero-GC Pipeline
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CHAPTER 4 · VISUAL PERCEPTION");
  addFooter(s, 9, TOTAL);
  s.addText("Zero-GC Memory Pipeline", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 6.0, 2.6, { border: C.red });
  s.addText("Traditional: On-Heap Allocation", { x: 0.8, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.red });
  s.addText("new BoundingBox() per frame → JVM heap\n→ GC triggered when heap full → UI frame drops\n→ Sawtooth memory curve (88 MB ↔ 15 MB)\n→ Frequent GC pauses destroy AR smoothness", { x: 0.8, y: 2.2, w: 5.5, h: 1.8, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.5 });
\naddCard(s, 6.8, 1.6, 6.0, 2.6, { border: C.green });
  s.addText("VisionVoice: Zero-GC Off-Heap", { x: 7.1, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.green });
  s.addText("ByteBuffer.allocateDirect(4.9 MB)\n→ Pre-allocated off-heap memory pool\n→ Overwrite memory offsets per frame\n→ No object allocation → No GC pauses\n→ Flat memory ~14 MB, stable ≥ 30 FPS", { x: 7.1, y: 2.2, w: 5.5, h: 1.8, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.5 });
\naddCard(s, 0.5, 4.5, 12.2, 2.6);
  s.addText("Zero-GC Inference Pipeline", { x: 0.8, y: 4.65, w: 5, h: 0.4, fontSize: 15, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ t: "CameraX\nFrame", c: C.teal },\n{ t: "Bitmap →\nByteBuffer", c: C.navy },\n{ t: "TFLite\nInference", c: C.purple },\n{ t: "Off-Heap\nParse", c: C.accent },\n{ t: "AR\nRender", c: C.green },\n].forEach((st, i) => {\nconst xx = 0.8 + i * 2.4;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.25, w: 1.8, h: 1.3, rectRadius: 0.1, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 5.25, w: 1.8, h: 1.3, fontSize: 12, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.2 });
    if (i < 4) s.addText("→", { x: xx + 1.8, y: 5.4, w: 0.6, h: 1.0, fontSize: 22, color: C.subtext, align: "center", valign: "middle" });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 10 — O(N) Tensor Parsing
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CHAPTER 4 · VISUAL PERCEPTION");
  addFooter(s, 10, TOTAL);
  s.addText("O(N) Single-Pass Flat Tensor Parsing", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 5.9, 2.2, { border: C.red });
  s.addText("Traditional: O(N×C) Nested Loop", { x: 0.8, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.red });
  s.addText("for each class (84 channels):\n  for each anchor (8400):\n    → 705,600 iterations per frame\n→ BoundingBox object per detection\n→ High GC pressure + slow", { x: 0.8, y: 2.2, w: 5.3, h: 1.4, fontSize: 12, fontFace: "Courier New", color: C.text, lineSpacingMultiple: 1.35 });
\naddCard(s, 6.8, 1.6, 5.9, 2.2, { border: C.green });
  s.addText("VisionVoice: O(N) Flat Access", { x: 7.1, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.green });
  s.addText("Tensor shape: [1, 84, 8400]\nIndex_flat = j × 8400 + i\n→ Direct memory offset mapping\n→ Single pass, zero object creation", { x: 7.1, y: 2.2, w: 5.3, h: 1.4, fontSize: 12, fontFace: "Courier New", color: C.text, lineSpacingMultiple: 1.35 });
\naddCard(s, 0.5, 4.1, 12.2, 1.4);
  s.addText("Flat Index Formula", { x: 0.8, y: 4.2, w: 3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Index_flat = j × 8400 + i   where i = anchor index [0, 8400), j = channel [0, 84)\n  → channels 0–3: x, y, w, h   |   channel 4: confidence   |   channels 5–83: class scores", { x: 0.8, y: 4.6, w: 11.5, h: 0.8, fontSize: 12, fontFace: "Courier New", color: C.teal, lineSpacingMultiple: 1.3 });
\naddCard(s, 0.5, 5.8, 12.2, 1.3);
  s.addText("Post-Processing:  BBox Decode  →  Confidence Filter (θ=0.50)  →  NMS (IoU>0.45)  →  AR Mapping", { x: 0.8, y: 5.95, w: 11.5, h: 0.5, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy, align: "center" });
  s.addText("Table 4.1 (thesis): O(N) single-pass flat tensor parsing - initializes D, filters by S, decodes boxes, applies NMS, returns D", { x: 0.8, y: 6.45, w: 11.5, h: 0.4, fontSize: 11, fontFace: "Arial", color: C.subtext, align: "center" });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 11 — 4.1.6 AR Space Mapping (BBox + NMS)
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.1.6 BBOX & AR MAPPING");
  addFooter(s, 11, TOTAL);
  s.addText("§ 4.1.6  Bounding Box Regression, NMS & AR Space Mapping", { x: 0.6, y: 0.85, w: 11, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\n// Left: IoU + NMS
  addCard(s, 0.5, 1.6, 6.2, 2.6);
  s.addText("Bounding Box Regression & IoU", { x: 0.8, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Decode [cx, cy, w, h] → [x1, y1, x2, y2]\nIoU(A, B) = Area(A∩B) / Area(A∪B)\n\n  = max(0, x2−x1) · max(0, y2−y1)\n  / (|A| + |B| − Area(A∩B))", { x: 0.8, y: 2.2, w: 5.6, h: 1.8, fontSize: 11.5, fontFace: "Courier New", color: C.teal, lineSpacingMultiple: 1.35 });
\n// Right: NMS steps
  addCard(s, 6.9, 1.6, 5.8, 2.6);
  s.addText("NMS Iterative Greedy Suppression", { x: 7.1, y: 1.75, w: 5.2, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("1. Sort all boxes by confidence → O(A log A)\n2. Pop top box, suppress all with IoU > 0.45\n3. Recurse until queue empty\n→ Each physical object retains exactly ONE box", { x: 7.1, y: 2.2, w: 5.4, h: 1.8, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Bottom: AR Space Mapping Flow
  addCard(s, 0.5, 4.5, 12.2, 2.6);
  s.addText("§ 4.1.6 AR Space Mapping — Full Pipeline (Figure 4.3)", { x: 0.8, y: 4.65, w: 8, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
\nconst arSteps = [\n{ t: "[1,84,8400]\nTensor", c: C.teal },\n{ t: "O(N) Flat\nParse", c: C.navy },\n{ t: "NMS\nIoU>0.45", c: C.accent },\n{ t: "[0,1] Norm\nCoords", c: C.purple },\n{ t: "Dynamic Res\n(W×H)", c: C.green },\n{ t: "AR OverlayView\nPrecise Fit", c: C.red },\n];\narSteps.forEach((st, i) => {\nconst xx = 0.7 + i * 2.05;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.15, w: 1.7, h: 1.0, rectRadius: 0.08, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 5.15, w: 1.7, h: 1.0, fontSize: 10.5, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.15 });
    if (i < 5) s.addText("→", { x: xx + 1.7, y: 5.2, w: 0.35, h: 0.9, fontSize: 16, color: C.subtext, align: "center", valign: "middle" });
  });
\ns.addText("200ms Anti-Flicker + Min-Area Priority Click (OverlayView)", { x: 0.8, y: 6.2, w: 11.5, h: 0.3, fontSize: 10, fontFace: "Arial", color: C.subtext, align: "center" });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 12 — 4.2.1 Wav2Vec2 Acoustic Model
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · SPEECH EVALUATION");
  addFooter(s, 12, TOTAL);
  s.addText("§ 4.2.1  Wav2Vec2: On-Device Phoneme Decoding", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 12.2, 2.2);
  s.addText("Acoustic Model Architecture (CNN + Transformer + CTC)", { x: 0.8, y: 1.75, w: 10, h: 0.35, fontSize: 15, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ t: "Raw\nWaveform", c: C.teal },\n{ t: "CNN\nFeature\nEncoder", c: C.navy },\n{ t: "Transformer\nContext\nNetwork", c: C.purple },\n{ t: "CTC\nDecoding", c: C.accent },\n{ t: "ARPAbet\n39-class", c: C.green },\n].forEach((st, i) => {\nconst xx = 0.8 + i * 2.4;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 2.2, w: 1.8, h: 1.3, rectRadius: 0.1, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 2.2, w: 1.8, h: 1.3, fontSize: 11, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.15 });
    if (i < 4) s.addText("→", { x: xx + 1.8, y: 2.35, w: 0.6, h: 1.0, fontSize: 22, color: C.subtext, align: "center", valign: "middle" });
  });
\naddCard(s, 0.5, 4.1, 5.9, 2.9);
  s.addText("Model Deployment", { x: 0.8, y: 4.25, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("• Wav2Vec2-base (95M params) → ONNX opset14\n• INT8 dynamic quantization\n• Model: 1.26 GB (FP32) → 50 MB (INT8 quantized)\n• ONNX Runtime Mobile + Direct Memory Buffer\n• Zero-copy tensor delivery to CPU/NPU\n• Greedy CTC decoding: no language model needed\n• Inference: ~300 ms on Snapdragon 8 Gen 2", { x: 0.8, y: 4.7, w: 5.3, h: 2.1, fontSize: 11.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.4 });
\naddCard(s, 6.8, 4.1, 5.9, 2.9);
  s.addText("CTC Greedy Decoding Formula", { x: 7.1, y: 4.25, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("ŷ = argmax  Σ  xt[yt]   (per time step t)\n       y∈L*\n\nCollapse: merge consecutive identical phonemes\nFilter: remove h#, spn, <pad> tokens\n\n→ Pure acoustic output (no semantic smoothing)", { x: 7.1, y: 4.7, w: 5.3, h: 2.1, fontSize: 11.5, fontFace: "Courier New", color: C.teal, lineSpacingMultiple: 1.35 });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 13 — 4.2.2 Audio Preprocessing + CMU Dict Pruning
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.2.2 AUDIO PREPROCESSING");
  addFooter(s, 13, TOTAL);
  s.addText("§ 4.2.2  VAD · Z-score Normalization · CMU Dictionary Pruning", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 18, fontFace: "Arial", bold: true, color: C.navy });
\n// Three column cards
  addCard(s, 0.5, 1.6, 3.9, 2.8);
  s.addText("① VAD Silence Trimming", { x: 0.7, y: 1.75, w: 3.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("AudioRecord 16kHz PCM -> Float\n\nThreshold: 0.02 (empirical)\n-> Filter noise, preserve faint fricatives\n\nKey: 1600-sample buffer zone\n-> Protects /p/, /t/, /k/ plosive tails\n-> Prevents threshold clipping from breaking time-domain integrity", { x: 0.7, y: 2.2, w: 3.5, h: 2.0, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\naddCard(s, 4.65, 1.6, 3.9, 2.8);
  s.addText("② Z-score Normalization", { x: 4.85, y: 1.75, w: 3.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Eliminates volume fluctuation from mic hardware:\n\nz = (x - mu) / (sigma + eps)\neps = 1e-7 (prevent div by zero)\n\n-> Maps input to Wav2Vec2 pretraining acoustic space\n-> Boosts on-device feature decoding confidence", { x: 4.85, y: 2.2, w: 3.5, h: 2.0, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\naddCard(s, 8.8, 1.6, 4.0, 2.8);
  s.addText("③ Vision-Anchored Dictionary Pruning", { x: 9.0, y: 1.75, w: 3.6, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Original CMU Dict: 135,000 entries\n-> Keep only visually accessible concrete nouns\n\nPruning: remove prepositions/verbs/adj/abstract nouns\n-> Keep: concrete nouns in daily AR scene\n\nResult: 135,000 -> ~1,000 (O(1) hash)\n\n-> Closes visual-to-acoustic loop with zero latency", { x: 9.0, y: 2.2, w: 3.6, h: 2.0, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Bottom: Complete audio preprocessing flow
  addCard(s, 0.5, 4.7, 12.2, 2.3);
  s.addText("Complete Audio Preprocessing Pipeline", { x: 0.8, y: 4.85, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ t: "AudioRecord\n16kHz PCM", c: C.teal },\n{ t: "VAD Trim\n(thresh=0.02\nbuf=1600)", c: C.navy },\n{ t: "Z-score\nNorm", c: C.purple },\n{ t: "Wav2Vec2\nONNX", c: C.accent },\n{ t: "CTC Greedy\nDecode", c: C.green },\n].forEach((st, i) => {\nconst xx = 0.8 + i * 2.4;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.3, w: 1.8, h: 1.3, rectRadius: 0.1, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 5.3, w: 1.8, h: 1.3, fontSize: 10.5, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.15 });
    if (i < 4) s.addText("→", { x: xx + 1.8, y: 5.4, w: 0.6, h: 1.0, fontSize: 20, color: C.subtext, align: "center", valign: "middle" });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 14 — 4.2.3 Multi-Level Weighted NW Algorithm
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.2.3 MULTI-LVL NW ALGORITHM");
  addFooter(s, 14, TOTAL);
  s.addText("§ 4.2.3  Multi-Level Weighted Needleman-Wunsch Alignment", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\n// Left: DP equation
  addCard(s, 0.5, 1.55, 6.2, 2.4);
  s.addText("DP State Transition Equation", { x: 0.8, y: 1.7, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("M[i,j] = max {\nM[i-1,j-1] + S(r_i, u_j)  (sub/match)\nM[i-1,j]   + P_del        (del/skip)\nM[i,j-1]   + P_ins        (ins/extra)\n}\n\nP_del = P_ins = -1.0  |  Gap penalties", { x: 0.8, y: 2.1, w: 5.6, h: 1.7, fontSize: 11.5, fontFace: "Courier New", color: C.teal, lineSpacingMultiple: 1.35 });
\n// Right: Scoring function S(ri, uj)
  addCard(s, 6.9, 1.55, 5.8, 2.4);
  s.addText("Scoring Function S(rᵢ, uⱼ)", { x: 7.1, y: 1.7, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("=  1.0    if ri = uj          (perfect match)\n=  1.0    if (ri,uj) in I    (ignored/safe pass)\n=  0.6    if (ri,uj) in F    (flaw/tolerant)\n= -1.0    otherwise         (substitution/error)", { x: 7.1, y: 2.1, w: 5.4, h: 1.7, fontSize: 11.5, fontFace: "Courier New", color: C.teal, lineSpacingMultiple: 1.35 });
\n// Ignored 12 pairs
  addCard(s, 0.5, 4.2, 6.2, 1.45, { border: C.green });
  s.addShape(pres.ShapeType.roundRect, { x: 0.7, y: 4.32, w: 1.6, h: 0.38, rectRadius: 0.06, fill: { color: C.green } });
  s.addText("🛡️ Ignored (Safe Pass) — 12 pairs", { x: 0.7, y: 4.32, w: 1.6, h: 0.38, fontSize: 10, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
  s.addText("Voiceless/voiced: t/d, p/b, k/g, v/b   |   Weak drift: k/hh, p/hh, t/hh, k/d   |   Vowel confusion: ah/ae, ae/ah   |   iy/ih, ih/iy\n-> Counters on-device MEMS mic distortion (VOT differences)   |   Score: +1.0   |   UI: green", { x: 0.7, y: 4.75, w: 5.8, h: 0.8, fontSize: 10.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.25 });
\n// Flaw 14 pairs
  addCard(s, 6.9, 4.2, 5.8, 1.45, { border: C.orange });
  s.addShape(pres.ShapeType.roundRect, { x: 7.1, y: 4.32, w: 1.6, h: 0.38, rectRadius: 0.06, fill: { color: C.orange } });
  s.addText("⚡ Flaw (Tolerance) — 14 pairs", { x: 7.1, y: 4.32, w: 1.6, h: 0.38, fontSize: 10, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
  s.addText("L2 typical errors: th/s, dh/z, r/l   |   ae/eh, ao/aa, ao/ah, ah/ao   |   v/w\n-> Positive reinforcement: +0.6 (not -1.0)   |   UI: yellow highlight   |   Guides tongue position adjustment", { x: 7.1, y: 4.75, w: 5.4, h: 0.8, fontSize: 10.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.25 });
\n// Bottom: Table 4.2 reference
  addCard(s, 0.5, 5.9, 12.2, 1.2);
  s.addText("Table 4.2 (thesis): Multi-level weighted NW - initializes DP matrix, fills via transition equation, executes traceback + trimEdgeInsertions, computes score with short-word lenient model (if L≤4 & Penalty≤1.0 → Sfinal=max(Sraw, 88.0))", { x: 0.8, y: 6.0, w: 11.5, h: 1.0, fontSize: 11, fontFace: "Arial", color: C.subtext, lineSpacingMultiple: 1.3 });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 15 - 4.2.4 Artifact Trimming + Lenient Scoring
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.2.4 ARTIFACT TRIM & SCORING");
  addFooter(s, 15, TOTAL);
  s.addText("§ 4.2.4  trimEdgeInsertions & Short-Word Lenient Scoring Model", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\n// Left: trimEdgeInsertions
  addCard(s, 0.5, 1.6, 6.0, 2.8);
  s.addText("trimEdgeInsertions Algorithm (Figure 4.4, 4.6)", { x: 0.8, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Problem: Wav2Vec2 misrecognizes boundary noise\n(breathing, mic current) as speech-like phonemes\n\n1. Front Trimming: scan from index 0, strip leading\n   Insertions before first Match/Substitution\n\n2. Tail Trimming: scan backward, strip trailing\n   Insertions after last valid phoneme\n\nResult: scoring focuses on real vocalization only", { x: 0.8, y: 2.2, w: 5.5, h: 2.0, fontSize: 11.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Right: Short-word lenient model
  addCard(s, 6.8, 1.6, 5.9, 2.8);
  s.addText("Short-Word Lenient Scoring Model", { x: 7.1, y: 1.75, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Problem: 3-phoneme word + 1 flaw → score 66 (failing)\n\n  S_raw = max(0, 1 − Penalty/L) × 100\n\nNonlinear protection:\n  S_final = max(S_raw, S_baseline=88.0)\n  if L ≤ 4 AND Penalty ≤ 1.0\n\nS_baseline = 88.0 (empirically tuned via beta testing)\n→ Prevents \"score cliff\" for beginners\n→ Still highlights flaw in yellow at phoneme level", { x: 7.1, y: 2.2, w: 5.3, h: 2.0, fontSize: 11.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Bottom: Three states
  addCard(s, 0.5, 4.7, 12.2, 2.3);
  s.addText("Three-State Diagnostic UI (Figure 4.7)", { x: 0.8, y: 4.85, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ label: "Correct (Perfect)", desc: "Perfect phoneme match\nScore: +1.0 → Green UI", color: "22C55E", bg: "F0FDF4" },\n{ label: "Flaw (Minor Error)", desc: "Typical L2 error pattern\nScore: +0.6 → Yellow UI", color: C.orange, bg: "FFFBEB" },\n{ label: "Error (Major)", desc: "Complete mispronunciation\nScore: −1.0 → Red UI", color: C.red, bg: "FEF2F2" },\n].forEach((st, i) => {\nconst xx = 1.0 + i * 3.9;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.3, w: 3.5, h: 1.4, rectRadius: 0.1, fill: { color: st.bg }, line: { color: st.color, width: 2 } });
    s.addText(st.label, { x: xx + 0.15, y: 5.4, w: 3.2, h: 0.45, fontSize: 13, fontFace: "Arial", bold: true, color: st.color });
    s.addText(st.desc, { x: xx + 0.15, y: 5.9, w: 3.2, h: 0.7, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.2 });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 16 — AR Rendering: Shader + PorterDuff
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · AR RENDERING");
  addFooter(s, 16, TOTAL);
  s.addText("§ 4.3.1  YIQ Laser Shader & PorterDuff Holographic Feathering", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 5.9, 2.8);
  s.addText("YIQ Color Space Hue Rotation", { x: 0.8, y: 1.75, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("RGB → YIQ transforms to decouple luminance from color\n\n[I']   [cos(ωt)  -sin(ωt)] [I]\n[Q'] = [sin(ωt)   cos(ωt)] [Q]\n\nY channel (luminance) stays constant\n→ GPU pixel-level matrix multiplication\n→ Zero-performance-loss laser animation\n→ Preserves original texture & lighting", { x: 0.8, y: 2.2, w: 5.3, h: 2.0, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.4 });
\naddCard(s, 6.8, 1.6, 5.9, 2.8);
  s.addText("PorterDuff DST_IN Holographic Feathering", { x: 7.1, y: 1.75, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("A_out = A_dst × A_src  (DST_IN composite)\n\n→ RadialGradient: opacity decays from center outward\n→ Edge pixels: A_src ≈ 0 → A_out = 0\n→ Solid center + feathered dissolving edge\n→ \"Holographic projection\" effect\n→ Dual-track: cyberpunk laser card", { x: 7.1, y: 2.2, w: 5.3, h: 2.0, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.4 });
\nconst arScanPath = path.join(PIC, "ar_scan.jpg");
  const laserPath = path.join(PIC, "lasercard.jpg");
  const addImageCard = (x, pathImg, label, fallback) => {\nif (fs.existsSync(pathImg)) {\naddCard(s, x, 4.7, 5.9, 2.4);
      s.addImage({ path: pathImg, x: x + 0.2, y: 4.9, w: 5.5, h: 1.9, rounding: true });
      s.addText(label, { x: x + 0.2, y: 6.8, w: 5.5, h: 0.25, fontSize: 10, fontFace: "Arial", bold: true, color: C.teal, align: "center" });
    } else {\naddCard(s, x, 4.7, 5.9, 2.4);
      s.addText(fallback, { x: x + 0.2, y: 5.5, w: 5.5, h: 1.2, fontSize: 13, color: C.subtext, align: "center", valign: "middle" });
    }\n};\naddImageCard(0.5, arScanPath, "AR Real-Time Scanning (Figure 4.9a)", "AR Real-Time Scan\n(Image: ar_scan.jpg)");
  addImageCard(6.8, laserPath, "Holographic Laser Card (Figure 4.9b)", "Holographic Laser Card\n(Image: lasercard.jpg)");
}\n\n// ═══════════════════════════════════════════════
// SLIDE 17 - 4.3.2 Gyro 1st-Order Filter + Recursive Parallax
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.3.2 GYRO FILTER & PARALLAX");
  addFooter(s, 17, TOTAL);
  s.addText("§ 4.3.2  1st-Order Lag Filter & Recursive DFS 3D Parallax", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\n// Left: 1st order filter
  addCard(s, 0.5, 1.6, 6.0, 2.8);
  s.addText("1st-Order Lag Low-Pass Filter (α = 0.15)", { x: 0.8, y: 1.75, w: 5.5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Problem: Hand tremor + MEMS thermal noise\n→ High-frequency jitter on gyroscope data\n\nFilter equation:\n  y[n] = α · x[n] + (1 − α) · y[n−1]\n\n  α = 0.15 (empirically tuned)\n→ O(1) constant time complexity\n→ Silky-smooth gravity offset output (dx, dy)\n→ Eliminates high-frequency jitter, preserves real motion", { x: 0.8, y: 2.2, w: 5.5, h: 2.0, fontSize: 11.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Right: Recursive DFS
  addCard(s, 6.8, 1.6, 5.9, 2.8);
  s.addText("Recursive DFS View Tree Traversal", { x: 7.1, y: 1.75, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("For each child View with depth tag k:\n\n  TranslationX = dx × k_view\n  TranslationY = dy × k_view\n\nDepth layers (Figure 4.9):\n  k = 0.05  (Background — nearly fixed)\n  k = 0.20  (Holographic frame — moderate follow)\n  k = 0.50  (Floating text — strong float)\n\nDFS: recursively traverses nested ViewGroup\n→ Single gyro signal → full 3D spatial response", { x: 7.1, y: 2.2, w: 5.3, h: 2.0, fontSize: 11.5, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.35 });
\n// Bottom: Architecture diagram (Figure 4.9 simplified)
  addCard(s, 0.5, 4.7, 12.2, 2.3);
  s.addText("Figure 4.9 — Gyroscope Filter & Parallax UI Architecture", { x: 0.8, y: 4.85, w: 10, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ t: "MEMS\nGyroscope", c: C.teal },\n{ t: "1st-Order\nLPF (α=0.15)", c: C.navy },\n{ t: "Gravity\nOffset (dx,dy)", c: C.purple },\n{ t: "DFS View\nTree Traversal", c: C.accent },\n{ t: "Background\nk=0.05", c: C.teal },\n{ t: "Frame\nk=0.20", c: C.navy },\n{ t: "Text\nk=0.50", c: C.green },\n].forEach((st, i) => {\nconst xx = 0.6 + i * 1.72;\ns.addShape(pres.ShapeType.roundRect, { x: xx, y: 5.3, w: 1.45, h: 1.0, rectRadius: 0.08, fill: { color: st.c } });
    s.addText(st.t, { x: xx, y: 5.3, w: 1.45, h: 1.0, fontSize: 9.5, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle", lineSpacingMultiple: 1.15 });
    if (i < 6) s.addText("→", { x: xx + 1.45, y: 5.4, w: 0.27, h: 0.9, fontSize: 14, color: C.subtext, align: "center", valign: "middle" });
  });
  s.addText("3D Depth Parallax Effect — near objects appear larger/move faster, distant objects smaller/slower", { x: 0.8, y: 6.35, w: 11.5, h: 0.3, fontSize: 10, fontFace: "Arial", color: C.subtext, align: "center" });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 18 — User Profile Dashboard
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.accent);
  addChapterBadge(s, "CH.4 · § 4.3.3 USER PROFILE DASHBOARD");
  addFooter(s, 18, TOTAL);
  s.addText("§ 4.3.3  User Profile & Multi-Dimensional Learning Radar", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 20, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 12.2, 2.4);
  s.addText("Five-Dimensional Radar Chart (Figure 4.10)", { x: 0.8, y: 1.75, w: 6, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("The learning dashboard aggregates pronunciation records, showcase breadth, and Ebbinghaus forgetting-curve review data into five normalized dimensions [0, 1]:", { x: 0.8, y: 2.2, w: 11.5, h: 0.7, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.3 });
  [\n{ n: "① Pronunciation Accuracy", c: C.teal },\n{ n: "② Word Card Diversity", c: C.navy },\n{ n: "③ Activity Frequency", c: C.accent },\n{ n: "④ Breakthrough Potential", c: C.purple },\n{ n: "⑤ Long-Term Memory Retention", c: C.green },\n].forEach((d, i) => {\ns.addShape(pres.ShapeType.roundRect, { x: 0.8 + i * 2.2, y: 3.0, w: 2.0, h: 0.5, rectRadius: 0.06, fill: { color: d.c } });
    s.addText(d.n, { x: 0.8 + i * 2.2, y: 3.0, w: 2.0, h: 0.5, fontSize: 10, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
  });
\naddCard(s, 0.5, 4.3, 5.9, 2.7);
  s.addText("Architecture", { x: 0.8, y: 4.45, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("• Room ORM persistent storage\n• Background coroutine pre-aggregation\n• Radar vector v = (v1...v5), each vi ∈ [0, 1]\n• Intermediate results cached in UserStats table\n• Presentation: one lightweight SQL query\n• Fully offline — no external network dependency", { x: 0.8, y: 4.9, w: 5.3, h: 2.0, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.45 });
\naddCard(s, 6.8, 4.3, 5.9, 2.7);
  s.addText("ML/UI Decoupling", { x: 7.1, y: 4.45, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("• Analysis module fully decoupled from ML inference\n• ML engine and UI rendering on independent threads\n• Pre-computed on app startup (background)\n• Real-time on-demand SQL render on presentation layer\n• Showcases Ebbinghaus curve visual degradation", { x: 7.1, y: 4.9, w: 5.3, h: 2.0, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.45 });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 19 — Chapter 5: Experiments Divider
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.darkBg };\ns.addShape(pres.ShapeType.rect, { x: 0, y: 0, w: 13.33, h: 0.08, fill: { color: C.green } });
  s.addText("05", { x: 0.6, y: 1.0, w: 3, h: 1.5, fontSize: 72, fontFace: "Arial", bold: true, color: C.green });
  s.addShape(pres.ShapeType.rect, { x: 0.6, y: 2.6, w: 3.0, h: 0.05, fill: { color: C.green } });
  s.addText("Experiments &\nPerformance\nEvaluation", { x: 0.6, y: 2.9, w: 6, h: 2.2, fontSize: 30, fontFace: "Arial", bold: true, color: C.white, lineSpacingMultiple: 1.2 });
  [\n{ title: "Object Detection", items: "Recall across 3 environments", color: C.teal },\n{ title: "Memory & GC", items: "250s stress test results", color: C.navy },\n{ title: "Pronunciation", items: "Confusion matrix + F1=90.8%", color: C.accent },\n].forEach((m, i) => {\ns.addShape(pres.ShapeType.roundRect, { x: 7.5, y: 1.8 + i * 1.7, w: 5.2, h: 1.35, rectRadius: 0.12, fill: { color: "111D30" }, line: { color: m.color, width: 1.5 } });
    s.addText(m.title, { x: 7.8, y: 1.9 + i * 1.7, w: 4.6, h: 0.5, fontSize: 16, fontFace: "Arial", bold: true, color: m.color });
    s.addText(m.items, { x: 7.8, y: 2.4 + i * 1.7, w: 4.6, h: 0.5, fontSize: 11, fontFace: "Arial", color: C.subtext });
  });
  addFooter(s, 19, TOTAL);
}\n\n// ═══════════════════════════════════════════════
// SLIDE 20 — Experiments: Detection
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.green);
  addChapterBadge(s, "CH.5 · EXPERIMENTS");
  addFooter(s, 20, TOTAL);
  s.addText("Experiment: Object Detection Recall (3 Scenarios)", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\ns.addTable([\n[{ text: "Scenario", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "Baseline FP32", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "VisionVoice INT8+FT", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "Improvement", options: { bold: true, color: C.white, fill: { color: C.navy } } }],\n[{ text: "Standard Lighting" }, { text: "89.5%" }, { text: "92.1%", options: { bold: true, color: C.green } }, { text: "+2.6%", options: { bold: true, color: C.green } }],\n[{ text: "Strong Backlight" }, { text: "78.2%" }, { text: "84.5%", options: { bold: true, color: C.green } }, { text: "+6.3%", options: { bold: true, color: C.green } }],\n[{ text: "30% Occlusion" }, { text: "65.4%" }, { text: "76.3%", options: { bold: true, color: C.green } }, { text: "+10.9%", options: { bold: true, color: C.green } }],\n], { x: 0.5, y: 1.65, w: 8.5, fontSize: 13, fontFace: "Arial", color: C.text, border: { type: "solid", pt: 0.5, color: C.divider }, colW: [2.5, 2.0, 2.2, 1.5], rowH: [0.4, 0.4, 0.4, 0.4], autoPage: false });
\naddCard(s, 9.5, 1.65, 3.2, 1.6);
  s.addText("Key Finding", { x: 9.7, y: 1.8, w: 2.8, h: 0.35, fontSize: 13, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("INT8 + domain fine-tuning\nnot only compresses 4×\nbut IMPROVES recall in\nALL scenarios", { x: 9.7, y: 2.2, w: 2.8, h: 0.9, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.3 });
\naddCard(s, 0.5, 3.4, 12.2, 3.6);
  s.addText("Bar Chart: Detection Recall Across Environments", { x: 0.8, y: 3.55, w: 8, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
\nconst barData = [\n{ label: "Standard\nLighting", baseline: 89.5, visionvoice: 92.1, y: 4.2, h: 2.4 },\n{ label: "Strong\nBacklight", baseline: 78.2, visionvoice: 84.5, y: 4.2, h: 2.4 },\n{ label: "30%\nOcclusion", baseline: 65.4, visionvoice: 76.3, y: 4.2, h: 2.4 },\n];\nbarData.forEach((b, i) => {\nconst x = 1.5 + i * 3.5;\nconst maxH = 2.4;\n// Baseline bar
    const baseH = (b.baseline / 100) * maxH;\ns.addShape(pres.ShapeType.rect, { x: x, y: b.y + maxH - baseH, w: 0.9, h: baseH, fill: { color: C.red } });
    s.addText(b.baseline + "%", { x: x, y: b.y + maxH - baseH - 0.35, w: 0.9, h: 0.3, fontSize: 10, fontFace: "Arial", bold: true, color: C.red, align: "center" });
    // VisionVoice bar
    const vvH = (b.visionvoice / 100) * maxH;\ns.addShape(pres.ShapeType.rect, { x: x + 1.0, y: b.y + maxH - vvH, w: 0.9, h: vvH, fill: { color: C.green } });
    s.addText(b.visionvoice + "%", { x: x + 1.0, y: b.y + maxH - vvH - 0.35, w: 0.9, h: 0.3, fontSize: 10, fontFace: "Arial", bold: true, color: C.green, align: "center" });
    // Label
    s.addText(b.label, { x: x, y: b.y + maxH + 0.05, w: 1.9, h: 0.5, fontSize: 11, fontFace: "Arial", color: C.text, align: "center" });
  });
  // Legend
  s.addShape(pres.ShapeType.rect, { x: 10.5, y: 4.3, w: 0.4, h: 0.3, fill: { color: C.red } });
  s.addText("Baseline FP32", { x: 11.0, y: 4.3, w: 1.8, h: 0.3, fontSize: 11, fontFace: "Arial", color: C.text });
  s.addShape(pres.ShapeType.rect, { x: 10.5, y: 4.7, w: 0.4, h: 0.3, fill: { color: C.green } });
  s.addText("VisionVoice", { x: 11.0, y: 4.7, w: 1.8, h: 0.3, fontSize: 11, fontFace: "Arial", color: C.text });
  s.addText("Test device: Snapdragon 8 Gen 2, 12 GB RAM, Android 14", { x: 0.8, y: 6.7, w: 11.5, h: 0.25, fontSize: 10, fontFace: "Arial", color: C.subtext, align: "center" });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 21 — Experiments: Memory + Pronunciation
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.green);
  addChapterBadge(s, "CH.5 · EXPERIMENTS");
  addFooter(s, 21, TOTAL);
  s.addText("Experiment: Memory GC Performance & Pronunciation Diagnosis", { x: 0.6, y: 0.85, w: 12, h: 0.55, fontSize: 22, fontFace: "Arial", bold: true, color: C.navy });
\n// Left: Memory
  addCard(s, 0.5, 1.6, 5.9, 3.2);
  s.addText("250s Memory GC Stress Test", { x: 0.8, y: 1.75, w: 5.2, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addText("Baseline: sawtooth heap curve (88 MB ↔ 15 MB)\n→ Frequent GC pauses → frame drops\n\nVisionVoice: ByteBuffer.allocateDirect(4.9 MB)\noff-heap pool, flat ~14 MB throughout\n→ Zero GC pauses → stable ≥ 30 FPS", { x: 0.8, y: 2.2, w: 5.3, h: 1.8, fontSize: 12, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.4 });
\n// Draw simplified sawtooth vs flat graph
  s.addShape(pres.ShapeType.rect, { x: 0.8, y: 3.8, w: 2.5, h: 0.8, fill: { color: "FEF2F2" }, line: { color: C.red, width: 1 } });
  s.addText("Baseline:\n~88 MB → ~15 MB\nSawtooth", { x: 0.9, y: 3.85, w: 2.3, h: 0.7, fontSize: 10, fontFace: "Arial", color: C.red, align: "center" });
  s.addShape(pres.ShapeType.rect, { x: 3.5, y: 4.0, w: 2.5, h: 0.6, fill: { color: "F0FDF4" }, line: { color: C.green, width: 1 } });
  s.addText("VisionVoice:\n~14 MB flat line\nZero GC", { x: 3.6, y: 4.05, w: 2.3, h: 0.5, fontSize: 10, fontFace: "Arial", color: C.green, align: "center" });
\n// Right: Confusion matrix
  addCard(s, 6.8, 1.6, 5.9, 3.2);
  s.addText("Pronunciation Confusion Matrix", { x: 7.1, y: 1.75, w: 5.3, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  s.addTable([\n[{ text: "Category", options: { bold: true, color: C.white, fill: { color: C.navy } } }, { text: "Accuracy", options: { bold: true, color: C.white, fill: { color: C.navy } } }],\n[{ text: "Perfect" }, { text: "88%", options: { color: C.green } }],\n[{ text: "Flaw" }, { text: "82%", options: { color: C.orange } }],\n[{ text: "Error" }, { text: "84%", options: { color: C.red } }],\n[{ text: "Noise" }, { text: "91%", options: { color: C.green } }],\n], { x: 7.1, y: 2.2, w: 5.4, fontSize: 12, fontFace: "Arial", color: C.text, border: { type: "solid", pt: 0.5, color: C.divider }, colW: [2.8, 2.0], rowH: [0.4, 0.4, 0.4, 0.4, 0.4], autoPage: false });
\n// Key metrics
  addCard(s, 0.5, 5.1, 12.2, 1.9);
  s.addText("Key Performance Metrics", { x: 0.8, y: 5.25, w: 5, h: 0.35, fontSize: 14, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ m: "< 45 ms", l: "End-to-End Detection" },\n{ m: "90.8%", l: "NW Error Detection F1" },\n{ m: "r = 0.87", l: "Expert Correlation" },\n{ m: ">= 30 FPS", l: "AR Rendering" },\n{ m: "~14 MB", l: "Heap Memory" },\n{ m: "~450 ms", l: "E2E Pronunciation" },\n].forEach((k, i) => {\nconst x = 0.8 + i * 2.05;\ns.addShape(pres.ShapeType.roundRect, { x, y: 5.7, w: 1.85, h: 1.1, rectRadius: 0.08, fill: { color: C.teal } });
    s.addText(k.m, { x, y: 5.72, w: 1.85, h: 0.55, fontSize: 13, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
    s.addText(k.l, { x, y: 6.28, w: 1.85, h: 0.45, fontSize: 9, fontFace: "Arial", color: "D0E8FF", align: "center", valign: "middle" });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 22 — Chapter 6: Conclusion
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.lightBg };\naddTopBar(s, C.red);
  addChapterBadge(s, "CH.6 · CONCLUSION");
  addFooter(s, 22, TOTAL);
  s.addText("Conclusion & Future Work", { x: 0.6, y: 0.85, w: 10, h: 0.55, fontSize: 24, fontFace: "Arial", bold: true, color: C.navy });
\naddCard(s, 0.5, 1.6, 7.5, 5.4);
  s.addText("Four Core Innovations", { x: 0.8, y: 1.75, w: 7, h: 0.4, fontSize: 16, fontFace: "Arial", bold: true, color: C.navy });
  [\n{ num: "1", title: "Pure On-Device Offline Multi-Modal Architecture", desc: "YOLO + Wav2Vec2 entirely local; zero network dependency; voiceprints never leave device sandbox", color: C.teal },\n{ num: "2", title: "O(N) Flat Parsing + Zero-Allocation Memory", desc: "Off-heap ByteBuffer eliminates GC stutter; stable ≥ 30 FPS AR rendering", color: C.navy },\n{ num: "3", title: "Pedagogy-Oriented NW Phoneme Alignment", desc: "Safe Pass (12 pairs) + Flaw Downgrade (14 pairs) specifically for L2 Chinese EFL learners", color: C.accent },\n{ num: "4", title: "Context → Cognition → Pronunciation Closed Loop", desc: "AR object anchoring deeply coupled with phoneme-level pronunciation correction", color: C.green },\n].forEach((inn, i) => {\nconst yy = 2.35 + i * 1.05;\ns.addShape(pres.ShapeType.roundRect, { x: 0.9, y: yy, w: 0.55, h: 0.55, rectRadius: 0.08, fill: { color: inn.color } });
    s.addText(inn.num, { x: 0.9, y: yy, w: 0.55, h: 0.55, fontSize: 16, fontFace: "Arial", bold: true, color: C.white, align: "center", valign: "middle" });
    s.addText(inn.title, { x: 1.6, y: yy + 0.02, w: 6.2, h: 0.28, fontSize: 13, fontFace: "Arial", bold: true, color: C.text });
    s.addText(inn.desc, { x: 1.6, y: yy + 0.3, w: 6.2, h: 0.28, fontSize: 10.5, fontFace: "Arial", color: C.subtext });
  });
\naddCard(s, 8.3, 1.6, 4.5, 5.4);
  s.addText("Future Directions", { x: 8.6, y: 1.75, w: 4, h: 0.4, fontSize: 16, fontFace: "Arial", bold: true, color: C.purple });
  [\n"Open-vocabulary detection (YOLO-World / CLIP)",\n"Wav2Vec2 knowledge distillation (<200ms)",\n"Ebbinghaus adaptive review scheduling",\n"Attention-based multimodal fusion",\n"Large-scale RCT (N > 200)",\n].forEach((f, i) => {\ns.addText("→  " + f, { x: 8.6, y: 2.3 + i * 0.65, w: 4, h: 0.55, fontSize: 11, fontFace: "Arial", color: C.text, lineSpacingMultiple: 1.1 });
  });
}\n\n// ═══════════════════════════════════════════════
// SLIDE 23 — Thank You
// ═══════════════════════════════════════════════
{\nconst s = pres.addSlide();
  s.background = { fill: C.darkBg };\ns.addShape(pres.ShapeType.rect, { x: 0, y: 0, w: 13.33, h: 0.08, fill: { color: C.teal } });
  s.addShape(pres.ShapeType.rect, { x: 0, y: 7.42, w: 13.33, h: 0.08, fill: { color: C.green } });
  s.addShape(pres.ShapeType.ellipse, { x: 4.6, y: 0.5, w: 4.0, h: 4.0, fill: { color: "0D3B66" }, line: { color: C.teal, width: 2, dashType: "dash" } });
  s.addText("Thank You", { x: 1.0, y: 2.2, w: 11.3, h: 1.5, fontSize: 52, fontFace: "Arial", bold: true, color: C.white, align: "center" });
  s.addText("Questions & Discussion", { x: 1.0, y: 3.8, w: 11.3, h: 0.8, fontSize: 22, fontFace: "Arial", color: C.teal, align: "center" });
  s.addShape(pres.ShapeType.rect, { x: 4.5, y: 4.8, w: 4.3, h: 0.04, fill: { color: C.teal } });
  s.addText("Song Yaobohan  ·  Wuhan University  ·  2026", { x: 1.0, y: 5.1, w: 11.3, h: 0.5, fontSize: 14, fontFace: "Arial", color: C.subtext, align: "center" });
  s.addText("VisionVoice — See It, Speak It, Master It", { x: 1.0, y: 5.7, w: 11.3, h: 0.5, fontSize: 13, fontFace: "Arial", italic: true, color: C.green, align: "center" });
  addFooter(s, 23, TOTAL);
}\n\n// ═══════════════════════════════════════════════
// SAVE
// ═══════════════════════════════════════════════
const outPath = "C:\\Users\\Dell\\Desktop\\Hayden\\毕设文件夹\\VisionVoice_Presentation_v2.pptx";\npres.writeFile({ fileName: outPath }).then(() => {\nconsole.log("✅ PPT generated: " + outPath);
  console.log("Total slides: " + TOTAL);
}).catch(err => {\nconsole.error("❌ Error:", err);
});
