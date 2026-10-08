package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperquery.core.PredicateBuilder;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.impl.HCondition;
import org.slowcoders.hyperquery.impl.SqlBuilder;

@Getter
@Setter
public class AuthorFilter extends QFilter<Author> {
    @Predicate("@.name ilike '%' || ? || '%'")
    private String name;

    private String filter_type;

    private String p1;
    private String p2;
    private String p3;

    /*
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


    static PredicateBuilder<AuthorFilter> predicateBuilder2 = new PredicateBuilder<AuthorFilter>() {
        @Override
        public HCondition build() {
            return _AND_(
                    q("@.p1 = #{p1:notEmpty}"),
                    IF(r -> "any".equals(r.filter_type), _OR_notEmpty(
                            q("@.p2 = #{p2:notEmpty?}"),
                            q("@.p3 = #{p3:notEmpty?}")
                    )).ELSE_IF(r -> "any".equals(r.filter_type), _OR_notEmpty(
                            q("@.p2 = ?{p2:notEmpty}"),
                            q("@.p3 = ?{p3:notEmpty}")
                    )),
                    IF(r -> "between".equals(r.filter_type),
                            q("@.p2 between #{p2:notEmpty!} and #{p3:notEmpty!}") // --> 둘 다 조건 만족
                    ).ELSE(_AND_(
                            q("@.p2 = #{p2:notEmpty!}"),
                            q("@.p3 = #{p3:notEmpty!}")
                    )),
                    SWITCH(r -> r.filter_type)
                            .WHEN("any", _OR_notEmpty(
                                    q("@.p2 = #{p2:notEmpty?}"),
                                    q("@.p3 = #{p3:notEmpty?}")
                            ))
                            .WHEN(v -> "between".equals(v), _AND_(
                                    q("@.p2 between #{p2:notEmpty!} and #{p3:notEmpty!}") // --> 둘 다 조건 만족
                            ))
                            .WHEN("both"::equals, _AND_(
                                    q("@.p2 = #{p2:notEmpty!}"),
                                    q("@.p3 = #{p3:notEmpty!}")
                            ))
                            .OTHERWISE(_AND_(
                                    q("@.p2 = #{p2:notEmpty!}"),
                                    q("@.p3 = #{p3:notEmpty!}")
                            )),
                    _OR_(

                    )


            );

        }
    };

    static PredicateBuilder<AuthorFilter> predicateBuilder3 = new PredicateBuilder<AuthorFilter>() {
        @Override
        public HCondition build() {
            return _AND_(
                    q("@.p1 = #{p1:notEmpty}"),
                    _if_(r -> "any".equals(r.filter_type), _OR_(
                            q("@.p2 = #{p2:notEmpty?}"),
                            q("@.p3 = #{p3:notEmpty?}")
                    )).else_if(r -> "any".equals(r.filter_type), _OR_(
                        q("@.p2 = ?{p2:notEmpty}"),
                        q("@.p3 = ?{p3:notEmpty}")
                    )),
                    _if_(r -> "between".equals(r.filter_type),
                            q("@.p2 between #{p2:notEmpty!} and #{p3:notEmpty!}") // --> 둘 다 조건 만족
                    ).otherwise(_AND_(
                        q("@.p2 = #{p2:notEmpty!}"),
                        q("@.p3 = #{p3:notEmpty!}")
                    )),
                    _switch_(r -> r.filter_type)
                            .when("any", _OR_(
                                    q("@.p2 = #{p2:notEmpty?}"),
                                    q("@.p3 = #{p3:notEmpty?}")
                            ))
                            .when(v -> "between".equals(v), _AND_(
                                    q("@.p2 between #{p2:notEmpty!} and #{p3:notEmpty!}") // --> 둘 다 조건 만족
                            ))
                            .when("both"::equals, _AND_(
                                    q("@.p2 = #{p2:notEmpty!}"),
                                    q("@.p3 = #{p3:notEmpty!}")
                            ))
                            .otherwise(_AND_(
                                    q("@.p2 = #{p2:notEmpty!}"),
                                    q("@.p3 = #{p3:notEmpty!}")
                            )),
                    _OR_(

                    )

            );

        }
    };
    */

    public PredicateBuilder<AuthorFilter> createPredicateBuilder(SqlBuilder sqlBuilder) {
        return new PredicateBuilder<AuthorFilter>(sqlBuilder) {
            @Override
            public HCondition<AuthorFilter> build() {
                return _OR_(
                        Q("@.profile->>'p1' = #{p1!} /*first*/"),
                        If(r -> "any".equals(r.filter_type), _OR_(
                                Q("@.profile->>'p2' = #{p2?} /*if-1-1*/"),
                                Q("@.profile->>'p3' = #{p3?} /*if-1-1*/")
                        )),
                        If(r -> "between".equals(r.filter_type),
                                Q("@.profile->>'p2' between #{p2!} and #{p3!} /*between*/") // --> 둘 다 조건 만족
                        ).Else(_OR_notEmpty(
                                Q("@.profile->>'p2' = #{p2!} /*between-else-1*/"),
                                Q("@.profile->>'p3' = #{p3!} /*between-else-2*/")
                        )),
                        Switch(r -> r.filter_type)
                                .When("any", _OR_(
                                        Q("@.profile->>'p2' = #{p2?} /*switch-any-1*/"),
                                        Q("@.profile->>'p3' = #{p3?} /*switch-any-2*/")
                                ))
                                .When(v -> "between".equals(v), _AND_(
                                        Q("@.profile->>'p2' between #{p2!} and #{p3!} /*switch-between-1*/") // --> 둘 다 조건 만족
                                ))
                                .When("both"::equals, _AND_(
                                        Q("@.profile->>'p2' = #{p2!} /*switch-both-1*/"),
                                        Q("@.profile->>'p3' = #{p3!} /*switch-both-2*/")
                                ))
                                .Otherwise(_AND_(
                                        Q("@.profile->>'p2' = #{p2!} /*otherwise-1*/"),
                                        Q("@.profile->>'p3' = #{p3!} /*otherwise-2*/")
                                ))
//                        q("""
//                            case @.salesCategory
//                                when 'New' then
//                                    @.profile->>'p4' = #{p4!}
//                                when 'Used' then
//                                    @.profile->>'p5' = #{p5!}
//                                else
//                                    @.profile->>'p6' = #{p6!}
//                            end
//                        """)
                );
            }
        };
    }
}
