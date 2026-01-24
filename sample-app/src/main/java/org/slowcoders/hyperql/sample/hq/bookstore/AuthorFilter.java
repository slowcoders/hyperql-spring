package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperquery.core.PredicateBuilder;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.impl.HCondition;

@Getter
@Setter
public class AuthorFilter extends QFilter<Author> {
    @Predicate("@.name ilike '%' || ? || '%'")
    private String name;

    static PredicateBuilder<AuthorFilter> predicateBuilder = new PredicateBuilder<>() {
        @Override
        public HCondition build() {
            return PredicateSet(LogicalOp.AND)
                    .add("@.p1 = ?", "p1", _mustNotNull)
                    .add(PredicateSet(QFilter.LogicalOp.OR)
                            .add("@.p2 = ?", "p2", _notNull)
                            .add("@.p3 = ?", "p3", _notNull)
                            .add(PredicateSet(QFilter.LogicalOp.AND)
                                    .add("@.p2 = ?", "p2", _notNull)
                                    .add("@.p3 = ?", "p3", _notNull)
                                    .mustNotEmpty()
                            )
                    )
                    .add(PropertyCase("p4", _notEmpty)
                            .when("a"::equals, "@.p4 = ?")
                            .equals("b", "@.p4 = ?")
                            .when("b"::equals, PredicateSet(QFilter.LogicalOp.AND)
                                    .add("@.p5 = ?", "p5", _notEmpty)
                                    .add("@.p5 = ?", "p6", _notEmpty)
                            )
                            .equals("c", PredicateSet(QFilter.LogicalOp.AND)
                                    .add("@.p5 = ?", "p5", _notEmpty)
                                    .add("@.p5 = ?", "p6", _notEmpty)
                            )
                            .otherwise("@p4 = ?")
                    )
                    .add(GeneralCase(f -> f, _notNull)
                            .when(f -> "a".equals(f.name), "@.p4 = #{name}")
                            .when(f -> "b".equals(f.name), "@.p5 = #{name}")
                            .otherwise("@p4 = ?")
                    );
        }
    };
}
