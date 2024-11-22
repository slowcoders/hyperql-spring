package org.slowcoders.hyperql.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.HyperSelect;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QSchema;
import org.slowcoders.hyperql.schema.QResultMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HyperFilter extends TableFilter {

    private final ArrayList<QResultMapping> columnGroupMappings = new ArrayList<>();
    private int cntMappingAlias;

    private boolean selectAuto;
    private boolean enableJPQL;

    private static HqlParser parser = new HqlParser(new ObjectMapper());
    private ArrayList<Object> columnNameMappings;

    public HyperFilter(QSchema schema) {
        super(schema, "t_0");
        enableJPQL = schema.isJPARequired();
    }

    public static <ID> HyperFilter of(QSchema schema, ID id) {
        HyperFilter filter = new HyperFilter(schema);
        PredicateSet ps = filter.getPredicateSet();
        if (id instanceof Object[]) {
            Object[] ids = (Object[]) id;
            int idx = 0;
            for (QColumn pk : schema.getPKColumns()) {
                ps.add(PredicateFactory.IS.createPredicate(pk, ids[idx++]));
            }
        }
        else {
        QColumn first_pk = schema.getPKColumns().get(0);
            ps.add(PredicateFactory.IS.createPredicate(first_pk, id));
        }
        return filter;
    }

    public static <ID> HyperFilter of(QSchema schema, Iterable<ID> idList) {
        HyperFilter filter = new HyperFilter(schema);
        PredicateSet ps = filter.getPredicateSet();
        ID firstId = idList.iterator().next();
        if (firstId instanceof Object[]) {
            PredicateSet or_qs = new PredicateSet(Conjunction.OR, filter);
            ps.add(or_qs);
            for (ID id : idList) {
                PredicateSet new_ps = new PredicateSet(Conjunction.AND, filter);
                Object[] ids = (Object[]) id;
                int idx = 0;
                for (QColumn pk : schema.getPKColumns()) {
                    new_ps.add(PredicateFactory.IS.createPredicate(pk, ids[idx++]));
                }
                or_qs.add(new_ps);
            }
        } else {
            QColumn first_pk = schema.getPKColumns().get(0);
            ps.add(PredicateFactory.IS.createPredicate(first_pk, idList));
        }
        return filter;
    }

    public static HyperFilter of(QSchema schema, Map<String, Object> jsFilter) {
        HyperFilter filter = new HyperFilter(schema);
        parser.parse(filter.getPredicateSet(), jsFilter, true);
        return filter;
    }

    public static HyperFilter of(QSchema schema) {
        return new HyperFilter(schema);
    }


    public void setSelectedProperties(List<String> keys) {
        selectAuto = (keys == null || keys.size() == 0);
        if (selectAuto) {
            this.addSelection("*");
            return;
        }

        for (String k : keys) {
            this.addSelection(k.trim());
        }
    }


    private void addSelection(String key) {
        EntityFilter scope = this;
        for (int p; (p = key.indexOf('.')) > 0; ) {
            QSchema schema = scope.getSchema();
            if (schema != null && schema.hasColumn(key)) {
                break;
            }
            String token = key.substring(0, p);
            TableFilter table = scope.asTableFilter();
            if (table.isArrayNode()) {
                table.addSelectedColumn("0");
            }
            scope = scope.makeSubNode(token, HqlParser.NodeType.Entity);
            key = key.substring(p + 1);
        }

        TableFilter table = scope.asTableFilter();
        if (table != null) {
            if (table.getSchema().getEntityJoinBy(key) != null) {
                scope = table.makeSubNode(key, HqlParser.NodeType.Leaf).asTableFilter();
                key = "*";
            }
        }
        scope.addSelectedColumn(key);
    }


    public void setSelectedProperties(HyperSelect.ResultMap resultMap) {
        selectAuto = (resultMap == null || resultMap == HyperSelect.Auto.getPropertyMap());
        if (selectAuto) {
            this.addSelection("*");
            return;
        }

        this.addSelection(resultMap);
    }


    public HyperFilter getRootNode() {
        return this;
    }

    @Override
    public boolean isArrayNode() {
        return true;
    }

    public List<QResultMapping> getResultMappings() {
        if (columnGroupMappings.size() == 0) {
            gatherColumnMappings(columnGroupMappings);
        }
        return columnGroupMappings;
    }

    String createUniqueMappingAlias() {
        cntMappingAlias ++;
        if (cntMappingAlias < 10) {
            return "t_" + cntMappingAlias;
        } else {
            return "t" + cntMappingAlias;
        }
    }

    public boolean isJPQLEnabled() {
        return this.enableJPQL;
    }

    boolean isSelectAuto() {
        return selectAuto;
    }

    public void disableJPQL() {
        this.enableJPQL = false;
    }

    public Class getJpqlEntityType() {
        return enableJPQL ? getSchema().getEntityType() : null;
    }

    public void setColumnNameMappings(ArrayList<Object> columnNames) {
        this.columnNameMappings = columnNames;
    }

    public ArrayList<Object> getColumnNameMappings() {
        return columnNameMappings;
    }
}
