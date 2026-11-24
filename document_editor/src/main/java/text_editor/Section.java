package text_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A section has a name, its own paragraphs, and a list of child sections.
 *
 * Its sectionID is a hierarchical label of 1-based positions joined by "/":
 * the second subsection of the first top-level section is "1/2". Labels are
 * assigned automatically when a section is attached, and refreshed whenever
 * siblings are added or removed.
 */
public class Section {

    private String name;
    private String sectionID = "";
    private final List<Paragraph> paragraphs = new ArrayList<>();
    private final List<Section> subSections = new ArrayList<>();
    private int startLocation;
    private int endLocation;

    public Section(String name) {
        setName(name);
    }

    public String getName() { return name; }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Section name cannot be null");
        }
        this.name = name;
    }

    public String getSectionID() { return sectionID; }

    public int getStartLocation() { return startLocation; }

    public int getEndLocation() { return endLocation; }

    // ---- structure ----------------------------------------------------

    public void appendSection(Section newSection) {
        insertSubSection(subSections.size() + 1, newSection);
    }

    /** Inserts a subsection at a one-based position. */
    public void insertSubSection(int index, Section newSection) {
        if (newSection == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }
        if (newSection == this || newSection.contains(this)) {
            throw new IllegalArgumentException("A section cannot be added inside itself");
        }
        checkInsertIndex(index, subSections.size());
        subSections.add(index - 1, newSection);
        assignLabel(sectionID); // relabels the whole subtree; no-op ID if this is unattached
    }

    public void appendParagraph(Paragraph newParagraph) {
        // keeps elias's append behavior while sharing the new position logic
        insertParagraph(paragraphs.size() + 1, newParagraph);
    }

    // inserts at a one-based position and updates every paragraph id
    public void insertParagraph(int index, Paragraph newParagraph) {
        if (newParagraph == null) {
            throw new IllegalArgumentException("Paragraph cannot be null");
        }
        checkInsertIndex(index, paragraphs.size());
        paragraphs.add(index - 1, newParagraph);
        // uses elias's existing relabeling so later paragraph ids stay correct
        assignLabel(sectionID);
    }

    /** Removes the subsection at the given 1-based index and relabels the rest. */
    public Section removeSubSection(int index) {
        checkIndex(index, subSections.size());
        Section removed = subSections.remove(index - 1);
        removed.sectionID = "";
        assignLabel(sectionID);
        return removed;
    }

    /** Removes the paragraph at the given 1-based index and renumbers the rest. */
    public Paragraph removeParagraph(int index) {
        checkIndex(index, paragraphs.size());
        Paragraph removed = paragraphs.remove(index - 1);
        removed.setParagraphID("");
        assignLabel(sectionID);
        return removed;
    }

    public int countParagraphs() { return paragraphs.size(); }

    public int countSubSections() { return subSections.size(); }

    public List<Paragraph> getParagraphs() { return Collections.unmodifiableList(paragraphs); }

    public List<Section> getSubSections() { return Collections.unmodifiableList(subSections); }

    /** Returns the direct child at the given 1-based index, or null if none. */
    public Section getSubSection(int index) {
        return (index >= 1 && index <= subSections.size()) ? subSections.get(index - 1) : null;
    }

    /** True if the given section is anywhere below this one. */
    public boolean contains(Section other) {
        for (Section child : subSections) {
            if (child == other || child.contains(other)) {
                return true;
            }
        }
        return false;
    }

    // ---- package-private helpers used by SectionTree --------------------

    /** Sets this section's label and recursively relabels children and paragraphs. */
    void assignLabel(String label) {
        this.sectionID = label;
        for (int i = 0; i < subSections.size(); i++) {
            String childLabel = label.isEmpty() ? String.valueOf(i + 1) : label + "/" + (i + 1);
            subSections.get(i).assignLabel(childLabel);
        }
        for (int i = 0; i < paragraphs.size(); i++) {
            paragraphs.get(i).setParagraphID(label + "#" + (i + 1));
        }
    }

    /**
     * Lays out character locations starting at {@code start}: this section's
     * paragraphs first (one newline between them), then its subsections.
     * Returns the position just after this section's last character.
     */
    int recalculateLocations(int start) {
        this.startLocation = start;
        int pos = start;
        for (Paragraph p : paragraphs) {
            p.setLocations(pos, pos + p.length());
            pos += p.length() + 1;
        }
        for (Section child : subSections) {
            pos = child.recalculateLocations(pos);
        }
        this.endLocation = Math.max(start, pos - 1);
        return pos;
    }

    /** Adds this section and all descendants to the list, depth-first. */
    void collect(List<Section> out) {
        out.add(this);
        for (Section child : subSections) {
            child.collect(out);
        }
    }

    private static void checkIndex(int index, int size) {
        if (index < 1 || index > size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of range 1.." + size);
        }
    }

    private static void checkInsertIndex(int index, int size) {
        if (index < 1 || index > size + 1) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of range 1.." + (size + 1));
        }
    }

    @Override
    public String toString() {
        return "Section{" + sectionID + " '" + name + "', paragraphs=" + paragraphs.size()
                + ", subsections=" + subSections.size() + "}";
    }
}