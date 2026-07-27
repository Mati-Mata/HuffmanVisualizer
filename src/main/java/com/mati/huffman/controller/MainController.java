package com.mati.huffman.controller;

import com.mati.huffman.model.CompressionMetrics;
import com.mati.huffman.model.CompressionResult;
import com.mati.huffman.service.HuffmanCompressionService;
import com.mati.huffman.ui.SymbolAnalysisMapper;
import com.mati.huffman.ui.SymbolAnalysisRow;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Coordinates the JavaFX interface with the reusable Huffman engine.
 */
public final class MainController {

    private static final String TREE_INITIAL_MESSAGE =
            "Comprima un texto para generar el árbol";
    private static final String TREE_READY_MESSAGE =
            "Árbol generado. La visualización estará disponible en la Fase 9.";

    private final HuffmanCompressionService compressionService =
            new HuffmanCompressionService();
    private final SymbolAnalysisMapper analysisMapper =
            new SymbolAnalysisMapper();

    private CompressionResult currentResult;

    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab homeTab;
    @FXML
    private Tab analysisTab;
    @FXML
    private Tab treeTab;
    @FXML
    private TextArea originalTextArea;
    @FXML
    private Label fileNameLabel;
    @FXML
    private VBox resultsPane;
    @FXML
    private TextArea compressedTextArea;
    @FXML
    private Button decompressButton;
    @FXML
    private TextArea reconstructedTextArea;
    @FXML
    private Label verificationLabel;
    @FXML
    private Label originalSizeLabel;
    @FXML
    private Label huffmanSizeLabel;
    @FXML
    private Label savingLabel;
    @FXML
    private TableView<SymbolAnalysisRow> analysisTable;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> symbolColumn;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> unicodeColumn;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> frequencyColumn;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> probabilityColumn;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> huffmanCodeColumn;
    @FXML
    private TableColumn<SymbolAnalysisRow, String> codeLengthColumn;
    @FXML
    private Label totalSymbolsLabel;
    @FXML
    private Label distinctSymbolsLabel;
    @FXML
    private Label averageLengthLabel;
    @FXML
    private Label treePlaceholder;

    @FXML
    private void initialize() {
        configureAnalysisTable();
        resetResultState();
    }

    @FXML
    private void onOpenFile() {
        showAlert(
                Alert.AlertType.INFORMATION,
                "Abrir archivo",
                "La apertura de archivos se implementará en una fase posterior."
        );
    }

    @FXML
    private void onCompress() {
        String text = originalTextArea.getText();
        if (text.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Texto vacío",
                    "Escribe o pega un texto antes de comprimir."
            );
            return;
        }

        try {
            CompressionResult result = compressionService.compress(text);
            currentResult = result;
            displayCompressionResult(result);
        } catch (RuntimeException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "No se pudo comprimir",
                    userMessage(exception)
            );
        }
    }

    @FXML
    private void onDecompress() {
        if (currentResult == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Sin compresión",
                    "Primero comprime un texto para asociar su árbol."
            );
            return;
        }

        String reconstructed = currentResult.reconstructedText();
        reconstructedTextArea.setText(reconstructed);
        boolean matches = currentResult.originalText().equals(reconstructed);
        setVerificationState(
                matches,
                matches
                        ? "✓ El texto reconstruido coincide exactamente."
                        : "✕ El texto reconstruido no coincide."
        );
    }

    @FXML
    private void onClear() {
        originalTextArea.clear();
        fileNameLabel.setText("");
        resetResultState();
        mainTabPane.getSelectionModel().select(homeTab);
    }

    private void displayCompressionResult(CompressionResult result) {
        compressedTextArea.setText(result.encodedBits());
        reconstructedTextArea.clear();

        CompressionMetrics metrics = result.metrics();
        originalSizeLabel.setText(String.format(
                Locale.ROOT,
                "%d bytes · %d bits",
                metrics.originalUtf8ByteSize(),
                metrics.originalUtf8BitSize()
        ));
        huffmanSizeLabel.setText(
                metrics.huffmanMessageBitSize() + " bits"
        );
        savingLabel.setText(String.format(
                Locale.ROOT,
                "%.2f %%",
                metrics.theoreticalSavingPercentage()
        ));

        analysisTable.setItems(FXCollections.observableArrayList(
                analysisMapper.rowsFor(result)
        ));
        totalSymbolsLabel.setText(
                Long.toString(metrics.totalSymbolCount())
        );
        distinctSymbolsLabel.setText(
                Long.toString(metrics.distinctSymbolCount())
        );
        averageLengthLabel.setText(String.format(
                Locale.ROOT,
                "%.4f bits/símbolo",
                metrics.averageCodeLength()
        ));

        setVerificationState(
                result.roundTripSuccessful(),
                "✓ Round-trip verificado por el motor Huffman."
        );
        treePlaceholder.setText(TREE_READY_MESSAGE);
        resultsPane.setVisible(true);
        resultsPane.setManaged(true);
        decompressButton.setDisable(false);
        analysisTab.setDisable(false);
        treeTab.setDisable(false);
    }

    private void resetResultState() {
        currentResult = null;
        compressedTextArea.clear();
        reconstructedTextArea.clear();
        analysisTable.getItems().clear();

        originalSizeLabel.setText("—");
        huffmanSizeLabel.setText("—");
        savingLabel.setText("—");
        totalSymbolsLabel.setText("—");
        distinctSymbolsLabel.setText("—");
        averageLengthLabel.setText("—");
        setVerificationState(false, "Sin resultados");

        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
        decompressButton.setDisable(true);
        analysisTab.setDisable(true);
        treeTab.setDisable(true);
        treePlaceholder.setText(TREE_INITIAL_MESSAGE);
    }

    private void configureAnalysisTable() {
        symbolColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().symbol())
        );
        unicodeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().unicodeCode())
        );
        frequencyColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        Long.toString(cell.getValue().frequency())
                )
        );
        probabilityColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(String.format(
                        Locale.ROOT,
                        "%.4f",
                        cell.getValue().probability()
                ))
        );
        huffmanCodeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().huffmanCode())
        );
        codeLengthColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        Integer.toString(cell.getValue().codeLength())
                )
        );
        analysisTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }

    private void setVerificationState(boolean success, String message) {
        verificationLabel.setText(message);
        verificationLabel.getStyleClass().removeAll(
                "status-success",
                "status-error",
                "status-neutral"
        );
        verificationLabel.getStyleClass().add(
                success ? "status-success" : "status-neutral"
        );
        if (!success && currentResult != null) {
            verificationLabel.getStyleClass().remove("status-neutral");
            verificationLabel.getStyleClass().add("status-error");
        }
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message
    ) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (originalTextArea.getScene() != null) {
            alert.initOwner(originalTextArea.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private static String userMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "Ocurrió un error inesperado durante la compresión."
                : message;
    }
}
