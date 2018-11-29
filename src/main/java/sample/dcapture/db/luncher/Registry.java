package sample.dcapture.db.luncher;

import dcapture.db.core.DataSet;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlParser;
import dcapture.io.AppSettings;
import dcapture.io.DispatcherRegistry;
import dcapture.io.IOStream;
import dcapture.io.Localization;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;
import sample.dcapture.db.service.*;
import sample.dcapture.db.shared.DataUtils;
import sample.dcapture.db.shared.KeySequence;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Registry implements DispatcherRegistry, IOStream {
    private AppSettings settings;

    @Override
    public void bind(Binder binder) {
        try {
            binder.bind(AppSettings.class).toInstance(getSettings());
            binder.bind(Localization.class).toInstance(Localization.load(getSettings(), Registry.this));
            binder.bind(SqlDatabase.class).toInstance(DataUtils.loadDatabase(getSettings()));
            binder.bind(SqlParser.class).toInstance(new SqlParser());
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
        list.add(ExpenseCategoryService.class);
        list.add(ExpenseService.class);
        return list;
    }

    @Override
    public void contextInitialized(ServletContext context) {
        Injector injector = (Injector) context.getAttribute(Injector.class.getName());
        SqlDatabase database = injector.getInstance(SqlDatabase.class);
        DataUtils.executeForwardQueries(database);
        database.transact(query -> {
            List<DataSet> keySequenceList = DataUtils.loadKeySequences(injector.getInstance(AppSettings.class));
            KeySequence keySequence = KeySequence.create(query);
            keySequence.save(keySequenceList);
            return true;
        });
    }

    @Override
    public void contextDestroyed(ServletContext context) {
        try {
            Injector injector = (Injector) context.getAttribute(Injector.class.getName());
            Injectors.dispose(injector);
            injector.getInstance(SqlDatabase.class).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        return Registry.class.getResourceAsStream(path);
    }

    public AppSettings getSettings() {
        if (settings == null) {
            settings = AppSettings.load(getInputStream(AppSettings.PATH));
        }
        return settings;
    }
}
