package com.mati.huffman.controller;

import com.mati.huffman.model.CompressionMetrics;
import com.mati.huffman.model.CompressionResult;
import com.mati.huffman.service.HuffmanCompressionService;
import com.mati.huffman.service.TextFileService;
import com.mati.huffman.ui.HuffmanTreeView;
import com.mati.huffman.ui.SymbolAnalysisMapper;
import com.mati.huffman.ui.SymbolAnalysisRow;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Locale;
import java.util.Optional;

/**
 * Coordinates the JavaFX interface with the reusable Huffman engine.
 */
public final class MainController {

    private final HuffmanCompressionService compressionService =
            new HuffmanCompressionService();
    private final SymbolAnalysisMapper analysisMapper =
            new SymbolAnalysisMapper();
    private final TextFileService textFileService = new TextFileService();

    private CompressionResult currentResult;
    private File lastDirectory;

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
    private HuffmanTreeView huffmanTreeView;

    @FXML
    private void initialize() {
        configureAnalysisTable();
        resetResultState();
    }

    @FXML
    private void onOpenFile() {
        FileChooser chooser = createTextFileChooser();
        File selectedFile = chooser.showOpenDialog(
                originalTextArea.getScene().getWindow()
        );
        if (selectedFile == null) {
            return;
        }

        rememberDirectory(selectedFile);
        try {
            String content = textFileService.readUtf8(selectedFile.toPath());
            if (hasReplaceableState() && !confirmReplacement()) {
                return;
            }
            loadFileContent(selectedFile, content);
        } catch (CharacterCodingException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "UTF-8 no válido",
                    "No se abrió el archivo porque contiene una secuencia "
                            + "inválida. Esta versión acepta únicamente UTF-8."
            );
        } catch (NoSuchFileException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Archivo no encontrado",
                    "El archivo seleccionado ya no existe."
            );
        } catch (AccessDeniedException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Acceso denegado",
                    "No hay permisos suficientes para leer el archivo."
            );
        } catch (IOException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "No se pudo leer el archivo",
                    readableIoMessage(exception)
            );
        } catch (SecurityException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Acceso denegado",
                    "El sistema no permitió acceder al archivo seleccionado."
            );
        }
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
        huffmanTreeView.setTree(result.root());
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
        huffmanTreeView.clear();
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

    private FileChooser createTextFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir archivo de texto");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Archivos de texto (*.txt)", "*.txt"
                ),
                new FileChooser.ExtensionFilter(
                        "Todos los archivos (*.*)", "*.*"
                )
        );
        if (lastDirectory != null
                && lastDirectory.isDirectory()
                && lastDirectory.canRead()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        return chooser;
    }

    private void rememberDirectory(File selectedFile) {
        File parent = selectedFile.getParentFile();
        if (parent != null && parent.isDirectory() && parent.canRead()) {
            lastDirectory = parent;
        }
    }

    private boolean hasReplaceableState() {
        return !originalTextArea.getText().isEmpty() || currentResult != null;
    }

    private boolean confirmReplacement() {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "El texto actual y cualquier resultado de compresión "
                        + "se reemplazarán.",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirmation.setTitle("Reemplazar contenido");
        confirmation.setHeaderText("¿Deseas abrir el archivo seleccionado?");
        if (originalTextArea.getScene() != null) {
            confirmation.initOwner(originalTextArea.getScene().getWindow());
        }
        Optional<ButtonType> response = confirmation.showAndWait();
        return response.isPresent() && response.get() == ButtonType.OK;
    }

    private void loadFileContent(File file, String content) {
        resetResultState();
        originalTextArea.setText(content);
        fileNameLabel.setText(file.getName());
        mainTabPane.getSelectionModel().select(homeTab);

        if (content.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Archivo vacío",
                    "El archivo se cargó correctamente, pero no contiene "
                            + "texto para comprimir."
            );
        }
    }

    private static String readableIoMessage(IOException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("5 MiB")) {
            return "El archivo supera el límite de "
                    + TextFileService.MAX_FILE_SIZE_MIB
                    + " MiB y no fue cargado.";
        }
        if (message != null
                && message.contains("not a regular file")) {
            return "La selección no corresponde a un archivo regular.";
        }
        if (message != null && message.contains("not readable")) {
            return "El archivo seleccionado no se puede leer.";
        }
        return "Ocurrió un error de entrada/salida al leer el archivo.";
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
