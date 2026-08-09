package ru.vanillagame.voicechat.bettergroups.client;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

// The default font renders non-ASCII glyphs from the squished unifont
// fallback, so UI icons ship as 8x8 bitmap glyphs on private-use codepoints
// in a mod font. Textures are white; color comes from the component style.
public final class Icons {

    public static final FontDescription FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("svc_better_groups_client", "icons"));

    public static final String CROWN = "\uE000";
    public static final String AJAR_DOOR = "\uE001";

    private Icons() {
    }
}
