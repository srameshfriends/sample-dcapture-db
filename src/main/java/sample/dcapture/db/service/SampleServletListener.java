package sample.dcapture.db.service;

import dcapture.db.core.SqlContext;
import dcapture.db.core.SqlDatabase;
import dcapture.db.core.SqlFactory;
import dcapture.db.postgres.PgDatabase;
import dcapture.io.AppSettings;
import dcapture.io.DispatcherListener;
import dcapture.io.IOStream;
import dcapture.io.Localization;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SampleServletListener extends DispatcherListener implements IOStream {

    @Override
    protected void configureBinder(Binder binder) {
        try {
            URL configUrl = SampleServletListener.class.getResource("/config");
            AppSettings settings = AppSettings.load(getResourceAsStream(AppSettings.PATH));
            SqlContext sqlContext = SqlFactory.getSqlContext(settings.getDatabase(), new File(configUrl.getFile()));
            SqlDatabase database = new PgDatabase(sqlContext, SqlFactory.getPoolingDataSource(settings.getDatabase()));
            binder.bind(AppSettings.class).toInstance(settings);
            binder.bind(Localization.class).toInstance(Localization.load(settings, SampleServletListener.this));
            binder.bind(SqlDatabase.class).toInstance(database);
            binder.bind(SqlContext.class).toInstance(sqlContext);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<Class<?>> getHttpPathList() {
        List<Class<?>> list = new ArrayList<>();
        list.add(LocaleService.class);
        list.add(SessionService.class);
        list.add(AppsUserService.class);
        list.add(CurrencyService.class);
        list.add(ExpenseCategoryService.class);
        list.add(ExpenseService.class);
        list.add(ConsoleService.class);
        return list;
    }

    @Override
    protected void initialized(ServletContext context) {
        Path path = Paths.get(System.getProperty("user.home"), "dcapture", "multipart");
        context.setInitParameter("multipart-config-location", path.toString());
        context.setInitParameter("multipart-config-max-file-size", "5242880L"); // 5MP
        context.setInitParameter("multipart-config-max-request-size", "20971520L"); // 20MP
        context.setInitParameter("multipart-config-file-size-threshold", "0");
    }

    @Override
    protected void destroyed(ServletContext context) {
        try {
            Injector injector = (Injector) context.getAttribute(Injector.class.getName());
            injector.getInstance(SqlDatabase.class).close();
            Injectors.dispose(injector);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getResourceAsStream(String file) {
        return SampleServletListener.class.getResourceAsStream(file);
    }
}
