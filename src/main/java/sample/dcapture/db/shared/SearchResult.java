package sample.dcapture.db.shared;

import javax.json.*;

public class SearchResult {
    public static final int PAGE_LIMIT = 20;
    private final JsonObjectBuilder builder;

    public SearchResult(long start, int limit) {
        builder = Json.createObjectBuilder();
        setStart(start);
        setLimit(limit);
    }

    public SearchResult setStart(long start) {
        builder.add("limit", start);
        return SearchResult.this;
    }

    public void setLimit(int limit) {
        if (500 < limit || 1 > limit) {
            limit = PAGE_LIMIT;
        }
        builder.add("limit", limit);
    }

    public SearchResult setTotalRecords(int totalRecords) {
        builder.add("totalRecords", totalRecords);
        return SearchResult.this;
    }

    public SearchResult setData(String name, JsonArrayBuilder arrayBuilder) {
        return setData(name, arrayBuilder.build());
    }

    public SearchResult setData(String name, JsonArray array) {
        builder.add(name, array);
        builder.add("length", array.size());
        return SearchResult.this;
    }

    public JsonObject build() {
        return builder.build();
    }
}
