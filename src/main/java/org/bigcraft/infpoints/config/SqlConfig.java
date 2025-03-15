package org.bigcraft.infpoints.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.wyne.wutils.config.Config;
import me.wyne.wutils.config.ConfigEntry;
import me.wyne.wutils.jdbc.DriverLibrary;
import org.bigcraft.infpoints.InfPoints;

@SuppressWarnings("FieldMayBeFinal")
@Singleton
@Getter
public class SqlConfig {

    @ConfigEntry(section = "SQL", comment = "Load and register JDBC driver, options: NONE, H2_V1, H2_V2, MYSQL, MARIADB, POSTGRESQL, SQLITE")
    private String driver = "NONE";

    @ConfigEntry(section = "SQL")
    private String jdbcUrl = "jdbc:mysql://localhost:3306/test";

    @ConfigEntry(section = "SQL")
    private String username, password;

    @Inject
    public SqlConfig(InfPoints plugin) {
        Config.global.registerConfigObject(this);
        Config.global.loadConfig(plugin.getConfig(), this);
        DriverLibrary.valueOf(driver).registerDriver();
    }

    public boolean isConfigured()
    {
        return (jdbcUrl != null && username != null && password != null)
                && (!jdbcUrl.isEmpty() && !username.isEmpty());
    }

}
