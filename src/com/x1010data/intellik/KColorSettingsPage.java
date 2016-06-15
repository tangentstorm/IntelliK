package com.x1010data.intellik;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Created by michal.wallace on 6/15/16.
 */
public class KColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[] {
            new AttributesDescriptor("Comments", KSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Numbers", KSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("Strings", KSyntaxHighlighter.STRING),
            new AttributesDescriptor("Identifiers", KSyntaxHighlighter.IDENT),
            new AttributesDescriptor("Symbols", KSyntaxHighlighter.SYMBOL),
            new AttributesDescriptor("Primitives", KSyntaxHighlighter.PRIM),
            new AttributesDescriptor("Braces", KSyntaxHighlighter.BRACE),
            new AttributesDescriptor("Brackets", KSyntaxHighlighter.BRACK),
            new AttributesDescriptor("Parens", KSyntaxHighlighter.PAREN),
            new AttributesDescriptor("Builtins", KSyntaxHighlighter.BUILTIN),
            new AttributesDescriptor("Keywords", KSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Adverbs", KSyntaxHighlighter.ADVERB),
            new AttributesDescriptor("Commands", KSyntaxHighlighter.COMMAND),
            new AttributesDescriptor("Semicolons", KSyntaxHighlighter.SEMI),
            new AttributesDescriptor("Dot", KSyntaxHighlighter.DOT),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return KIcons.FILE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new KSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "/ my awesome k program\n" +
               "\\d .fizz.buzz\n"+
                "\n"+
               "fb:{1 _ ,/\" \" ,' ${:[~x!15; `fb; ~x!3;`f;  ~x!5;`b; `$$x]}' 1+!x}\n" +
                "\n"+
               "if[~\"1 2 f 4 b f 7 8 f b 11 f 13 14 fb 16 17 f 19 b\" ~ fb 20; `0: \"weak. :(\"]\n";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "K";
    }
}
