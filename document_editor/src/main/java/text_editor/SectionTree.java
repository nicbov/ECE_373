package text_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Holds only the first-layer sections of a document and finds any section by
 * its hierarchical label. Labels are 1-based positions joined by "/":
 *   "2"   -> second top-level section
 *   "1/2" -> second subsection of the first top-level section
 *   "1/2/3" -> third subsection of that one, and so on.
 */
public class SectionTree {

    private final List<Section> mainSectionArray = new ArrayList<>();

    // ---- adding -------------------------------------------------------

    /** Appends a top-level section and returns its label. */
    public String addSection(Section newSection) {
        if (newSection == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }
        mainSectionArray.add(newSection);
        newSection.assignLabel(String.valueOf(mainSectionArray.size()));
        return newSection.getSectionID();
    }

    /** Appends a subsection under the section with the given label; returns its label. */
    public String addSubSection(Section newSubSection, String sectionID) {
        Section parent = require(sectionID);
        parent.appendSection(newSubSection);
        return newSubSection.getSectionID();
    }

    /** Appends a paragraph to the section with the given label. */
    public void addParagraph(Paragraph newParagraph, String sectionID) {
        Section section = require(sectionID);
        section.appendParagraph(newParagraph);
        recalculateLocations();
    }

    // inserts a paragraph at a one-based position inside one section
    public void insertParagraph(Paragraph newParagraph, String sectionID, int index) {
        require(sectionID).insertParagraph(index, newParagraph);
        // follows elias's existing tree layout after the structure changes
        recalculateLocations();
    }

    // removes a paragraph by id and returns it so another edit can reuse it
    public Paragraph removeParagraph(String paragraphID) {
        if (paragraphID == null) {
            throw new IllegalArgumentException("Paragraph ID is required");
        }
        int hash = paragraphID.lastIndexOf('#');
        if (hash < 1) {
            throw new IllegalArgumentException("Invalid paragraph ID '" + paragraphID + "'");
        }
        Section section = require(paragraphID.substring(0, hash));
        int index;
        try {
            index = Integer.parseInt(paragraphID.substring(hash + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid paragraph ID '" + paragraphID + "'", e);
        }
        if (index < 1 || index > section.countParagraphs()) {
            throw new IllegalArgumentException("No paragraph with ID '" + paragraphID + "'");
        }
        // section handles id changes and the tree handles document locations
        Paragraph removed = section.removeParagraph(index);
        recalculateLocations();
        return removed;
    }

    // ---- searching by hierarchical label ------------------------------------

    /** Finds a section by label such as "1", "1/2" or "3/1/2". Empty if it does not exist. */
    public Optional<Section> find(String label) {
        int[] path = parseLabel(label);
        if (path == null || path[0] < 1 || path[0] > mainSectionArray.size()) {
            return Optional.empty();
        }
        Section current = mainSectionArray.get(path[0] - 1);
        for (int i = 1; i < path.length; i++) {
            current = current.getSubSection(path[i]);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    public boolean hasSection(String label) {
        return find(label).isPresent();
    }

    /** Finds a paragraph by ID of the form {@code <sectionLabel>#<n>}, e.g. {@code 1/2#3}. */
    public Optional<Paragraph> findParagraph(String paragraphID) {
        if (paragraphID == null) return Optional.empty();
        int hash = paragraphID.lastIndexOf('#');
        if (hash < 0) return Optional.empty();
        Optional<Section> section = find(paragraphID.substring(0, hash));
        if (section.isEmpty()) return Optional.empty();
        try {
            int n = Integer.parseInt(paragraphID.substring(hash + 1));
            List<Paragraph> list = section.get().getParagraphs();
            return (n >= 1 && n <= list.size()) ? Optional.of(list.get(n - 1)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** The section with this label and everything below it, depth-first. */
    public List<Section> getSubtree(String label) {
        List<Section> out = new ArrayList<>();
        find(label).ifPresent(s -> s.collect(out));
        return out;
    }

    /** Every section in the document, depth-first in reading order. */
    public List<Section> getAllSections() {
        List<Section> out = new ArrayList<>();
        for (Section s : mainSectionArray) {
            s.collect(out);
        }
        return out;
    }

    /** All sections whose name matches (case-insensitive). */
    public List<Section> findByName(String name) {
        List<Section> matches = new ArrayList<>();
        for (Section s : getAllSections()) {
            if (s.getName().equalsIgnoreCase(name)) {
                matches.add(s);
            }
        }
        return matches;
    }

    // ---- removing -----------------------------------------------------

    /** Removes the section with this label (and its subtree); later siblings are relabeled. */
    public Section removeSection(String label) {
        int[] path = parseLabel(label);
        require(label);
        if (path.length == 1) {
            Section removed = mainSectionArray.remove(path[0] - 1);
            removed.assignLabel("");
            relabelMain();
            return removed;
        }
        String parentLabel = label.substring(0, label.lastIndexOf('/'));
        return require(parentLabel).removeSubSection(path[path.length - 1]);
    }

    /** Moves a section and its subtree to a new parent and position. */
    public void moveSection(String sourceLabel, String destinationParent, int index) {
        Section source = require(sourceLabel);
        Section parent = destinationParent.equals("root") ? null : require(destinationParent);
        if (parent == source || source.contains(parent)) {
            throw new IllegalArgumentException("A section cannot move inside itself.");
        }
        int destinationSize = parent == null ? mainSectionArray.size() : parent.countSubSections();
        if (index < 1 || index > destinationSize + 1) {
            throw new IllegalArgumentException("Invalid section destination position.");
        }
        removeSection(sourceLabel);
        if (parent == null) {
            mainSectionArray.add(Math.min(index, mainSectionArray.size() + 1) - 1, source);
            relabelMain();
        } else {
            parent.insertSubSection(Math.min(index, parent.countSubSections() + 1), source);
        }
        recalculateLocations();
    }

    // ---- misc -------------------------------------------------------

    public List<Section> getMainSectionArray() { return Collections.unmodifiableList(mainSectionArray); }

    public int countSections() { return mainSectionArray.size(); }

    /** Recomputes start/end character locations for every section and paragraph. */
    public void recalculateLocations() {
        int pos = 0;
        for (Section s : mainSectionArray) {
            pos = s.recalculateLocations(pos);
        }
    }

    void replaceWith(SectionTree source) {
        if (source == null) {
            throw new IllegalArgumentException("Section tree is required.");
        }
        if (source == this) {
            return;
        }
        mainSectionArray.clear();
        mainSectionArray.addAll(source.mainSectionArray);
        relabelMain();
        recalculateLocations();
    }

    private void relabelMain() {
        for (int i = 0; i < mainSectionArray.size(); i++) {
            mainSectionArray.get(i).assignLabel(String.valueOf(i + 1));
        }
    }

    private Section require(String label) {
        return find(label).orElseThrow(
                () -> new IllegalArgumentException("No section with label '" + label + "'"));
    }

    /** Parses "1/2/3" into {1,2,3}; returns null if the label is malformed. */
    private static int[] parseLabel(String label) {
        if (label == null || label.isBlank()) return null;
        String[] parts = label.trim().split("/", -1);
        int[] path = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                path[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return path;
    }
}