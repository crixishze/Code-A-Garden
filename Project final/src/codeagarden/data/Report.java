package codeagarden.data;

/**
 * One report on the REPORTS screen: a question in plain words, the SQL features it shows,
 * and (only for SaveRepository) the SQL itself. The screen never sees or builds SQL.
 */
public final class Report {

    private final String title;
    private final String features;
    private final String inputPrompt;   // null = the report needs no typed value
    final String sql;                   // package-private: only SaveRepository may read it

    Report(String title, String features, String inputPrompt, String sql) {
        this.title = title;
        this.features = features;
        this.inputPrompt = inputPrompt;
        this.sql = sql;
    }

    /** The business question, shown in the list, e.g. "Crops ready to harvest". */
    public String getTitle() {
        return title;
    }

    /** Which SQL features the query uses, e.g. "JOIN of 4 tables, WHERE". */
    public String getFeatures() {
        return features;
    }

    /** True if the player must type a value first (the "Search players by name" report). */
    public boolean needsInput() {
        return inputPrompt != null;
    }

    /** The question asked before running, e.g. "Show players whose name starts with:". */
    public String getInputPrompt() {
        return inputPrompt;
    }

    /** The title is what the list on the REPORTS screen shows. */
    @Override
    public String toString() {
        return title;
    }
}
