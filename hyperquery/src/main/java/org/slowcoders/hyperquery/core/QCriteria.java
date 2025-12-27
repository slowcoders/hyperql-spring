package org.slowcoders.hyperquery.core;

import java.util.ArrayList;

class QCriteria extends ArrayList<String> {

    public enum LogicalOp {
        AND, OR, NOT_AND, NOT_OR;
    }

    LogicalOp type;

    QCriteria(LogicalOp type) {
        this.type = type;
    }

    public static QCriteria buildCriteria(QFilter<?> filter, String s) {
        return new CriteriaBuilder(filter, s).build();
    }

    public String toString() {
        if (this.isEmpty()) return "true";

        String delimiter = "";
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case NOT_AND:
                sb.append("NOT ");
            case AND:
                delimiter = " AND ";
                break;
            case NOT_OR:
                sb.append("NOT ");
            case OR:
                delimiter = " OR ";
        }
        sb.append("(");
        for (String predication : this) {
            sb.append(predication).append(delimiter);
        }
        sb.setLength(sb.length() - delimiter.length());
        sb.append(")");
        return sb.toString();
    }

}

