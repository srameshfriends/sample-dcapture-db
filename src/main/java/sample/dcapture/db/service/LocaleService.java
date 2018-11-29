package sample.dcapture.db.service;

import dcapture.io.HttpPath;
import dcapture.io.Localization;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;
import java.util.Properties;

@HttpPath(value = "/locale", secured = false)
public class LocaleService {
    private Localization locale;

    @Inject
    public LocaleService(Localization locale) {
        this.locale = locale;
    }

    @HttpPath(value = "/default", secured = false)
    private JsonObject locale() {
        Properties prop = locale.getProperties(null);
        JsonObjectBuilder result = Json.createObjectBuilder();
        for (Map.Entry<Object, Object> locale : prop.entrySet()) {
            result.add((String) locale.getKey(), (String) locale.getValue());
        }
        return result.build();
    }
}
