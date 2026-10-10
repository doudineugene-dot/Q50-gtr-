# InTouch Gen1 external transport leads — 2026-10-10

## Current scope correction and live verification

The user requires **built-in stock DCU Bluetooth only**. All historical Path B/fallback prioritisation below is superseded; external dongles, phone/UDP bridges and mirroring are outside scope. No driver work or purchases are proposed.

Re-read the linked replacement/root/custom-app posts on 2026-10-10. The replacement author is u/raisamarth; the post explicitly describes an external BT dongle and offers a future code release. Visible comments contain no downloadable source/APK/vendor library; a commenter names Virtual HDT/IVI but provides no Bluetooth signatures. The custom-app thread likewise says no GitHub repo yet. These are author reports, not stock EVI-BT R9 proof. The root post reports host SSH/serial/native binaries, but provides no verified VR30 transport artifact.

More concrete primary artifacts were found in [nickfromstatefarm's 2022 Gen1 post](https://www.reddit.com/r/CarHacking/comments/tjls9z/): immutable process/USB/module dumps. Exact links and engineering consequences are recorded in `bluetooth-findings.md`; Android/host namespace separation is now a specific diagnostic target. Generic GitHub search hits IntRoot/InkRoot and virtyaluk/InTouch are unrelated notes/VK projects, excluded. Public searches for VR30 DASH/VR30DASH/0.5.4 did not locate a verified downloadable APK or its Bluetooth code.

## Verified Reddit lead: Full Intouch Gen-1 Replacement (Kinda)

Source:
https://www.reddit.com/r/q50/comments/1wyfemv/full_intouch_gen1_replacement_kinda_external_wifi/

Published 2026-10-05.

The author states that they built an application intended to replace most factory lower-display functionality and, crucially, that they:

- wrote **Wi-Fi and Bluetooth drivers**;
- added an **external Bluetooth dongle**;
- connected the DCU to a **Bluetooth OBD module** through that external dongle;
- added an **external Wi-Fi dongle** with drivers;
- connected to a phone hotspot;
- updated the **TCP stack** for Internet access;
- pulled Spotify metadata including album art;
- read current climate-control state (read-only);
- implemented most media/source switching.

The author explicitly offered to publish how-to guides and source code.

### Why this matters to EcuTek

This is direct evidence that InTouch Gen1 can use an externally attached USB Bluetooth controller when suitable drivers/userspace support are supplied.

This is **not a phone bridge** and is not screen mirroring. The topology is still:

DCU -> local USB Bluetooth controller -> Bluetooth device

Therefore it is an acceptable fallback architecture for EcuTek if the built-in vendor Bluetooth subsystem cannot expose the BLE/L2CAP CoC path required by EVI-BT R9.

Important limitation: the Reddit post only proves connection to a generic Bluetooth OBD module. It does **not** by itself prove BLE L2CAP CoC support, nor compatibility with EVI-BT R9.

## Related Reddit evidence: Custom Apps on Intouch

Source:
https://www.reddit.com/r/q50/comments/1w811uw/custom_apps_on_intouch/

Useful statements from the thread:

- custom native applications on InTouch Gen1 are working;
- some vehicle sensor data can be read directly by native apps;
- the author later clarified that direct **polling CAN is not possible** through the path they found, but sensor updates already flowing through the system can be consumed;
- packaging EPK is considered straightforward; old hardware/GPU is the harder problem;
- the author did not yet have a public GitHub repository as of 2026-09-11, but said they planned to publish one.

## Related root work

Source:
https://www.reddit.com/r/q50/comments/1wx8j0t/ive_successfully_gained_root_access_to_in_touch/

A separate 2026-10-04 project reports:

- root access on 2014–2018 InTouch Gen1;
- host-level root shell over SSH/serial;
- direct APK sideload from USB;
- pinning apps to home screen;
- ability to run own native binaries;
- root method described as non-permanent/reversible;
- another commenter confirms external Wi-Fi/Bluetooth work is being developed in the same community.

Treat root claims as community-reported until reproduced on this user's DCU.

## Engineering priority

### Path A — preferred
Reproduce the transport used by VR30 DASH with the stock built-in Bluetooth controller.

Goal:
DCU -> built-in BT -> EVI-BT R9 -> LE L2CAP CoC -> live EcuTek RPM.

### Path B — now proven viable in principle
External USB Bluetooth controller on the DCU with custom/native driver + userspace Bluetooth stack.

Goal:
DCU -> USB BT dongle -> EVI-BT R9 -> LE L2CAP CoC -> live EcuTek RPM.

This path is now higher priority than any phone/USB-network/UDP bridge because it preserves a fully standalone in-car solution.

### Path B requirements

Before choosing a dongle/chipset, determine from the author's eventual code/guide or from DCU kernel inspection:

- USB host controller and usable port;
- kernel version/config;
- available USB HCI class support;
- BlueZ/native HCI support;
- whether loadable kernel modules are permitted;
- supported BT USB chipsets;
- whether the stack supports BLE and LE Credit Based L2CAP CoC;
- whether an updated/custom userspace stack can talk to HCI directly if kernel support is insufficient.

Do not assume a cheap CSR classic-Bluetooth dongle is sufficient. EVI R9 is EVI-BTLE.

## Search targets

Continue monitoring for:
- source code release from the Reddit author;
- guide/how-to;
- exact Wi-Fi dongle chipset;
- exact Bluetooth dongle chipset;
- kernel modules / firmware blobs;
- TCP stack changes;
- root/native binary toolchain.

If a repository appears, mirror the relevant technical findings into this document and link the exact files/commits.
