grammar Lambda;

/* =======================
 * Parser rules
 * ======================= */
expr
    : (macroInvocation
    | parameter
    | property
    | tuple
    | trivia
    | string
    | atom)+
    ;

macroInvocation
    : property tuple
    ;

parameter
    : ParameterLiteral
    ;

property
    : PropertyLiteral
    ;

trivia
    : Tribia
    ;

tuple
    : '(' expr (',' expr)* ')'
    ;

atom
    : Identifier
    ;

string
    : StringLiteral
    ;

/* =======================
 * Lexer rules
 * ======================= */

PropertyLiteral
    : Alias '.' Identifier
    ;

ParameterLiteral
    : '?' NumberLiteral
    ;

Alias
    : '@' | ('@' Identifier)+
    ;

Identifier
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

Tribia
    : ~[a-zA-Z,?@()]+
    ;

StringLiteral
    : '"' ( ~["\\] | '\\' . )* '"'
    | '\'' ( ~['\\] | '\\' . )* '\''
    ;

NumberLiteral
    : [0-9]+
    ;
