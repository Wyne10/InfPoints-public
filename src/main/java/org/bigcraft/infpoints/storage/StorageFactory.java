package org.bigcraft.infpoints.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bigcraft.infpoints.InfPoints;
import org.bigcraft.infpoints.point.PointDefinition;
import org.bigcraft.infpoints.point.PointState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class StorageFactory {

    private final SqlStorage sql;
    private final PdcStorage pdc;
    private final LevelStorage level;
    private final ExpStorage exp;

    @Inject
    public StorageFactory(SqlStorage sql, PdcStorage pdc, LevelStorage level, ExpStorage exp) {
        this.sql = sql;
        this.pdc = pdc;
        this.level = level;
        this.exp = exp;
    }

    public @NotNull PointState create(@NotNull PointDefinition definition, @Nullable PointState previous) {
        return switch (definition.type()) {
            case MEMORY -> new PointState(definition, memory(definition, previous), null);
            case PDC -> new PointState(definition, pdc, pdc.validate(definition));
            case LEVEL -> new PointState(definition, level, null);
            case EXP -> new PointState(definition, exp, null);
            case SQL -> new PointState(definition, sql, sql.prepare(definition));
        };
    }

    // Memory balances survive a reload as long as they can still be read the same way
    private PointStorage memory(PointDefinition definition, @Nullable PointState previous) {
        if (previous != null && previous.storage() instanceof MemoryStorage memory) {
            if (previous.definition().decimals() == definition.decimals())
                return memory;
            InfPoints.logger().warn("Decimals of memory point '{}' changed, its balances were reset", definition.key());
        }
        return new MemoryStorage();
    }

}
