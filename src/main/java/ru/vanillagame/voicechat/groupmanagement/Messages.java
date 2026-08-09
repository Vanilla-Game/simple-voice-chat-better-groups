package ru.vanillagame.voicechat.groupmanagement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

final class Messages {

    private static final String PREFIX = "svc_group_management.";

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
    static final String INVITE_COOLDOWN = key("invite.cooldown");
    static final String INVITE_NOT_FOUND = key("invite.not_found");
    static final String INVITE_EXPIRED = key("invite.expired");
    static final String INVITE_WRONG_PLAYER = key("invite.wrong_player");
    static final String KICK_SENDER_NOT_IN_GROUP = key("kick.sender_not_in_group");
    static final String KICK_UNKNOWN_LEADER = key("kick.unknown_leader");
    static final String KICK_NOT_LEADER = key("kick.not_leader");
    static final String KICK_SELF = key("kick.self");
    static final String KICK_TARGET_NOT_MEMBER = key("kick.target_not_member");
    static final String KICK_FAILED = key("kick.failed");
    static final String KICK_SUCCESS = key("kick.success");
    static final String KICK_TARGET_NOTIFICATION = key("kick.target_notification");
    static final String TRANSFER_SENDER_NOT_IN_GROUP = key("transfer.sender_not_in_group");
    static final String TRANSFER_SELF = key("transfer.self");
    static final String TRANSFER_TARGET_NOT_MEMBER = key("transfer.target_not_member");
    static final String TRANSFER_UNKNOWN_LEADER = key("transfer.unknown_leader");
    static final String TRANSFER_NOT_LEADER = key("transfer.not_leader");
    static final String TRANSFER_SUCCESS = key("transfer.success");
    static final String REQUEST_NOT_NEEDED = key("request.not_needed");
    static final String REQUEST_GROUP_NOT_FOUND = key("request.group_not_found");
    static final String REQUEST_GROUP_AMBIGUOUS = key("request.group_ambiguous");
    static final String REQUEST_UNKNOWN_LEADER = key("request.unknown_leader");
    static final String REQUEST_LEADER_OFFLINE = key("request.leader_offline");
    static final String REQUEST_COOLDOWN = key("request.cooldown");
    static final String REQUEST_SENT = key("request.sent");
    static final String REQUEST_RECEIVED = key("request.received");
    static final String REQUEST_ACCEPT_LABEL = key("request.accept.label");
    static final String REQUEST_ACCEPT_HOVER = key("request.accept.hover");
    static final String REQUEST_EXPIRES = key("request.expires");
    static final String REQUEST_NOT_FOUND = key("request.not_found");
    static final String REQUEST_EXPIRED = key("request.expired");
    static final String APPROVE_NOT_LEADER = key("approve.not_leader");
    static final String APPROVE_REQUESTER_OFFLINE = key("approve.requester_offline");
    static final String APPROVE_REQUESTER_IN_GROUP = key("approve.requester_in_group");
    static final String APPROVE_FAILED = key("approve.failed");
    static final String APPROVE_SUCCESS = key("approve.success");
    static final String APPROVE_JOINED = key("approve.joined");
    static final String LEADER_PROMOTED = key("leader.promoted");

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
            INVITE_COOLDOWN,
            INVITE_NOT_FOUND,
            INVITE_EXPIRED,
            INVITE_WRONG_PLAYER,
            KICK_SENDER_NOT_IN_GROUP,
            KICK_UNKNOWN_LEADER,
            KICK_NOT_LEADER,
            KICK_SELF,
            KICK_TARGET_NOT_MEMBER,
            KICK_FAILED,
            KICK_SUCCESS,
            KICK_TARGET_NOTIFICATION,
            TRANSFER_SENDER_NOT_IN_GROUP,
            TRANSFER_SELF,
            TRANSFER_TARGET_NOT_MEMBER,
            TRANSFER_UNKNOWN_LEADER,
            TRANSFER_NOT_LEADER,
            TRANSFER_SUCCESS,
            REQUEST_NOT_NEEDED,
            REQUEST_GROUP_NOT_FOUND,
            REQUEST_GROUP_AMBIGUOUS,
            REQUEST_UNKNOWN_LEADER,
            REQUEST_LEADER_OFFLINE,
            REQUEST_COOLDOWN,
            REQUEST_SENT,
            REQUEST_RECEIVED,
            REQUEST_ACCEPT_LABEL,
            REQUEST_ACCEPT_HOVER,
            REQUEST_EXPIRES,
            REQUEST_NOT_FOUND,
            REQUEST_EXPIRED,
            APPROVE_NOT_LEADER,
            APPROVE_REQUESTER_OFFLINE,
            APPROVE_REQUESTER_IN_GROUP,
            APPROVE_FAILED,
            APPROVE_SUCCESS,
            APPROVE_JOINED,
            LEADER_PROMOTED
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
