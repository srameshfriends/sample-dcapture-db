package sample.dcapture.sql.service;

import dcapture.io.Localization;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.util.Map;
import java.util.Properties;

@Path("/locale")
public class LocaleService extends SqlMapper {
    private Localization locale;

    @Inject
    public LocaleService(Localization locale) {
        this.locale = locale;
    }

    @Path("/default")
    private JsonObject locale() {
        Properties prop = locale.getProperties(null);
        JsonObjectBuilder result = Json.createObjectBuilder();
        for (Map.Entry<Object, Object> locale : prop.entrySet()) {
            result.add((String) locale.getKey(), (String) locale.getValue());
        }
        return result.build();
    }
}
