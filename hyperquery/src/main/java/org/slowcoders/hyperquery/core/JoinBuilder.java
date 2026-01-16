package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;

public class JoinBuilder {
    private final String[] columns;

    public JoinBuilder(String[] columns) {
        this.columns = columns;
    }

    public QJoin toOne(HModel target, String... joinColumns) {
        return createJoin(target, joinColumns, true);
    }


    public QJoin toMany(HModel target, String... joinColumns) {
        return createJoin(target, joinColumns, false);
    }

    private QJoin createJoin(HModel target, String[] joinColumns, boolean b) {
        if (columns.length != joinColumns.length) {
            throw new IllegalArgumentException("Invalid join columns");
        }
        String joinOn;
        if (columns.length == 0) {
            joinOn = "true";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < joinColumns.length; i++) {
                sb.append(columns[i]).append(" = ").append(joinColumns[i]);
                sb.append("\n AND ");
            }
            sb.setLength(sb.length() - 5);
            joinOn = sb.toString();
        }
        return new QJoin(target, joinOn, false);
    }
}

