package text_editor;

/**
 * A paragraph holds plain text only. Its ID and character locations are
 * assigned by the Section / SectionTree that contains it.
 */
public class Paragraph {

    private String paragraphID = "";
    private String text;
    private int startLocation;
    private int endLocation;

    public Paragraph(String text) {
        setText(text);
    }

    public String getParagraphID() { return paragraphID; }

    /** Package-private: IDs are managed by the owning Section. */
    void setParagraphID(String paragraphID) { this.paragraphID = paragraphID; }

    public String getText() { return text; }

    public void setText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Paragraph text cannot be null");
        }
        this.text = text;
    }

    public int length() { return text.length(); }

    public int getStartLocation() { return startLocation; }

    public int getEndLocation() { return endLocation; }

    /** Package-private: locations are managed by the owning Section. */
    void setLocations(int start, int end) {
        this.startLocation = start;
        this.endLocation = end;
    }

    @Override
    public String toString() {
        return "Paragraph{id=" + paragraphID + ", [" + startLocation + "," + endLocation + "]}";
    }
}