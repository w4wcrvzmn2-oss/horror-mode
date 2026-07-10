# Horror Mode

A psychological horror conversion for Minecraft **1.21.1** (Fabric). Instead of cheap
jumpscares it builds constant, low-grade dread: you are being watched, the world is
slowly rotting, and your character's mind is quietly falling apart.

> ## ⚠ Disclaimer
> This mod contains **jumpscares, flashing lights, sudden loud sounds, chase
> sequences and psychological horror content**. It is not recommended for people with
> photosensitive epilepsy or heart conditions. Every element (jumpscares, darkness,
> hallucinations, visual effects, sounds) can be toggled individually in
> `config/exest-horror.json`. An in-game warning is shown once on first join.

## Features

**Psychological horror.** Distant footsteps, breathing behind your back, whispers,
unexplained sounds, doors that open on their own, blocks that change while you are not
looking, cave sounds on the surface, music that suddenly distorts.

**Dynamic darkness.** Normally the world looks vanilla — then the light suddenly dies:
short, sharp blackout pulses (~5 seconds) where even torches are smothered, Night
Vision barely works, and fog can briefly close in to a black wall four blocks from
your face. Never a permanent filter.

**The Stalker & 10 more creatures.** Each with its own AI: the Stalker watches, vanishes
when stared at and reappears behind you; the Smiler moves only while unseen; the Crawler
strikes only turned backs; the Eyeless One hunts by sound and ignores silent sneaking;
the Mimic borrows the name of a real online player; The Unseen has no body at all.
Monsters open doors, break weak blocks, flank, hunt in groups, retreat and pretend to
leave, learn your hiding spots, and attack only when the odds favor them.

**Random encounters.** Figures on hills, in windows, between trees, underwater, in cave
darkness, for a single frame, silhouettes sprinting across your screen — plus fake
join/leave/death/advancement chat messages only you can see.

**Sanity.** A hidden 0–100 stat drained by darkness, monsters and events, restored by
daylight and sleep. Low sanity brings hallucinations: fake ore, fake mobs, fake players,
phantom sounds, drifting camera, involuntary blinks — and more frequent events.

**World events.** Red moon, blackout, endless thunder, frozen time (clocks stop),
spinning compasses, total silence zones, flickering light.

**Environment corruption.** As difficulty grows: leaves rot away, grass coarsens,
plants wither, flesh-like patches surface, villages and pastures quietly empty out —
always off-screen.

**Difficulty scaling.** One horror level per ~2 in-game days (0–10): more events,
smarter and stronger monsters, deeper darkness, faster sanity drain, new creatures
unlock progressively.

**Multiplayer.** Events are per-player (one of you gets gaslit while the other sees
nothing), hunters split across different targets, world events are synchronized.

**The Final Hunt.** At horror level 10 it stops hiding: darkness, waves of enraged
hunters and a named boss that does not vanish, fear light or stop. Kill it and the
world is cleansed — difficulty resets, sanity restores, the sun returns. Fail, and
"Оно остаётся."

**Structures.** An abandoned hut frozen mid-struggle and a forest altar that a trail
of signs invites you to visit. Visiting the altar has consequences.

**The Journal.** A written book by "???" appears in your inventory. Its entries grow
over time, and the newest one always describes — accurately — what you were doing
five minutes ago.

**No safe rituals.** Sleep can end with you yanked out of bed into the dark (or waking
up a few blocks from where you lay down). Burying yourself underground makes monsters
dig to you through any block; your pounding heart gives away your exact position; ice
floes and sky pillars get their supporting block knocked out from under you.

**HUD & commands.** A toggleable corner panel (key `H`): horror level, sanity bar,
day counter and a presence indicator. `/horror status` and `/horror stats` (the score
it keeps on you) work without op. Optional ModMenu config screen.

## The Legend

Years before you, someone played in these lands. His name was **Benton**. The last
entry in his journal reads: *"it knows my name."* Benton never logged off — something
logged off wearing him. It still walks here, watching from the treeline in his skin,
vanishing when you look too long.

Later a man came looking for him. The players on the old server only ever called him
**Murder** — nobody remembers why, and nobody asked twice. He found Benton. Or what
was left. Whatever he saw broke something in him: now he walks these woods with slow,
ordinary, human footsteps — the only undistorted sound in this world — and he finishes
the wounded. He is still a man. You can kill him. That's the difference between him
and everything else here.

And beneath it all there is a name from the journal that was never meant to be read
aloud: **Ridavoumax**. Benton read it. It does not run, does not hide and does not go
around anything: it walks toward you in a perfectly straight line — through stone,
through your walls, through the floor. Weapons pass through it like cold air. It is
slow. You can outrun it forever. But you have to keep moving, and one day you will
stop to sleep. One touch is all it needs.

## Commands (op level 2)

- `/horror difficulty` — current horror level
- `/horror sanity` / `/horror sanity set <0..100>` — inspect/set your sanity
- `/horror event <id>` — force any event (tab completion lists all)
- `/horror debug on|off` — 10× event frequency + logging, for testing
- `/horror reload` — reload the config
- `/summon exest:stalker` etc. — spawn any creature directly

## Configuration

`config/exest-horror.json` — every system can be toggled (jumpscares, darkness, sanity,
hallucinations, visual effects, fake messages, world events, corruption, monsters) and
tuned (event/monster frequency, audio intensity, difficulty scale, spawn rate,
abduction chance, mob cap, debug mode).

## Design notes

- All sound design uses vanilla assets played wrong (pitch-shifted, sequenced,
  repositioned in 3D), so the mod ships zero binary files and works on any resource pack.
- Monsters reuse vanilla models/skins rendered as pitch-black, scaled silhouettes.
- Performance: events run off one-second timers, corruption has a fixed per-player
  block budget, overlays are plain rect fills — no shaders or framebuffers.

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.
