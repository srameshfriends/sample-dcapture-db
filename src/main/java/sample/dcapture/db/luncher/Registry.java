package sample.dcapture.db.luncher;

import dcapture.db.core.SqlDatabase;
import dcapture.db.util.SqlTableBuilder;
import dcapture.io.BaseSettings;
import dcapture.io.DispatcherRegistry;
import dcapture.io.Localization;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;
import sample.dcapture.db.api.KeySequence;
import sample.dcapture.db.service.*;

import javax.json.Json;
import javax.json.JsonReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Registry implements DispatcherRegistry {
    private static final Logger logger = Logger.getLogger(Registry.class.getName());

    @Override
    public void inject(Binder binder) {
        try {
            BaseSettings settings = BaseSettings.load(Registry.class);
            Localization localization = Localization.development(Registry.class);
            binder.bind(BaseSettings.class).toInstance(settings);
            binder.bind(Localization.class).toInstance(localization);
            SqlDatabase database = getDatabase(settings);
            binder.bind(SqlDatabase.class).toInstance(database);
            binder.bind(KeySequence.class).toInstance(getKeySequence(database));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
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

    private SqlDatabase getDatabase(BaseSettings settings) throws SQLException {
        String[] values = settings.getDatabase().trim().split(" ");
        if (4 > values.length) {
            throw new IllegalArgumentException("Database user and password should be encrypted, and format is : " +
                    "name [space] url [space] userName [space] password");
        }
        if (values[0].trim().isEmpty()) {
            throw new IllegalArgumentException("Database name not valid");
        }
        if (values[1].trim().isEmpty()) {
            throw new IllegalArgumentException("Database url not valid");
        }
        if (values[2].trim().isEmpty()) {
            throw new IllegalArgumentException("Database user not valid");
        }
        if (values[3].trim().isEmpty()) {
            throw new IllegalArgumentException("Database password not valid");
        }
        final String name = values[0].trim();
        final String url = values[1].trim();
        String dbCfgPath = "/" + name + ".json";
        logger.severe("Database configuration reading from " + Registry.class.getResource(dbCfgPath));
        JsonReader systemDBReader = Json.createReader(Registry.class.getResourceAsStream("/system-db.json"));
        JsonReader sampleDBReader = Json.createReader(Registry.class.getResourceAsStream(dbCfgPath));
        SqlTableBuilder tableBuilder = new SqlTableBuilder(name, url);
        tableBuilder.load(systemDBReader.readArray(), sampleDBReader.readArray());
        SqlDatabase database = tableBuilder.getDatabase(BaseSettings.decode(values[2].trim()),
                BaseSettings.decode(values[3].trim()));
        database.getForwardTool().executeUpdate().commit();
        return database;
    }

    private KeySequence getKeySequence(SqlDatabase database) throws SQLException {
        KeySequence keySequence = new KeySequence(database);
        keySequence.setSequence("expense", KeySequence.MONTHLY, "E");
        keySequence.commit();
        return keySequence;
    }
}
