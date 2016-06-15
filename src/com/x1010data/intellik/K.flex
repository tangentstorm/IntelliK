package com.x1010data.intellik;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

import com.x1010data.intellik.psi.KTypes;

%%

%class KLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

Digit      = [0-9]
Alpha      = [a-zA-Z]
Nat        = {Digit}+

WHITESPACE = [\ \t\f]
NEWLINE    = \n|\r|\r\n
COMMAND    = \\ [0?':.bcdve\\ilmprts_tw]
COMMENT    = "/"[^\r\n]*
SEMI       = ";"
COLON      = ":"
DOT        = "."
LPAREN     = "("
RPAREN     = ")"
LBRACE     = "{"
RBRACE     = "}"
LBRACK     = "["
RBRACK     = "]"
PRIM       = [~!@#$%\^&*+-<>|,?] ":"? // semi, colon, dot are also primitives, but handled specially
ADV        = ['/\\] ":" ?
INT        = "-"? {Nat}
RAT        = {INT}\.{Nat}
BUILTIN    = "_" ("bin"|"di"|"dv"|"dvl"|"draw"|"gtime"|"ic"|"ci"|"jd"|"dj"|"lsq"|"dot"|"mul"|"inv"|"in"|"lin"
                 |"sv"|"sm"|"ss"|"ssr"|"vs"|"abs"|"floor"|"sin"|"cos"|"tan"|"sinh"|"cosh"|"tanh"
                 |"exp"|"log"|"sqr"|"sqrt")

IDENT      = {Alpha} ({Alpha}|{Digit}|"_")*
STRING     = "\"" [^\"]* "\""
SYM        = "`" ({IDENT} | {STRING})?

// INLINE means that the / character behaves as an adverb (unlike INLINE, where it starts a comment)
%state INLINE

%%

{LPAREN}                                   { yybegin(INLINE); return KTypes.LPAREN; }
{RPAREN}                                   { yybegin(INLINE); return KTypes.RPAREN; }
{LBRACE}                                   { yybegin(INLINE); return KTypes.LBRACE; }
{RBRACE}                                   { yybegin(INLINE); return KTypes.RBRACE; }
{LBRACK}                                   { yybegin(INLINE); return KTypes.LBRACK; }
{RBRACK}                                   { yybegin(INLINE); return KTypes.RBRACK; }

{SEMI}                                     { yybegin(INLINE); return KTypes.SEMI; }
{COLON}                                    { yybegin(INLINE); return KTypes.COLON; }
{DOT}                                      { yybegin(INLINE); return KTypes.DOT; }
{PRIM}                                     { yybegin(INLINE); return KTypes.PRIM; }

"if"                                       { yybegin(YYINITIAL); return KTypes.IF; }
"do"                                       { yybegin(YYINITIAL); return KTypes.DO; }
"while"                                    { yybegin(YYINITIAL); return KTypes.WHILE; }

{BUILTIN}                                  { yybegin(INLINE); return KTypes.BUILTIN; }
{IDENT}                                    { yybegin(INLINE); return KTypes.IDENT; }

{STRING}                                   { yybegin(INLINE); return KTypes.STRING; }
{SYM}                                      { yybegin(INLINE); return KTypes.SYM; }
{INT}                                      { yybegin(INLINE); return KTypes.INT; }
{RAT}                                      { yybegin(INLINE); return KTypes.RAT; }

{ADV}                                      { yybegin(INLINE); return KTypes.ADV; }

<YYINITIAL> {COMMENT}                      { yybegin(YYINITIAL); return KTypes.COMMENT; }

{COMMAND}                                  { yybegin(YYINITIAL); return KTypes.COMMAND; }
{NEWLINE}                                  { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
{WHITESPACE}                               { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
.                                          { return TokenType.BAD_CHARACTER; }
