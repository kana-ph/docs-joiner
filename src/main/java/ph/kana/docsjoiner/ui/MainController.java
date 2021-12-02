package ph.kana.docsjoiner.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import ph.kana.docsjoiner.util.ImageMagickService;
import ph.kana.docsjoiner.util.QolService;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import static java.util.UUID.randomUUID;

public class MainController implements Initializable {

    @FXML
    private Pane rootPane;

    @FXML
    private TableView<InputFile> filesTableView;

    @FXML
    private TableColumn<InputFile, Path> fileColumn;

    @FXML
    private TableColumn<InputFile, IntPair> dimensionColumn;

    @FXML
    private TableColumn<InputFile, IntPair> densityColumn;

    @FXML
    private Button addFileButton;

    @FXML
    private Button removeButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button joinButton;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label messageLabel;

    private static final File INITIAL_OPEN_DIRECTORY = new File(System.getProperty("user.home"));
    private static final ExtensionFilter PDF_EXTENSION_FILTER = new ExtensionFilter("PDF Document", "*.pdf");
    private static final long MESSAGE_DISAPPEAR_DELAY = 5000L;

    private final ImageMagickService imageMagickService = new ImageMagickService();
    private final QolService qolService = new QolService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeColumnMappers();
        initializeTableDragSorting();
        initializeButtonBindings();

        checkImageMagickVersion();
    }

    @FXML
    public void addFile() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Open Files");
        fileChooser.setInitialDirectory(qolService.fetchInitialOpenDir().toFile());

        var files = fileChooser.showOpenMultipleDialog(getWindow());
        if (files != null) {
            addFilesToTableView(files);
            qolService.saveLastOpenDir(files);
        }
    }

    @FXML
    public void removeFile() {
        var selectionModel = filesTableView.getSelectionModel();
        var selectedItems = selectionModel.getSelectedItems();
        filesTableView.getItems()
            .removeAll(selectedItems);
        selectionModel.clearSelection();
    }

    @FXML
    public void clearFiles() {
        filesTableView.getItems()
            .clear();
    }

    @FXML
    public void joinDocs() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Save file as PDF");
        fileChooser.setInitialDirectory(INITIAL_OPEN_DIRECTORY);
        fileChooser.setSelectedExtensionFilter(PDF_EXTENSION_FILTER);
        fileChooser.setInitialFileName(randomFilename());
        var file = fileChooser.showSaveDialog(getWindow());

        if (file != null) {
            startJoiningDocs(file.toPath());
        }
    }

    @FXML
    public void fileDragOver(DragEvent event) {
        if (event.getGestureSource() != filesTableView && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    public void fileDragDropped(DragEvent event) {
        var dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasFiles()) {
            addFilesToTableView(dragboard.getFiles());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private void initializeColumnMappers() {
        fileColumn.setCellValueFactory(imageFile -> imageFile.getValue().fileProperty());
        dimensionColumn.setCellValueFactory(imageFile -> imageFile.getValue().dimensionProperty());
        densityColumn.setCellValueFactory(imageFile -> imageFile.getValue().densityProperty());
    }

    private void initializeTableDragSorting() {
        filesTableView.setRowFactory(tv -> new DragSortableImageFileTableRow(filesTableView));
    }

    private void initializeButtonBindings() {
        var selectedItems = filesTableView.getSelectionModel()
            .getSelectedItems();
        removeButton.disableProperty()
            .bind(Bindings.isEmpty(selectedItems).or(filesTableView.disabledProperty()));

        clearButton.disableProperty()
            .bind(Bindings.isEmpty(filesTableView.getItems()).or(filesTableView.disabledProperty()));

        joinButton.disableProperty()
            .bind(Bindings.isEmpty(filesTableView.getItems()).or(filesTableView.disabledProperty()));
    }

    private void checkImageMagickVersion() {
        new Thread(() -> {
            var version = imageMagickService.imageMagickVersion();
            if (version != null) {
                Platform.runLater(() -> messageLabel.setText(version.substring(9)));
                clearMessageOnDelay();
            } else {
                Platform.runLater(() -> {
                    addFileButton.setDisable(true);
                    messageLabel.setText("Error: ImageMagick not found or installed in system!");
                });
            }
        }, "check-version-thread").start();
    }

    private Window getWindow() {
        return rootPane.getScene()
            .getWindow();
    }

    private void addFilesToTableView(List<File> files) {
        files.stream()
            .map(File::toPath)
            .map(InputFile::new)
            .map(imageMagickService::identifyFile)
            .forEach(filesTableView.getItems()::add);
    }

    private void setApplicationLock(boolean locked) {
        List<Node> dontLockList = List.of(
            joinButton,
            removeButton,
            clearButton,
            messageLabel,
            progressIndicator
        );
        rootPane.getChildren()
            .stream()
            .filter(node -> !dontLockList.contains(node))
            .forEach(node -> node.setDisable(locked));
        progressIndicator.setVisible(locked);
    }

    private String randomFilename() {
        var random = randomUUID().toString()
            .substring(0, 8);
        return "joined-pdf_" + random + ".pdf";
    }

    private void startJoiningDocs(Path path) {
        new Thread(() -> {
            Platform.runLater(() -> {
                messageLabel.setText("Please wait...");
                setApplicationLock(true);
            });
            var pdf = imageMagickService.joinFiles(filesTableView.getItems(), path);
            Platform.runLater(() -> {
                messageLabel.setText("File created: " + pdf.getFileName());
                setApplicationLock(false);
            });
            clearMessageOnDelay();
        }, "pdf-joining-thread").start();
    }

    private void clearMessageOnDelay() {
        try {
            Thread.sleep(MESSAGE_DISAPPEAR_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> messageLabel.setText(""));
    }
}
