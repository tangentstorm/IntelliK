package com.tangentcode.intellik;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.tangentcode.intellik.dialect.KDialectService;
import com.tangentcode.intellik.parser.KParser;
import com.tangentcode.intellik.psi.KFile;
import com.tangentcode.intellik.psi.KTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KParserDefinition implements ParserDefinition {

    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    public static final TokenSet COMMENTS = TokenSet.create(KTypes.COMMENT);
    public static final TokenSet STRINGS = TokenSet.create(KTypes.STRING);

    public static final IFileElementType FILE =
            new IFileElementType(Language.<KLanguage>findInstance(KLanguage.class));


    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new KLexerAdapter(KDialectService.tableFor(project, null));
    }

    @Override
    public PsiParser createParser(Project project) {
        return new KParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode astNode) {
        return KTypes.Factory.createElement(astNode);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new KFile(fileViewProvider);
    }

    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }
}
