package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.InfPointsApi;
import org.bigcraft.infpoints.point.BalanceCache;
import org.bigcraft.infpoints.point.CacheListener;
import org.bigcraft.infpoints.point.PermissionRegistrar;
import org.bigcraft.infpoints.point.PointManager;
import org.bigcraft.infpoints.point.TransactionExecutor;
import org.bigcraft.infpoints.storage.ExpStorage;
import org.bigcraft.infpoints.storage.ExperienceChanges;
import org.bigcraft.infpoints.storage.LevelStorage;
import org.bigcraft.infpoints.storage.PdcStorage;
import org.bigcraft.infpoints.storage.StorageFactory;
import org.bigcraft.infpoints.storage.VanillaExperienceBridge;

public final class CoreModules {

    public static final AbstractModule POINT = new CoreModule(
            PointManager.class,
            TransactionExecutor.class,
            BalanceCache.class,
            CacheListener.class,
            PermissionRegistrar.class
    );

    public static final AbstractModule STORAGE = new CoreModule(
            StorageFactory.class,
            PdcStorage.class,
            LevelStorage.class,
            ExpStorage.class,
            ExperienceChanges.class,
            VanillaExperienceBridge.class
    );

    public static final AbstractModule API = new CoreModule(
            InfPointsApi.class
    );

    private CoreModules() {}

    private static final class CoreModule extends AbstractModule {

        private final Class<?>[] classes;

        private CoreModule(Class<?>... classes) {
            this.classes = classes;
        }

        @Override
        protected void configure() {
            for (Class<?> type : classes)
                bind(type);
        }

    }

}
