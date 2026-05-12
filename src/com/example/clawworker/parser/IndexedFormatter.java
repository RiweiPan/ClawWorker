package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class IndexedFormatter {
    private static final String INDENT = "  ";

    public static FormatResult format(UiElement root) {
        List<UiElement> flat = new ArrayList<>();
        StringBuilder dump = new StringBuilder();
        if (root != null) {
            root = TreePreprocessor.mergeAdjacentText(root);
            formatRecursive(root, flat, dump, 0, 0);
        }
        return new FormatResult(flat, dump.toString());
    }

    private static int formatRecursive(UiElement node, List<UiElement> flat,
                                       StringBuilder dump, int depth, int index) {
        boolean interactive = node.isClickable() || node.isEditable();
        boolean hasText = hasVisibleText(node);
        boolean isLeaf = node.getChildren().isEmpty();
        boolean skipSelf = interactive && !hasText && hasInteractiveDescendant(node);

        int nextIndex = index;

        if (skipSelf) {
            // Container is clickable but children already provide interaction points
        } else if (interactive || (hasText && isLeaf)) {
            node.setIndex(nextIndex);
            flat.add(node);
            appendLine(dump, depth, nextIndex, node);
            nextIndex = nextIndex + 1;
        } else if (hasText) {
            appendLine(dump, depth, -1, node);
        }

        // Check if children form a card group
        TreePreprocessor.CardGroupResult cardGroup =
                TreePreprocessor.detectCardGroup(node.getChildren());

        if (cardGroup != null && cardGroup.isCardGroup) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                UiElement child = node.getChildren().get(i);
                if (cardGroup.cardPositions.contains(i)) {
                    String title = cardGroup.cardTitles.get(i);
                    nextIndex = formatCard(child, flat, dump, depth, nextIndex, title);
                } else {
                    int cd = skipSelf ? depth : depth + 1;
                    nextIndex = formatRecursive(child, flat, dump, cd, nextIndex);
                }
            }
        } else {
            int childDepth = skipSelf ? depth : depth + 1;
            for (UiElement child : node.getChildren()) {
                nextIndex = formatRecursive(child, flat, dump, childDepth, nextIndex);
            }
        }

        return nextIndex;
    }

    private static int formatCard(UiElement card, List<UiElement> flat,
                                  StringBuilder dump, int depth, int nextIndex,
                                  String title) {
        // Card separator
        indent(dump, depth);
        dump.append("--- ").append(title).append(" ---\n");

        // Merge same-row tags within the card
        List<UiElement> children = TreePreprocessor.mergeCardTags(card.getChildren());

        int idx = nextIndex;
        for (UiElement child : children) {
            idx = formatRecursive(child, flat, dump, depth + 1, idx);
        }
        return idx;
    }

    private static boolean hasVisibleText(UiElement node) {
        String text = node.getText();
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.equals(node.getResourceId()) || text.equals(node.getClassName())) {
            return false;
        }
        return true;
    }

    private static boolean hasInteractiveDescendant(UiElement node) {
        if (node.getChildren().isEmpty()) {
            return false;
        }
        for (UiElement child : node.getChildren()) {
            if (child.isClickable() || child.isEditable()) {
                return true;
            }
            if (hasInteractiveDescendant(child)) {
                return true;
            }
        }
        return false;
    }

    private static void appendLine(StringBuilder dump, int depth, int index, UiElement node) {
        indent(dump, depth);
        if (index >= 0) {
            dump.append(index).append(". ");
        } else {
            dump.append("· ");
        }
        dump.append(node.getClassName())
                .append(": ")
                .append(node.getResourceId())
                .append(", \"")
                .append(displayText(node))
                .append("\" - (")
                .append(boundsStr(node))
                .append(")\n");
    }

    private static String displayText(UiElement node) {
        String text = node.getText();
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.equals(node.getResourceId()) || text.equals(node.getClassName())) {
            return "";
        }
        return text;
    }

    private static void indent(StringBuilder dump, int depth) {
        for (int i = 0; i < depth; i++) {
            dump.append(INDENT);
        }
    }

    private static String boundsStr(UiElement node) {
        Rect b = node.getBounds();
        return b.left + "," + b.top + "," + b.right + "," + b.bottom;
    }

    public static class FormatResult {
        private final List<UiElement> elements;
        private final String textDump;

        public FormatResult(List<UiElement> elements, String textDump) {
            this.elements = elements;
            this.textDump = textDump == null ? "" : textDump;
        }

        public List<UiElement> getElements() {
            return elements;
        }

        public String getTextDump() {
            return textDump;
        }
    }
}
