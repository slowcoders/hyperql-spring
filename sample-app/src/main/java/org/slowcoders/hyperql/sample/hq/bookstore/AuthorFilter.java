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
    public PredicateBuilder<AuthorFilter> createPredicateBuilder(SqlBuilder sqlBuilder) {
        return new PredicateBuilder<AuthorFilter>(sqlBuilder) {
            @Override
            public HCondition<AuthorFilter> build() {
                return _OR_(
                        q("@.profile->>'p1' = #{p1!} /*first*/"),
                        If(r -> "any".equals(r.filter_type), _OR_(
                                q("@.profile->>'p2' = #{p2?} /*if-1-1*/"),
                                q("@.profile->>'p3' = #{p3?} /*if-1-1*/")
                        )),
                        If(r -> "between".equals(r.filter_type),
                                q("@.profile->>'p2' between #{p2!} and #{p3!} /*between*/") // --> 둘 다 조건 만족
                        ).Else(Optional._OR_(
                                q("@.profile->>'p2' = #{p2!} /*between-else-1*/"),
                                q("@.profile->>'p3' = #{p3!} /*between-else-2*/")
                        )),
                        Switch(r -> r.filter_type)
                                .When("any", _OR_(
                                        q("@.profile->>'p2' = #{p2?} /*switch-any-1*/"),
                                        q("@.profile->>'p3' = #{p3?} /*switch-any-2*/")
                                ))
                                .When(v -> "between".equals(v), _AND_(
                                        q("@.profile->>'p2' between #{p2!} and #{p3!} /*switch-between-1*/") // --> 둘 다 조건 만족
                                ))
                                .When("both"::equals, _AND_(
                                        q("@.profile->>'p2' = #{p2!} /*switch-both-1*/"),
                                        q("@.profile->>'p3' = #{p3!} /*switch-both-2*/")
                                ))
                                .Otherwise(_AND_(
                                        q("@.profile->>'p2' = #{p2!} /*otherwise-1*/"),
                                        q("@.profile->>'p3' = #{p3!} /*otherwise-2*/")
                                ))
// q("""
// case @.salesCategory
// when 'New' then
// @.profile->>'p4' = #{p4!}
// when 'Used' then
// @.profile->>'p5' = #{p5!}
// else
// @.profile->>'p6' = #{p6!}
// end
// """)
                );
            }
        };
    }
}
