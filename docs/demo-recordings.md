# README demo recordings

These GIFs are screen recordings of real Minecraft clients, not reconstructed UI.
Only the caption strip above the game is added during encoding.

## Recording environment

- Debian 13, Xvfb at 1920 × 1080, Mesa llvmpipe OpenGL 4.5, Java 25.
- Minecraft 26.1.2, Fabric Loader 0.18.4, Fabric API 0.155.3+26.1.2,
  Simple Voice Chat 2.6.24.
- Paper 26.1.2 build 74 and the published Better Groups 0.13.0 server and Fabric client JARs.
- Two separate game directories with local test players `Alex` and `Steve`.
- A separate flat creative world; Minecraft listens on `127.0.0.1:25575`.
  Offline authentication is used only for this local recording server.
- Both clients completed Simple Voice Chat's connection handshake. Microphones
  are muted; the GIFs demonstrate the controls and routing transitions without audio.

The recordings use the published release JARs because the cloud Gradle dependency
download returned HTTP 429. No game or mod code was changed for recording.

## Scenarios

| GIF | Recorded actions | Duration | Size |
| --- | --- | --- | --- |
| `assets/demo-invite.gif` | Alex opens **+**, selects Steve; switch to Steve's client; Steve clicks **Accept** in chat and opens the group containing both players. | 13.7 s | 1,587,349 bytes |
| `assets/demo-leadership.gif` | Alex's crown and member removal control; Alex types `/voicegroup transfer Steve`; reopen the group to show Steve's crown. | 12.6 s | 1,413,790 bytes |
| `assets/demo-pause.gif` | Steve clicks **Ⅱ**, sees nearby Alex in proximity mode, opens the group menu, clicks **Return to group**, and returns with his crown restored. | 14.6 s | 1,202,868 bytes |

`Adventure` is password protected. Neither accepting the invite nor returning
from pause displays a password prompt. Alex remains in the group while Steve
pauses, so it continues to exist. Leadership transfer uses the real chat command;
the current client has a removal button, not a transfer button.

## Capture and optimization

Capture each 900 × 700 client window with FFmpeg X11 capture at 15 fps, using
`libx264 -preset ultrafast -crf 16` for the intermediate recording. Switch windows
by raising and focusing the corresponding X11 window; send real mouse and key
events with `xdotool`. Keep the client directories and intermediate MP4s outside
the repository.

For each final GIF, reduce to 10 fps, add a 40-pixel caption strip above the game,
and use a shared 128-color palette:

```text
fps=10,pad=iw:ih+40:0:40:color=0x17212b,
<drawtext captions>,split[a][b];
[a]palettegen=stats_mode=diff:max_colors=128[p];
[b][p]paletteuse=dither=none:diff_mode=rectangle
```

Encode with `-loop 0`, then optimize with `gifsicle -O3 --careful`. The final
900 × 740 GIFs total 4,204,007 bytes, about 38% smaller than the initial
256-color dithered GIFs. Check the decoded invitation, command, crowns, return
button, and final group states before replacing README links.

Use absolute HTTPS image URLs pinned to the commit containing these assets so
the Modrinth export works and deleting the feature branch does not break them.
Keep the generated download sections and Modrinth markers unchanged.
