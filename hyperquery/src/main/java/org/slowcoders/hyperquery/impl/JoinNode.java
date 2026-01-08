package org.slowcoders.hyperquery.impl;

import java.util.ArrayList;
import java.util.HashMap;

class ViewNode {
    final HashMap<String, String> usedAttributes = new HashMap<>();

    final HashMap<String, JoinNode> joins = new HashMap<>();

    protected JoinNode getJoin(String aliasQualifier) {
        return joins.get(aliasQualifier);
    }

    protected void addJoin(String aliasQualifier, JoinNode node) {
        this.joins.put(aliasQualifier, node);
    }
}

class JoinNode {
    final HModel model;
    final String aliasQualifier;
    final ArrayList<ViewNode> views = new ArrayList<>();
    String joinCriteria;
    int attrLevel;

    JoinNode(HModel model, String aliasQualifier) {
        this.model = model;
        this.aliasQualifier = aliasQualifier;
    }

    final boolean addUsedAttribute(String alias, String expr) {
        for (int i = attrLevel; --i >= 0; ) {
            if (views.get(i).usedAttributes.containsKey(alias)) {
                return false;
            }
        }
        for (int i = views.size(); --i >= attrLevel; ) {
            if (views.get(i).usedAttributes.containsKey(alias)) {
                return true;
            }
        }
        ViewNode view = views.get(attrLevel);
        view.usedAttributes.put(alias, expr);
        return true;
    }

    int setAttrLevel(int level) {
        int old = this.attrLevel;
        this.attrLevel = level;
        return old;
    }

    public ViewNode pushAttrLevel() {
        if (attrLevel == views.size()) {
            views.add(new ViewNode());
        }
        return views.get(attrLevel++);
    }

    public void popAttrLevel() {
        attrLevel--;
    }
}
