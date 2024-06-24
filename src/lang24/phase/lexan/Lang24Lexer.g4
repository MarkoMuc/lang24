lexer grammar Lang24Lexer;

@header {
	package lang24.phase.lexan;
	import lang24.common.report.*;
	import lang24.data.token.*;
}

@members {
    @Override
	public LocLogToken nextToken() {
		return (LocLogToken) super.nextToken();
	}

	public void ThrowNewError(String message) throws Report.Error{
	    throw new Report.Error(new Location(_tokenStartLine, _tokenStartCharPositionInLine,
	        getLine(), getCharPositionInLine()), message);
	}
}

fragment LETTER: ([a-z] | [A-Z]) ;
fragment CHARS: (' '..'&' | '('..'[' | ']'..'~' | '\\\''| '\\\\') ;
fragment CHARSS: (' '..'!' | '#'..'[' | ']'..'~' | '\\"'| '\\\\') ;
fragment DIGITS: [0-9] ;
fragment HEX : DIGITS | [A-F] ;

// Literals

NUML: DIGITS+ ;
CHARL: ['] (CHARS | '\\n' | '\\' HEX HEX) ['] ;
STRL: ["] (CHARSS | '\\n' | '\\' HEX HEX)* ["] ;

// Errors
ERRORC:
		(['] ('\\' HEX  ~[0-9A-F] | '\\' ~[0-9A-F] HEX) [']
		    {if(true)ThrowNewError("Character Literal Error: Hex Character Representation Is Not Valid.");})
		| (['] '\\' ~('n'|'\\'|'\'') CHARS*[']
		    {if(true)ThrowNewError("Character Literal Error: Not A Valid Escape Sequence");})
        | (['] (CHARS | '\\n' | '\\' HEX HEX)+ [']
            {if(true)ThrowNewError("Character Literal Error: Only One Character Allowed");})
        | (['] (CHARS | '\\n' | '\\' HEX HEX)*
            {if(true)ThrowNewError("Character Literal Error: No Closing \'");})
		| (['] [']
		    {if(true)ThrowNewError("Character Literal Error: Empty Character Not Allowed");})
		;

ERRORS:
        (["] (CHARSS | '\\n' | '\\' HEX HEX)*
            {if(true)ThrowNewError("String Literal Error: No Closing \"");})
		| (["] (CHARSS | '\\n' | ('\\' HEX ~[0-9A-F] | '\\' ~[0-9A-F] HEX) | '\\' HEX HEX)+ ["]
		    {if(true)ThrowNewError("String Literal Error: Hex Character Representation Is Not Valid.");})
		| (["] (CHARSS |  '\\' ~('n'|'\\'|'"') | '\\n' |'\\' HEX HEX)+ ["]
		    {if(true)ThrowNewError("String Literal Error: Not A Valid Escape Sequence");})
		;

// Symbol tokens
LPAR: '(' ;
RPAR: ')' ;
LCBRAC: '{' ;
RCBRAC: '}' ;
LSBRAC: '[' ;
RSBRAC: ']' ;
DOT: '.' ;
COMMA: ',' ;
COLON: ':' ;
SEMI: ';' ;
EQU: '==' ;
NEQU: '!=' ;
LT: '<' ;
GT : '>' ;
LTE: '<=' ;
GTE: '>=' ;
MUL: '*' ;
DIV: '/' ;
MOD: '%' ;
PLUS: '+' ;
MINUS: '-' ;
POW: '^' ;
ASSIGN: '=' ;

// Keyword tokens
KAND: 'and' ;
KBOOL: 'bool' ;
KCHAR: 'char' ;
KELSE: 'else' ;
KIF: 'if' ;
KINT: 'int' ;
KNIL: 'nil' ;
KNONE: 'none' ;
KNOT: 'not' ;
KOR: 'or' ;
KSIZEOF: 'sizeof' ;
KTHEN: 'then' ;
KRETURN: 'return' ;
KVOID: 'void' ;
KWHILE: 'while' ;
KFOR: 'for' ;
KTRUE: 'true';
KFALSE: 'false';

// Identifiers
ID: (LETTER | '_') (LETTER | DIGITS | '_')* ;

// Comments
COMMENT: '#' ~[\r\n]* -> skip ;

// ADDONS
DEC_VEC: '@vector';

ERRORD_V:
     ([@] (LETTER | '_') (LETTER | DIGITS | '_')*
     {if(true)ThrowNewError("Invalid Decorator Identifier: Illegal characters");})
     ;

// Whitespace
WS: [ \n\r]+ -> skip ;
TAB : '\t' {setCharPositionInLine(getCharPositionInLine() + (8 - getCharPositionInLine() % 8));} -> skip ;

// Other symbols
ERROR: . {if(true)ThrowNewError("Undefined Symbol");} ;