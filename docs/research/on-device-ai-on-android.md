# On-device AI on a mid-range Android phone: exploration notes

Status: exploration. Sources checked 2026-11-16. No measurements were made on any phone.

## Two kinds of job
1. A small model for one narrow job, such as detecting objects from a fixed list.
2. A general vision-language model for open questions about an image.
Size and speed depend on the model, quantization and phone. Neither is "fast" or "slow" in general; measure both on the target phone before choosing.

## Published figures for one small detector
Ultralytics lists YOLO11n at 2.6M parameters and 6.5 GFLOPs at 640 px input, with CPU ONNX speed on its reference hardware. These are vendor figures on that hardware, not a phone result. https://docs.ultralytics.com/models/yolo11/

## Fixed class lists
Models trained on COCO cover its 80 classes. https://docs.ultralytics.com/datasets/detect/coco/ That page says fine-tuning is recommended for domain-specific or unseen objects. Other options exist (open-vocabulary detectors, a general model, or rules around a known set); none was tested here.

## System-provided models
Android documents Gemini Nano through AICore and ML Kit GenAI APIs, with limits on tokens, hardware and capability, and says the model is not available on every device. It describes cloud fallback as one option. Any cloud fallback in an app should be an explicit opt-in, off by default. https://developer.android.com/ai/gemini-nano

## To test
Latency and memory of candidate models at 320 and 640 px on a mid-range phone, and whether a detector plus rules is enough.
