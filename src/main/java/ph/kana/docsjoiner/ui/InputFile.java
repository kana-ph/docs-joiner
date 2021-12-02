package ph.kana.docsjoiner.ui;

import javafx.beans.property.*;

import java.nio.file.Path;

public class InputFile {

    private final ObjectProperty<Path> fileProperty = new SimpleObjectProperty<>(this, "file");
    private final ObjectProperty<IntPair> dimensionProperty = new SimpleObjectProperty<>(this, "dimension");
    private final ObjectProperty<IntPair> densityProperty = new SimpleObjectProperty<>(this, "density");

    public InputFile(Path file) {
        fileProperty.set(file);
    }

    public ObjectProperty<Path> fileProperty() {
        return fileProperty;
    }

    public ObjectProperty<IntPair> dimensionProperty() {
        return dimensionProperty;
    }

    public ObjectProperty<IntPair> densityProperty() {
        return densityProperty;
    }

    public Path getFile() {
        return fileProperty.get();
    }

    public IntPair getDimension() {
        return dimensionProperty.get();
    }

    public void setDimension(IntPair dimension) {
        dimensionProperty.set(dimension);
    }

    public IntPair getDensity() {
        return densityProperty.get();
    }

    public void setDensity(IntPair density) {
        densityProperty.set(density);
    }
}
