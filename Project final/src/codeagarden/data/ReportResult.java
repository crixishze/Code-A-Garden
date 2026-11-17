package codeagarden.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The answer to a report: the column names and the rows, ready to put in a table on screen. */
public final class ReportResult {

    private final List<String> columns;
    private final List<List<Object>> rows;

    /** Keeps read-only copies of the column names and the rows. */
    public ReportResult(List<String> columns, List<List<Object>> rows) {
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    /** The column names, in the order of the SELECT. */
    public List<String> getColumns() {
        return columns;
    }

    /** Every row; each row has one value per column. */
    public List<List<Object>> getRows() {
        return rows;
    }

    /** True if the query found nothing. */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
