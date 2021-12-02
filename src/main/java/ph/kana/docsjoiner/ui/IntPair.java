package ph.kana.docsjoiner.ui;

public record IntPair(int first, int second) {

    public int max() {
        return Math.max(first, second);
    }

    @Override
    public String toString() {
        return String.format("%d \u00d7 %d", first, second);
    }
}
