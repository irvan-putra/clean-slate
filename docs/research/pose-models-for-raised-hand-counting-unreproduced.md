# Counting raised hands from a camera: a small test of two pose models (historical, unreproduced)

Status: record of a test run on 2026-10-27 on a laptop, using still public photos from Wikimedia Commons. It was not repeated for this note, so treat the numbers as unreproduced and not as a performance claim. Nothing was tested with a live audience.

## What was tried
Two pose detectors were compared for finding raised hands on a laptop:
- MediaPipe pose, the "full" `pose_full.task` model, up to 12 people, minimum detection confidence 0.35. A hand counted as raised when a wrist landmark (visibility above 0.3) was more than 0.04 of the image height above the nose.
- YOLOv8n-pose as an ONNX file (Xenova/yolov8n-pose on Hugging Face, `onnx/model.onnx`), run with ONNX Runtime at 640 px, confidence 0.25 and a non-maximum-suppression step, with the same wrist-above-nose rule.
Exact model file hashes and library versions were not recorded in the surviving test notes.

## What was seen (as recorded then)
- Classroom photo, one raised hand among about 15 seated children (Wikimedia Commons file "1964 Hammond Slides Student Raising Hand.jpg"): MediaPipe found 0 people. YOLOv8n-pose found 12 people and exactly 1 raised hand.
- Photo of 3 children seen from behind with 3 to 4 hands up (Commons file "Uchit 0.jpg"): MediaPipe found 1 person and 1 hand. YOLOv8n-pose found 5 boxes and 0 hands, because the nose is hidden from behind and the wrist-above-nose rule fails. A shoulder-line fallback was planned and not tested here.
- A large-hall crowd photo (about 200 people; the exact Commons file is not pinned in the record): both found 5 people. Neither worked at that distance.

## Cautions
- Three photos is not a benchmark. Two of the three photo names are given above; the third is not pinned, and model file hashes are missing, so the test is only partly repeatable.
- Counting people with a camera in a room needs the participants' informed agreement and a clear statement of what is and is not stored. Rounds count the peak number of hands seen at once, not unique people.
- The Ultralytics YOLOv8 models were reported as AGPL-3.0 at the time. That was not re-checked for this note, so check the current licence and its attribution terms before reusing the model in a public repo.
