package org.slowcoders.hyperql.parser;

import org.slowcoders.hyperql.HyperSelect;
import org.slowcoders.hyperql.schema.QSchema;

import java.util.HashMap;

public abstract class EntityFilter {

    private final EntityFilter parent;
    private final PredicateSet predicates;
    protected final HashMap<String, EntityFilter> subFilters = new HashMap<>();

    protected EntityFilter(EntityFilter parentNode) {
        this.predicates = new PredicateSet(Conjunction.AND, this);
        this.parent = parentNode;
    }

    public abstract String getMappingAlias();

    public final boolean isJsonNode() { return getSchema() == null; }

    /** return null if schemaless node. cf) json node */
    public QSchema getSchema() { return null; }

    public boolean isEmpty() {
        if (!predicates.isEmpty()) return false;
        for (EntityFilter subNode: subFilters.values()) {
            if (!subNode.isEmpty()) return false;
        }
        return true;
    }

    public Expression getPredicates() {
        return predicates;
    }

    final PredicateSet getPredicateSet() {
        return predicates;
    }

    public EntityFilter getParentNode() { return this.parent; }

    public HyperFilter getRootNode() {
        return parent.getRootNode();
    }

    protected abstract EntityFilter makeSubNode(String key, HqlParser.NodeType nodeType);

    protected abstract String getColumnName(String key);

    public TableFilter asTableFilter() { return null; }


    public final EntityFilter getFilterNode(String key, HqlParser.NodeType nodeType) {
        EntityFilter scope = this;
        for (int p; (p = key.indexOf('.')) > 0; ) {
            QSchema schema = scope.getSchema();
            if (schema != null && schema.hasColumn(key)) {
                return scope;
            }
            String token = key.substring(0, p);
            scope = scope.makeSubNode(token, HqlParser.NodeType.Entity);
            key = key.substring(p + 1);
        }
        scope = scope.makeSubNode(key, nodeType);
        return scope;
    }

    public Iterable<EntityFilter> getChildNodes() {
        return subFilters.values();
    }

    protected void addSelectedColumn(String key) {

    }

    public boolean visitPredicates(PredicateVisitor visitor) {
        Expression ps = this.getPredicates();
        if (ps.isEmpty()) return false;
        ps.accept(visitor);
        return true;
    }

    protected void addSelection(HyperSelect.ResultMap resultMap) {}

    public String getSqlToCheckReadable() {
        return null;
    }
}
