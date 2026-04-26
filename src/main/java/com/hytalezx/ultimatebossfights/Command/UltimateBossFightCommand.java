package com.hytalezx.ultimatebossfights.Command;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class UltimateBossFightCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;

    public UltimateBossFightCommand(String pluginName, String pluginVersion) {
        super("ultimatebossfights", "Show a " + pluginName + " mod version.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Running " + pluginName + " v" + pluginVersion + " mod!"));
    }
}