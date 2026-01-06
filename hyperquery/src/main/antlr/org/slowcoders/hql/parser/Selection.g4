grammar Selection;

/* =======================
 * Parser rules
 * ======================= */
expr
    : (macroInvocation
    | mapperParameter
    | property
    | tuple
    | trivia
    | string
    | atom)+
    ;

macroInvocation
    : property tuple
    ;

mapperParameter
    : '#{' Identifier ('.' Identifier)* '}'
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

