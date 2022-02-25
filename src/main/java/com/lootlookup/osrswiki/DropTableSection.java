package com.lootlookup.osrswiki;

import java.util.Map;

public class DropTableSection {
    private String header;
    private Map<String, WikiItem[]> table;

    public DropTableSection() {
        this.header = header;
        this.table = table;
    }
    public DropTableSection(String header, Map<String, WikiItem[]> table) {
        this.header = header;
        this.table = table;
    }

    public void setHeader(String newHeader) {
        this.header = newHeader;
    }

    public void setTable(Map<String, WikiItem[]> newTable) {
        this.table = newTable;
    }

    public String getHeader() {
        return header;
    }

    public Map<String, WikiItem[]> getTable() {
        return table;
    }
}
