package org.bigcraft.infpoints.command;

import dev.jorel.commandapi.CommandAPICommand;
import org.jetbrains.annotations.NotNull;

public abstract class SubCommand {

    private final String name;

    protected SubCommand(@NotNull String name) {
        this.name = name;
    }

    protected abstract @NotNull CommandAPICommand build(@NotNull CommandAPICommand command);

    public final @NotNull CommandAPICommand command() {
        return build(new CommandAPICommand(name));
    }

}
