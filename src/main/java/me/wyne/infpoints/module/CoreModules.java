package me.wyne.infpoints.module;

import com.google.inject.AbstractModule;
import me.wyne.infpoints.InfPointsApi;
import me.wyne.infpoints.point.BalanceCache;
import me.wyne.infpoints.point.CacheListener;
import me.wyne.infpoints.point.PermissionRegistrar;
import me.wyne.infpoints.point.PointManager;
import me.wyne.infpoints.point.TransactionExecutor;
import me.wyne.infpoints.storage.ExpStorage;
import me.wyne.infpoints.storage.ExperienceChanges;
import me.wyne.infpoints.storage.LevelStorage;
import me.wyne.infpoints.storage.PdcStorage;
import me.wyne.infpoints.storage.StorageFactory;
import me.wyne.infpoints.storage.VanillaExperienceBridge;

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
