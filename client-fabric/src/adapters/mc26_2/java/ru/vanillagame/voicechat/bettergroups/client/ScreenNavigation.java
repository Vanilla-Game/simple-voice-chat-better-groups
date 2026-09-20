package ru.vanillagame.voicechat.bettergroups.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenNavigation {

    private ScreenNavigation() {
    }

    public static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    public static void showActionBar(Minecraft minecraft, Component message) {
        minecraft.gui.hud.setOverlayMessage(message, false);
    }
}
