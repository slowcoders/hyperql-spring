grammar Predicate;

/* =======================
 * Parser rules
 * ======================= */
parse
    : expr EOF
    ;

expr
    : (macroInvocation
    | parameter
    | mapperParameter
    | property
    | joinTargetAttr
    | tuple
    | trivia
    | string
    | atom)+
    ;

macroInvocation
    : property tuple
    ;

parameter
    : '?' ('.' Identifier)*
    ;

mapperParameter
    : '#{' QualifiedIndetifier '}'
    ;

property
    : PropertyLiteral
    ;

joinTargetAttr
    : JoinTargetAttrLiteral
    ;

trivia
    : Tribia
    ;

tuple
    : '(' expr (',' expr)* ')'
    ;

atom
    : QualifiedIndetifier
    ;

string
    : StringLiteral
    ;

/* =======================
 * Lexer rules
 * ======================= */

PropertyLiteral
    : Alias '.' QualifiedIndetifier
    ;

JoinTargetAttrLiteral
    : '#.' QualifiedIndetifier
    ;

Alias
    : '@' | ('@' Identifier)+
    ;

QualifiedIndetifier
    : Identifier ('.' Identifier)*
    ;

Identifier
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

Tribia
    : ~[a-zA-Z,?@#(){}'"]+
    ;

StringLiteral
    : '"' ( ~["\\] | '\\' . )* '"'
    | '\'' ( ~['\\] | '\\' . )* '\''
    ;

