package org.bigcraft.infpoints.module;

import com.google.inject.AbstractModule;
import org.bigcraft.infpoints.config.SqlConfig;

public class ConfigModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SqlConfig.class);
    }
}
