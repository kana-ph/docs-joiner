package ph.kana.docsjoiner.ui;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;

class DragSortableImageFileTableRow extends TableRow<InputFile> {

    private final static DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    private final TableView<InputFile> tableView;

    DragSortableImageFileTableRow(TableView<InputFile> filesListView) {
        this.tableView = filesListView;
        initializeDragDropListeners(this);
    }

    private void initializeDragDropListeners(DragSortableImageFileTableRow row) {
        row.setOnDragDetected(event -> {
            if (row.isEmpty()) {
                return;
            }
            var dragboard = row.startDragAndDrop(TransferMode.MOVE);
            dragboard.setDragView(row.snapshot(null, null));

            var clipboardContent = new ClipboardContent();
            clipboardContent.put(SERIALIZED_MIME_TYPE, row.getIndex());
            dragboard.setContent(clipboardContent);
            event.consume();
        });

        row.setOnDragOver(event -> {
            var dragboard = event.getDragboard();
            if (dragboard.hasContent(SERIALIZED_MIME_TYPE) &&
                row.getIndex() != (int) dragboard.getContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });

        row.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            if (!dragboard.hasContent(SERIALIZED_MIME_TYPE)) {
                return;
            }
            int dragIndex = (int) dragboard.getContent(SERIALIZED_MIME_TYPE);
            var draggedPath = tableView.getItems()
                .remove(dragIndex);

            int dropIndex = (row.isEmpty()) ?
                tableView.getItems().size() :
                row.getIndex();
            tableView.getItems()
                .add(dropIndex, draggedPath);

            event.setDropCompleted(true);
            tableView.getSelectionModel()
                .select(dropIndex);
            event.consume();
        });
    }
}
