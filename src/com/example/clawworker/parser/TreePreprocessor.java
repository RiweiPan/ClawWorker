package com.example.clawworker.parser;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreePreprocessor {

    public static class CardGroupResult {
        public final boolean isCardGroup;
        public final Set<Integer> cardPositions;
        public final Map<Integer, String> cardTitles;

        public CardGroupResult(boolean isCardGroup, Set<Integer> cardPositions,
                               Map<Integer, String> cardTitles) {
            this.isCardGroup = isCardGroup;
            this.cardPositions = cardPositions;
            this.cardTitles = cardTitles;
        }
    }

    /**
     * Walk the tree and merge adjacent non-interactive TextViews that sit on
     * the same visual line (vertical overlap > 50%, horizontal gap < 30px).
     */
    public static UiElement mergeAdjacentText(UiElement root) {
        if (root == null) {
            return null;
        }
        List<UiElement> processed = new ArrayList<>();
        for (UiElement child : root.getChildren()) {
            processed.add(mergeAdjacentText(child));
        }
        if (processed.isEmpty()) {
            return root;
        }
        // Sort by left so consecutive elements are horizontally adjacent
        List<UiElement> sorted = new ArrayList<>(processed);
        sorted.sort((a, b) -> {
            int dx = a.getBounds().left - b.getBounds().left;
            if (dx != 0) return dx;
            return a.getBounds().top - b.getBounds().top;
        });
        List<UiElement> merged = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            UiElement cur = sorted.get(i);
            if (isMergeableText(cur)) {
                int j = i + 1;
                List<UiElement> group = new ArrayList<>();
                group.add(cur);
                while (j < sorted.size() && isMergeableText(sorted.get(j))
                        && sameLine(group.get(group.size() - 1), sorted.get(j))) {
                    group.add(sorted.get(j));
                    j++;
                }
                if (group.size() >= 2) {
                    merged.add(mergeGroup(group));
                } else {
                    merged.add(cur);
                }
                i = j;
            } else {
                merged.add(cur);
                i++;
            }
        }
        return new UiElement(root.getClassName(), root.getText(), root.getResourceId(),
                root.isClickable(), root.isEditable(), root.getBounds(), merged);
    }

    /**
     * Detect whether a list of sibling nodes forms a repeating card group
     * (RecyclerView / ListView pattern).
     */
    public static CardGroupResult detectCardGroup(List<UiElement> siblings) {
        if (siblings == null || siblings.size() < 2) {
            return new CardGroupResult(false, null, null);
        }
        // Compute fingerprint for each sibling
        List<String> fingerprints = new ArrayList<>();
        for (UiElement sib : siblings) {
            fingerprints.add(structuralFingerprint(sib));
        }
        // Group by fingerprint
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < fingerprints.size(); i++) {
            String fp = fingerprints.get(i);
            groups.computeIfAbsent(fp, k -> new ArrayList<>()).add(i);
        }
        // Find the largest group with same-class, similar-height siblings
        String bestFp = null;
        List<Integer> bestGroup = null;
        for (Map.Entry<String, List<Integer>> entry : groups.entrySet()) {
            List<Integer> positions = entry.getValue();
            if (positions.size() < 2) continue;
            // Verify that all members have similar height
            if (!sameHeightClass(siblings, positions)) continue;
            if (bestGroup == null || positions.size() > bestGroup.size()) {
                bestGroup = positions;
                bestFp = entry.getKey();
            }
        }
        if (bestGroup == null || bestGroup.size() < 2) {
            return new CardGroupResult(false, null, null);
        }
        Set<Integer> cardPos = new java.util.LinkedHashSet<>(bestGroup);
        Map<Integer, String> titles = new HashMap<>();
        for (int pos : bestGroup) {
            titles.put(pos, extractCardTitle(siblings.get(pos)));
        }
        return new CardGroupResult(true, cardPos, titles);
    }

    /**
     * Merge consecutive same-row tags within a card into a single "tags:" row element.
     * Returns a new children list with tags merged.
     */
    public static List<UiElement> mergeCardTags(List<UiElement> children) {
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        // Group by quantized Y coordinate
        Map<Integer, List<UiElement>> rows = new LinkedHashMap<>();
        for (UiElement child : children) {
            int rowKey = child.getBounds().centerY() / 25;
            rows.computeIfAbsent(rowKey, k -> new ArrayList<>()).add(child);
        }
        List<UiElement> result = new ArrayList<>();
        for (List<UiElement> row : rows.values()) {
            if (row.size() >= 2 && allTags(row)) {
                result.add(mergeRowToTagLine(row));
            } else {
                result.addAll(row);
            }
        }
        return result;
    }

    // ---- private helpers ----

    private static boolean isMergeableText(UiElement node) {
        if (node.isClickable() || node.isEditable()) return false;
        String cn = node.getClassName();
        return cn.contains("TextView") || cn.contains("EditText");
    }

    private static boolean sameLine(UiElement a, UiElement b) {
        Rect ra = a.getBounds();
        Rect rb = b.getBounds();
        int overlapTop = Math.max(ra.top, rb.top);
        int overlapBottom = Math.min(ra.bottom, rb.bottom);
        if (overlapTop >= overlapBottom) return false;
        int overlap = overlapBottom - overlapTop;
        int minH = Math.min(ra.height(), rb.height());
        if (minH <= 0) return false;
        // > 40% vertical overlap → same line
        return overlap * 100 / minH >= 40;
    }

    private static UiElement mergeGroup(List<UiElement> group) {
        StringBuilder sb = new StringBuilder();
        Rect union = new Rect(group.get(0).getBounds());
        for (int i = 0; i < group.size(); i++) {
            UiElement e = group.get(i);
            if (i > 0) {
                Rect eb = e.getBounds();
                union.union(eb);
            }
            String t = e.getText();
            if (t != null && !t.isEmpty()) {
                sb.append(t);
            }
        }
        return new UiElement("text", sb.toString(), "", false, false, union, new ArrayList<>());
    }

    private static String structuralFingerprint(UiElement node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getClassName());
        sb.append('|').append(node.getChildren().size());
        if (!node.getChildren().isEmpty()) {
            String firstChildClass = node.getChildren().get(0).getClassName();
            sb.append('|').append(firstChildClass);
        }
        return sb.toString();
    }

    private static boolean sameHeightClass(List<UiElement> siblings, List<Integer> positions) {
        int totalH = 0;
        for (int p : positions) {
            totalH += siblings.get(p).getBounds().height();
        }
        int avgH = totalH / positions.size();
        if (avgH <= 0) return false;
        for (int p : positions) {
            int h = siblings.get(p).getBounds().height();
            double ratio = (double) Math.min(h, avgH) / Math.max(h, avgH);
            if (ratio < 0.85) return false;
        }
        return true;
    }

    private static String extractCardTitle(UiElement card) {
        // BFS: find the text descendant with the widest bounds
        UiElement best = null;
        int bestW = 0;
        java.util.Deque<UiElement> queue = new java.util.ArrayDeque<>();
        queue.add(card);
        while (!queue.isEmpty()) {
            UiElement node = queue.removeFirst();
            String t = node.getText();
            if (t != null && !t.isEmpty()
                    && !t.equals(node.getResourceId())
                    && !t.equals(node.getClassName())) {
                int w = node.getBounds().width();
                if (w > bestW) {
                    bestW = w;
                    best = node;
                }
            }
            for (UiElement child : node.getChildren()) {
                queue.add(child);
            }
        }
        return best != null ? best.getText() : "";
    }

    private static boolean allTags(List<UiElement> row) {
        for (UiElement e : row) {
            if (e.isClickable() || e.isEditable()) return false;
            if (!isShortTextView(e)) return false;
        }
        return true;
    }

    private static boolean isShortTextView(UiElement node) {
        String cn = node.getClassName();
        if (!cn.contains("TextView")) return false;
        return node.getBounds().width() < 200;
    }

    private static UiElement mergeRowToTagLine(List<UiElement> row) {
        StringBuilder sb = new StringBuilder();
        Rect union = new Rect(row.get(0).getBounds());
        for (int i = 0; i < row.size(); i++) {
            UiElement e = row.get(i);
            if (i > 0) {
                sb.append(" · ");
                union.union(e.getBounds());
            }
            String t = e.getText();
            if (t != null && !t.isEmpty()) {
                sb.append(t);
            }
        }
        return new UiElement("tags", sb.toString(), "", false, false, union, new ArrayList<>());
    }
}
