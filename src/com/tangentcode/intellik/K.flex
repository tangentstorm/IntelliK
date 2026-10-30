package com.tangentcode.intellik;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

import com.tangentcode.intellik.psi.KTypes;
import com.tangentcode.intellik.dialect.KDialect;
import com.tangentcode.intellik.dialect.PrimitiveTable;

%%

%class KLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  // Built-in names (_bin, _sqrt, ...) come from the dialect's primitive table.
  private PrimitiveTable table = PrimitiveTable.forDialect(KDialect.DEFAULT);

  public KLexer(PrimitiveTable table) {
    this((java.io.Reader) null);
    this.table = table;
  }
%}

Digit      = [0-9]
Alpha      = [a-zA-Z]
Nat        = {Digit}+

WHITESPACE = [\ \t\f]
NEWLINE    = \n|\r|\r\n
COMMENT    = "/" [^\r\n]*
COMMAND    = "\\"[^\r\n]*
SEMI       = ";"
COLON      = ":"
DOT        = "."
LPAREN     = "("
RPAREN     = ")"
LBRACE     = "{"
RBRACE     = "}"
LBRACK     = "["
RBRACK     = "]"
MINUS      = "-"
PRIM       = ([~!@#$%\^&*+<>|,?=_] ":"?) | ({Digit} ":") // [;:.\-] are also primitives, but handled specially
ADV        = "'" | "':" | "/:" | "\\:"
INT        = {Nat} // handle negatives in the parser
RAT        = {INT}\.{Nat}
SYSNAME    = "_" {Alpha}+  // a builtin if the dialect's table says so, else "_" is a primitive

IF         = "if"
DO         = "do"
WHILE      = "while"
IDENT      = {Alpha} ({Alpha}|{Digit}|"_")*

STRING     = \" ("\\"[^\r\n]|[^\"\\])* \"
SYM        = "`" ({IDENT} | {STRING})?

// INLINE means that the / character behaves as an adverb (unlike INLINE, where it starts a comment)
%state INLINE
%state STRING

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
{MINUS}                                    { yybegin(INLINE); return KTypes.MINUS; }
{PRIM}                                     { yybegin(INLINE); return KTypes.PRIM; }

{IF}                                       { yybegin(YYINITIAL); return KTypes.IF; }
{DO}                                       { yybegin(YYINITIAL); return KTypes.DO; }
{WHILE}                                    { yybegin(YYINITIAL); return KTypes.WHILE; }

{SYSNAME}                                  { yybegin(INLINE);
                                             int n = table.longestBuiltinPrefix(yytext());
                                             if (n > 0) { yypushback(yylength() - n); return KTypes.BUILTIN; }
                                             yypushback(yylength() - 1); return KTypes.PRIM; }
{IDENT}                                    { yybegin(INLINE); return KTypes.IDENT; }

{STRING}                                   { yybegin(INLINE); return KTypes.STRING; }
{SYM}                                      { yybegin(INLINE); return KTypes.SYM; }
{INT}                                      { yybegin(INLINE); return KTypes.INT; }
{RAT}                                      { yybegin(INLINE); return KTypes.RAT; }

{ADV}                                      { return KTypes.ADV; }
<INLINE> "/"                               { return KTypes.ADV; }
<INLINE> "\\"                              { return KTypes.ADV; }
<YYINITIAL> {COMMENT}                      { yybegin(YYINITIAL); return KTypes.COMMENT; }
<YYINITIAL> {COMMAND}                      { yybegin(YYINITIAL); return KTypes.COMMAND; }

{NEWLINE}                                  { yybegin(YYINITIAL); return KTypes.NEWLINE; }
{WHITESPACE}                               { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

.                                          { return TokenType.BAD_CHARACTER; }
