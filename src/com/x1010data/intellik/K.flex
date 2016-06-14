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

NEWLINE= \n|\r|\r\n
WHITESPACE=[\ \t\f]
COMMENT= " /"[^\r\n]*
DIGIT=[0123456789]
%state WAITING_VALUE

%%

<YYINITIAL> {COMMENT}                                       { yybegin(YYINITIAL); return KTypes.COMMENT; }
<YYINITIAL> {DIGIT}+                                        { yybegin(YYINITIAL); return KTypes.INT; }
<YYINITIAL> ({NEWLINE}|{WHITESPACE})+                       { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

({NEWLINE}|{WHITESPACE})+                                   { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
{WHITESPACE}+                                               { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

.                                                           { return TokenType.BAD_CHARACTER; }

