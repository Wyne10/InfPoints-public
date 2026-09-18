package me.wyne.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.infpoints.InfPoints;
import me.wyne.infpoints.command.InfPointsCommand;
import me.wyne.infpoints.command.PersonalCommands;
import me.wyne.infpoints.placeholder.PointsPlaceholders;
import me.wyne.infpoints.vault.VaultRegistrar;

import java.util.List;
import java.util.function.Supplier;

public final class OptionalModules {

    public static final AbstractModule PLACEHOLDER = new OptionalModule(
            "me.clip.placeholderapi.PlaceholderAPI",
            "PlaceholderAPI not found, placeholders are not registered",
            () -> List.of(PointsPlaceholders.class)
    );

    public static final AbstractModule COMMAND = new OptionalModule(
            "dev.jorel.commandapi.CommandAPI",
            "CommandAPI not found, commands are not registered",
            () -> List.of(InfPointsCommand.class, PersonalCommands.class)
    );

    public static final AbstractModule VAULT = new OptionalModule(
            "net.milkbowl.vault.economy.Economy",
            "Vault not found, the Vault economy is not provided",
            () -> List.of(VaultRegistrar.class)
    );

    private OptionalModules() {}

    // Classes are supplied lazily, loading one that extends a missing plugin's class would fail before the check
    private static final class OptionalModule extends AbstractModule {

        private final String className;
        private final String missingMessage;
        private final Supplier<List<Class<?>>> classes;

        private OptionalModule(String className, String missingMessage, Supplier<List<Class<?>>> classes) {
            this.className = className;
            this.missingMessage = missingMessage;
            this.classes = classes;
        }

        @Override
        protected void configure() {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                InfPoints.logger().warn(missingMessage);
                return;
            }
            for (Class<?> type : classes.get())
                bind(type);
        }

    }

}
