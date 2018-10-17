package sample.dcapture.db.luncher;

import dcapture.db.core.KeySequence;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlForwardTool;
import dcapture.db.core.SqlTable;
import dcapture.db.h2.H2Database;
import dcapture.db.postgres.PgDatabase;
import dcapture.db.util.SqlTableBuilder;
import dcapture.io.BaseSettings;
import dcapture.io.DispatcherRegistry;
import dcapture.io.Localization;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;
import sample.dcapture.db.service.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class Registry implements DispatcherRegistry {

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
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void start(String... args) throws Exception;

    protected abstract void stop(String... args) throws Exception;

    private SqlDatabase getDatabase(BaseSettings settings) throws Exception {
        SqlDatabase database;
        if(settings.getDatabaseUrl().contains("postgresql")){
            database = new PgDatabase();
        } else {
            database = new H2Database();
        }
        SqlTableBuilder tableBuilder = new SqlTableBuilder(database.getTypeMap());
        List<SqlTable> tables = tableBuilder.getTableList(settings.getDatabaseConfig());
        database.config("schema", "dcapture");
        database.config("url", settings.getDatabaseUrl());
        database.config("user", BaseSettings.decode(settings.getDatabaseUser()));
        database.config("password", BaseSettings.decode(settings.getDatabasePassword()));
        database.config("autoCommit", false);
        database.config("tables", tables);
        database.start(SqlForwardTool.class.getSimpleName());
        addDatabaseDefaultRecords(database);
        return database;
    }

    private void addDatabaseDefaultRecords(SqlDatabase database) throws SQLException {
        KeySequence keySequence = new KeySequence(database);
        keySequence.setSequence("expense", KeySequence.MONTHLY, "E");
    }
}
