package ru.exeswi.exest.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent horror progress: how long the world has been terrorized, per-player sanity
 * and the hiding spots the monsters have "learned". Stored with the overworld so the
 * whole server shares one horror timeline.
 */
public class HorrorWorldState extends PersistentState {

    public static final PersistentState.Type<HorrorWorldState> TYPE =
            new PersistentState.Type<>(HorrorWorldState::new, HorrorWorldState::fromNbt, null);

    private static final int MAX_HIDING_SPOTS = 16;

    /** Total ticks any player has been present; drives difficulty scaling. */
    public long horrorTicks;
    /** Earliest world time the final hunt may (re)start. */
    public long nextFinalHuntAt;

    public static final int STAT_COUNT = 7;

    private final Map<UUID, Float> sanity = new HashMap<>();
    private final Map<UUID, Deque<BlockPos>> hidingSpots = new HashMap<>();
    private final java.util.Set<UUID> disclaimerSeen = new java.util.HashSet<>();
    private final Map<UUID, int[]> stats = new HashMap<>();
    private final Map<UUID, List<String>> journals = new HashMap<>();
    private final List<BlockPos> altars = new ArrayList<>();

    public static HorrorWorldState get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager()
                .getOrCreate(TYPE, "exest_horror");
    }

    public float getSanity(UUID player) {
        return sanity.getOrDefault(player, 100.0f);
    }

    public void setSanity(UUID player, float value) {
        sanity.put(player, Math.max(0.0f, Math.min(100.0f, value)));
        markDirty();
    }

    public void rememberHidingSpot(UUID player, BlockPos pos) {
        Deque<BlockPos> spots = hidingSpots.computeIfAbsent(player, k -> new ArrayDeque<>());
        // ignore duplicates of places we already know
        for (BlockPos known : spots) {
            if (known.isWithinDistance(pos, 4.0)) {
                return;
            }
        }
        spots.addFirst(pos);
        while (spots.size() > MAX_HIDING_SPOTS) {
            spots.removeLast();
        }
        markDirty();
    }

    public List<BlockPos> getHidingSpots(UUID player) {
        Deque<BlockPos> spots = hidingSpots.get(player);
        return spots == null ? List.of() : new ArrayList<>(spots);
    }

    /** True the first time this player is seen — used to show the disclaimer once. */
    public boolean markDisclaimerSeen(UUID player) {
        boolean first = disclaimerSeen.add(player);
        if (first) {
            markDirty();
        }
        return first;
    }

    public int[] getStats(UUID player) {
        int[] values = stats.computeIfAbsent(player, k -> new int[STAT_COUNT]);
        if (values.length < STAT_COUNT) {
            values = java.util.Arrays.copyOf(values, STAT_COUNT);
            stats.put(player, values);
        }
        return values;
    }

    public void incStat(UUID player, int index) {
        getStats(player)[index]++;
        markDirty();
    }

    public List<String> getJournal(UUID player) {
        return journals.computeIfAbsent(player, k -> new ArrayList<>());
    }

    public void addJournalEntry(UUID player, String entry) {
        List<String> journal = getJournal(player);
        journal.add(entry);
        while (journal.size() > 12) {
            journal.remove(0);
        }
        markDirty();
    }

    public void addAltar(BlockPos pos) {
        altars.add(pos);
        while (altars.size() > 8) {
            altars.remove(0);
        }
        markDirty();
    }

    public List<BlockPos> getAltars() {
        return altars;
    }

    public void removeAltar(BlockPos pos) {
        altars.remove(pos);
        markDirty();
    }

    public static HorrorWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        HorrorWorldState state = new HorrorWorldState();
        state.horrorTicks = nbt.getLong("HorrorTicks");
        NbtList sanityList = nbt.getList("Sanity", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < sanityList.size(); i++) {
            NbtCompound entry = sanityList.getCompound(i);
            state.sanity.put(entry.getUuid("Id"), entry.getFloat("Value"));
        }
        NbtList spotsList = nbt.getList("HidingSpots", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < spotsList.size(); i++) {
            NbtCompound entry = spotsList.getCompound(i);
            Deque<BlockPos> spots = new ArrayDeque<>();
            NbtList positions = entry.getList("Spots", NbtElement.LONG_TYPE);
            for (int j = 0; j < positions.size(); j++) {
                spots.add(BlockPos.fromLong(((net.minecraft.nbt.NbtLong) positions.get(j)).longValue()));
            }
            state.hidingSpots.put(entry.getUuid("Id"), spots);
        }
        NbtList seenList = nbt.getList("DisclaimerSeen", NbtElement.STRING_TYPE);
        for (int i = 0; i < seenList.size(); i++) {
            try {
                state.disclaimerSeen.add(UUID.fromString(seenList.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        state.nextFinalHuntAt = nbt.getLong("NextFinalHunt");
        NbtList statsList = nbt.getList("Stats", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < statsList.size(); i++) {
            NbtCompound entry = statsList.getCompound(i);
            state.stats.put(entry.getUuid("Id"), entry.getIntArray("Values"));
        }
        NbtList journalList = nbt.getList("Journals", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < journalList.size(); i++) {
            NbtCompound entry = journalList.getCompound(i);
            List<String> lines = new ArrayList<>();
            NbtList linesTag = entry.getList("Lines", NbtElement.STRING_TYPE);
            for (int j = 0; j < linesTag.size(); j++) {
                lines.add(linesTag.getString(j));
            }
            state.journals.put(entry.getUuid("Id"), lines);
        }
        NbtList altarList = nbt.getList("Altars", NbtElement.LONG_TYPE);
        for (int i = 0; i < altarList.size(); i++) {
            state.altars.add(BlockPos.fromLong(((net.minecraft.nbt.NbtLong) altarList.get(i)).longValue()));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLong("HorrorTicks", horrorTicks);
        NbtList sanityList = new NbtList();
        for (Map.Entry<UUID, Float> entry : sanity.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", entry.getKey());
            tag.putFloat("Value", entry.getValue());
            sanityList.add(tag);
        }
        nbt.put("Sanity", sanityList);
        NbtList spotsList = new NbtList();
        for (Map.Entry<UUID, Deque<BlockPos>> entry : hidingSpots.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", entry.getKey());
            NbtList positions = new NbtList();
            for (BlockPos pos : entry.getValue()) {
                positions.add(net.minecraft.nbt.NbtLong.of(pos.asLong()));
            }
            tag.put("Spots", positions);
            spotsList.add(tag);
        }
        nbt.put("HidingSpots", spotsList);
        NbtList seenList = new NbtList();
        for (UUID id : disclaimerSeen) {
            seenList.add(net.minecraft.nbt.NbtString.of(id.toString()));
        }
        nbt.put("DisclaimerSeen", seenList);
        nbt.putLong("NextFinalHunt", nextFinalHuntAt);
        NbtList statsList = new NbtList();
        for (Map.Entry<UUID, int[]> entry : stats.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", entry.getKey());
            tag.putIntArray("Values", entry.getValue());
            statsList.add(tag);
        }
        nbt.put("Stats", statsList);
        NbtList journalList = new NbtList();
        for (Map.Entry<UUID, List<String>> entry : journals.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", entry.getKey());
            NbtList lines = new NbtList();
            for (String line : entry.getValue()) {
                lines.add(net.minecraft.nbt.NbtString.of(line));
            }
            tag.put("Lines", lines);
            journalList.add(tag);
        }
        nbt.put("Journals", journalList);
        NbtList altarList = new NbtList();
        for (BlockPos pos : altars) {
            altarList.add(net.minecraft.nbt.NbtLong.of(pos.asLong()));
        }
        nbt.put("Altars", altarList);
        return nbt;
    }
}
