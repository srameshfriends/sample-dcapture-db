package sample.dcapture.sql.dev;

import dcapture.io.BaseSettings;
import dcapture.io.DispatcherRegistry;
import dcapture.io.Localization;
import dcapture.io.SettingsFactory;
import dcapture.sql.core.SqlDatabase;
import dcapture.sql.core.SqlForwardTool;
import dcapture.sql.core.SqlLogger;
import dcapture.sql.core.SqlTable;
import dcapture.sql.postgres.PgDatabase;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;
import org.apache.log4j.Logger;
import sample.dcapture.sql.service.*;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Registry implements DispatcherRegistry, SqlLogger {
    static final Logger logger = Logger.getLogger(Registry.class);

    BaseSettings getBaseSettings() throws Exception {
        File settingsFile = SettingsFactory.getClassPath(Registry.class, "settings.json");
        return SettingsFactory.loadBaseSetting(Registry.class, settingsFile);
    }

    @Override
    public void inject(Binder binder) {
        try {
            BaseSettings settings = getBaseSettings();
            Localization localization = SettingsFactory.getLocalization("en", settings.getLocaleFolder());
            binder.bind(BaseSettings.class).toInstance(settings);
            binder.bind(Localization.class).toInstance(localization);
            binder.bind(SqlDatabase.class).toInstance(getDatabase(settings));
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public List<Class<?>> getPathServiceList() {
        List<Class<?>> list = new ArrayList<>();
        list.add(LocaleService.class);
        list.add(SessionService.class);
        list.add(AppsUserService.class);
        list.add(CurrencyService.class);
        list.add(ProjectService.class);
        list.add(ExpenseCategoryService.class);
        list.add(ExpenseService.class);
        return list;
    }

    @Override
    public void destroyed(Injector injector) {
        try {
            SqlDatabase database = injector.getInstance(SqlDatabase.class);
            database.shutdown();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private SqlDatabase getDatabase(BaseSettings settings) throws Exception {
        SqlDatabase database = new PgDatabase();
        SqlTableBuilder tableBuilder = new SqlTableBuilder();
        File schemaFile = SettingsFactory.getClassPath(Registry.class, "sample-db.json");
        List<SqlTable> tableList = tableBuilder.getTableList(database.getTypeMap(), schemaFile);
        String[] dbs = settings.getDatabase("expenses");
        database.config("logger", this);
        database.config("schema", "dcapture");
        database.config("url", dbs[0]);
        database.config("user", SettingsFactory.decode(dbs[1]));
        database.config("password", SettingsFactory.decode(dbs[2]));
        database.config("autoCommit", false);
        database.config("tables", tableList);
        database.start(SqlForwardTool.class.getSimpleName());
        return database;
    }

    @Override
    public void onSqlLog(String description) {
        if (logger.isDebugEnabled()) {
            logger.info(description);
        }
    }

    @Override
    public void onSqlError(Exception ex) {
        logger.error(ex.getMessage());
        if (logger.isDebugEnabled()) {
            ex.printStackTrace();
        }
    }
}
