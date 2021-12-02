module ph.kana.docsjoiner {
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;

    opens ph.kana.docsjoiner to javafx.fxml;
    exports ph.kana.docsjoiner;
    exports ph.kana.docsjoiner.ui;
    opens ph.kana.docsjoiner.ui to javafx.fxml;
}
