# Download sizes of small speech and pose models, as used in a talk demo

Status: file sizes read from the hosts' own listings. Whisper sizes re-read 2026-11-16 and unchanged from 2026-10-26. The YOLOv8n-pose size was measured on 2026-10-26 and not re-measured. No speed or memory figures were measured on any phone or laptop, so none are given.

## Sizes
Units: MB means 10^6 bytes; MiB means 2^20 bytes. Byte counts are the primary figure.
The repo's code defaults are `--model base` for the voice demo, `yolov8n-pose.pt` for the Python counter, and `onnx-community/whisper-tiny` with `device: 'wasm'` in the browser demo (https://github.com/irvan-putra/edge-ai-multimodal-demos).
| Model | Configured for (in the demo repo's code, 2026-11-16; not a performance test) | Size | Source |
|---|---|---|---|
| Whisper "base" via faster-whisper (CTranslate2 conversion) | laptop, Python | model.bin 145,217,532 bytes (145.2 MB, 138.5 MiB); all listed files 147,882,941 bytes (147.9 MB, 141.0 MiB) | https://huggingface.co/Systran/faster-whisper-base |
| Whisper "tiny", quantized ONNX for browser use via transformers.js | browser, WebAssembly | encoder 10,124,990 bytes plus merged decoder 30,719,241 bytes, 40,844,231 bytes (40.8 MB, 38.95 MiB) for the two model files, plus tokenizer and config files not included in that total | https://huggingface.co/onnx-community/whisper-tiny |
| YOLOv8n-pose weights (PyTorch file) | laptop, Python | 6,832,633 bytes (6.83 MB, 6.52 MiB; measured 2026-10-26 on the Ultralytics v8.2.0 release file) | https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n-pose.pt |

Parameter counts: OpenAI's Whisper README lists tiny at 39 M and base at 74 M parameters, and about 1 GB of GPU memory for both unquantized: https://github.com/openai/whisper/blob/main/README.md . Ultralytics' pose table listed YOLOv8n-pose at about 3.3 M parameters when read on 2026-10-26; the current docs page has since moved to a newer model, so that figure is dated.

## What this does not say
- Download size is not memory use and not speed. Neither was measured here.
- Whether a given model runs well on a particular phone was not tested.
- The browser demo's pose model was a different one (MediaPipe pose landmarker, lite). The lite model's size was a label on the demo page, not measured here.
