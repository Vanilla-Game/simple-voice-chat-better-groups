package ru.vanillagame.voicechat.bettergroups.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ServerSupport {

    private static final String COMMAND = "vcgroup";
    private static boolean available;

    private ServerSupport() {
    }

    public static boolean isAvailable() {
        return available;
    }

    static void confirm() {
        available = true;
    }

    static void clear() {
        available = false;
    }

    public static void invite(String playerName) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && isAvailable()) {
            connection.sendCommand(COMMAND + " invite " + playerName);
        }
    }

    public static void requestJoin(java.util.UUID groupId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && isAvailable()) {
            connection.sendCommand(COMMAND + " request " + groupId);
        }
    }

    public static void kick(String playerName) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && isAvailable()) {
            connection.sendCommand(COMMAND + " kick " + playerName);
        }
    }
}
