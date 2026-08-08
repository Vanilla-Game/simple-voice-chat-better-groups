package ru.vanillagame.voicechat.grouptools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

final class Messages {

    private static final String PREFIX = "voicechat_group_tools.";

    static final String COMMAND_PLAYERS_ONLY = key("command.players_only");
    static final String COMMAND_NO_PERMISSION = key("command.no_permission");
    static final String COMMAND_USAGE = key("command.usage");
    static final String VOICECHAT_NOT_READY = key("voicechat.not_ready");
    static final String CONNECTION_SELF_UNAVAILABLE = key("connection.self_unavailable");
    static final String CONNECTION_TARGET_UNAVAILABLE = key("connection.target_unavailable");
    static final String PLAYER_NOT_ONLINE = key("player.not_online");
    static final String GROUP_NO_LONGER_EXISTS = key("group.no_longer_exists");
    static final String GROUP_ALREADY_IN_GROUP = key("group.already_in_group");
    static final String GROUP_TARGET_ALREADY_IN_GROUP = key("group.target_already_in_group");
    static final String GROUP_JOIN_FAILED = key("group.join_failed");
    static final String GROUP_JOINED = key("group.joined");
    static final String INVITE_SENDER_NOT_IN_GROUP = key("invite.sender_not_in_group");
    static final String INVITE_SELF = key("invite.self");
    static final String INVITE_ACCEPT_LABEL = key("invite.accept.label");
    static final String INVITE_ACCEPT_HOVER = key("invite.accept.hover");
    static final String INVITE_RECEIVED = key("invite.received");
    static final String INVITE_EXPIRES = key("invite.expires");
    static final String INVITE_SENT = key("invite.sent");
    static final String INVITE_NOT_FOUND = key("invite.not_found");
    static final String INVITE_EXPIRED = key("invite.expired");
    static final String INVITE_WRONG_PLAYER = key("invite.wrong_player");
    static final String KICK_SENDER_NOT_IN_GROUP = key("kick.sender_not_in_group");
    static final String KICK_UNKNOWN_CREATOR = key("kick.unknown_creator");
    static final String KICK_NOT_CREATOR = key("kick.not_creator");
    static final String KICK_SELF = key("kick.self");
    static final String KICK_TARGET_NOT_MEMBER = key("kick.target_not_member");
    static final String KICK_FAILED = key("kick.failed");
    static final String KICK_SUCCESS = key("kick.success");
    static final String KICK_TARGET_NOTIFICATION = key("kick.target_notification");

    private static final Set<String> KEYS = Set.of(
            COMMAND_PLAYERS_ONLY,
            COMMAND_NO_PERMISSION,
            COMMAND_USAGE,
            VOICECHAT_NOT_READY,
            CONNECTION_SELF_UNAVAILABLE,
            CONNECTION_TARGET_UNAVAILABLE,
            PLAYER_NOT_ONLINE,
            GROUP_NO_LONGER_EXISTS,
            GROUP_ALREADY_IN_GROUP,
            GROUP_TARGET_ALREADY_IN_GROUP,
            GROUP_JOIN_FAILED,
            GROUP_JOINED,
            INVITE_SENDER_NOT_IN_GROUP,
            INVITE_SELF,
            INVITE_ACCEPT_LABEL,
            INVITE_ACCEPT_HOVER,
            INVITE_RECEIVED,
            INVITE_EXPIRES,
            INVITE_SENT,
            INVITE_NOT_FOUND,
            INVITE_EXPIRED,
            INVITE_WRONG_PLAYER,
            KICK_SENDER_NOT_IN_GROUP,
            KICK_UNKNOWN_CREATOR,
            KICK_NOT_CREATOR,
            KICK_SELF,
            KICK_TARGET_NOT_MEMBER,
            KICK_FAILED,
            KICK_SUCCESS,
            KICK_TARGET_NOTIFICATION
    );

    private Messages() {
    }

    static Component component(String key, NamedTextColor color, ComponentLike... arguments) {
        return Component.translatable(key, arguments).color(color);
    }

    static Set<String> keys() {
        return KEYS;
    }

    private static String key(String suffix) {
        return PREFIX + suffix;
    }
}
