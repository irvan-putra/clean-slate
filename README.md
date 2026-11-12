# Clean Slate

A private, offline Android app for before/after cleaning photos. Take a BEFORE
photo of an area, clean it, take an AFTER photo with the BEFORE ghosted on top
for alignment, and the app shows what visibly changed: a difference overlay,
a percentage, a tally of recognized objects, and a short verdict that says
plainly what it can and cannot see.

Everything happens on the phone. The app has no internet permission, no
account, no ads, no analytics. Photos never leave the app unless you share
them yourself.

## What this build is

A personal test build, made for one phone (Samsung Galaxy A24).
It is a debug-signed APK installed by sideloading, not from Play Store.

Tested so far: the code compiles, its unit tests pass, and every screen has
been visually checked as a rendered preview. It has NOT yet run on a real
phone - camera, performance, and storage behavior on the actual device are
what the trial is for.

## Known limits (please read before judging it)

- The object recognizer knows the 80 general COCO categories (bottle, cup,
  bowl, chair, ...). It does NOT know cookware/dishes well: on the real sample
  sink photo it found nothing, and the app honestly reports "unable to assess
  recognized objects" rather than pretending.
- The verdict covers only recognized-object changes and pixel-visible change.
  It cannot see dirt, grease, dust, or hygiene, and says so next to every
  verdict.
- A dark photo scores lower regardless of mess. Good light helps.
- The streak counts real sessions only. The built-in "Kitchen sink (sample)"
  area uses a generic, illustrative sink image bundled with the app as its
  BEFORE and never counts toward the streak; attaching an AFTER to it asks
  for explicit confirmation first.

## Install

1. Get app-debug.apk onto the phone (send it to yourself as a WhatsApp
   document, USB cable, or Drive).
2. Tap the file. Android asks once to allow installs from that source - that
   prompt is the normal sideload flow and is fine to accept for this file.
3. If Play Protect warns or blocks: stop there and tell me the exact message.
   Do not tap through it - we look at it together first.
4. Open "Clean Slate" and allow the camera permission.

## Uninstall / recovery

Uninstalling removes the app and everything it created or stored: your
before/after photos, history, and its copy of the sample all live in
app-private storage and are excluded from Android backup and device-transfer,
so uninstalling wipes them completely.

Separate copies you made yourself are not affected and stay until you delete
them: the downloaded APK file, the source archive if you saved it, and the
original sample photo you shared (the app only holds its own copy of it).
Clearing the app's data without uninstalling resets it to the fresh sample
state.

## Privacy

Camera permission only. No INTERNET permission at all. Backup and
device-to-device transfer are disabled for app data.

The bundled sample sink image is a generic, synthetic illustration, not a
real photo of anyone's home. (The 24 Oct 2026 test build embedded a private
photo as the sample; it was replaced on 12 Nov 2026 and the download was
updated.) This package was re-signed with a new debug key during that
replacement: if you installed the 24 Oct build, uninstall it first - Android
will not update across different signing keys.
