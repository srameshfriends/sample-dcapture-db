package sample.dcapture.db.dev;

import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlForwardTool;
import dcapture.db.core.SqlTable;
import dcapture.db.postgres.PgDatabase;
import dcapture.io.BaseSettings;
import dcapture.io.DispatcherRegistry;
import dcapture.io.Localization;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;
import sample.dcapture.db.service.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Registry implements DispatcherRegistry {
    private static final Logger logger = Logger.getLogger(Registry.class.getName());

    @Override
    public void inject(Binder binder) {
        try {
            BaseSettings settings = BaseSettings.load(Registry.class);
            Localization localization = Localization.development(Registry.class);
            binder.bind(BaseSettings.class).toInstance(settings);
            binder.bind(Localization.class).toInstance(localization);
            binder.bind(SqlDatabase.class).toInstance(getDatabase(settings));
        } catch (Exception ex) {
            ex.printStackTrace();
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
        List<SqlTable> tables = tableBuilder.getTableList(database, settings.getDatabaseConfig());
        database.config("schema", "dcapture");
        database.config("url", settings.getDatabaseUrl());
        database.config("user", BaseSettings.decode(settings.getDatabaseUser()));
        database.config("password", BaseSettings.decode(settings.getDatabasePassword()));
        database.config("autoCommit", false);
        database.config("tables", tables);
        database.start(SqlForwardTool.class.getSimpleName());
        return database;
    }
}
