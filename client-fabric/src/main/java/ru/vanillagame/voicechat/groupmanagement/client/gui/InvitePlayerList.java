package ru.vanillagame.voicechat.groupmanagement.client.gui;

import de.maxhenkel.voicechat.gui.widgets.ListScreenListBase;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import ru.vanillagame.voicechat.groupmanagement.client.ServerSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvitePlayerList extends ListScreenListBase<InvitePlayerEntry> {

    protected final List<InvitePlayerEntry> entries;
    protected String filter;

    public InvitePlayerList(int width, int height, int top, int itemSize) {
        super(width, height, top, itemSize);
        this.entries = new ArrayList<>();
        this.filter = "";
        updateEntryList();
    }

    // Candidates: connected to voice chat, not in any group, not ourselves
    // (getPlayerStates(false) already excludes the local player). The server
    // re-validates all of this on /vcgroup invite anyway.
    public void updateEntryList() {
        entries.clear();
        for (PlayerState state : ClientManager.getPlayerStateManager().getPlayerStates(false)) {
            if (state.isDisconnected() || state.hasGroup()) {
                continue;
            }
            entries.add(new InvitePlayerEntry(state));
        }
        updateFilter();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    private void updateFilter() {
        clearEntries();
        List<InvitePlayerEntry> filtered = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filtered.removeIf(entry ->
                    !entry.getState().getName().toLowerCase(Locale.ROOT).contains(filter));
        }
        filtered.sort((e1, e2) -> e1.getState().getName().compareToIgnoreCase(e2.getState().getName()));
        replaceEntries(filtered);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        InvitePlayerEntry entry = getEntryAtPosition(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (entry == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
        ServerSupport.invite(entry.getState().getName());
        minecraft.gui.setScreen(null);
        return true;
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }
}
