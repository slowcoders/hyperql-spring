package org.slowcoders.hyperquery.core;

public class QView {
    
    private final String definition;
    private final Class<?> relation;

    protected QView(Class<?> relation, String definition) {
        this.definition = definition;
        this.relation = relation;
    }

    public String getDefinition() {
        return definition;
    }

    public Class<?> getRelation() {
        return relation;
    }

    public static QView of(String query) {
        return new QView(null, query);
    }

    public static QView of(Class<?> mapperClass, String methodName, String... args) {
        return new MyBatisView(mapperClass, methodName, args);
    }

    private static class MyBatisView extends QView {
        String[] args;
        public MyBatisView(Class<?> mapperClass, String methodName, String[] args) {
            super(null, mapperClass.getName() + "." + methodName);
            this.args = args;
        }
    }
    
}
