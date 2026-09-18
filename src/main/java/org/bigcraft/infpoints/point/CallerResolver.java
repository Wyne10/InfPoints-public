package org.bigcraft.infpoints.point;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CallerResolver {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private CallerResolver() {}

    // Names the plugin whose code called into InfPoints, so the history records who changed a balance
    // without every consumer passing a source explicitly
    public static @Nullable String callingPlugin() {
        ClassLoader own = CallerResolver.class.getClassLoader();
        return WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(type -> type.getClassLoader() != own)
                .map(CallerResolver::providingPlugin)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
    }

    private static @Nullable String providingPlugin(Class<?> type) {
        try {
            return JavaPlugin.getProvidingPlugin(type).getName();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

}
