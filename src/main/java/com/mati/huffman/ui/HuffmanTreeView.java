package com.mati.huffman.ui;

import com.mati.huffman.model.HuffmanNode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

/**
 * JavaFX view that renders a complete Huffman tree at deterministic positions.
 */
public final class HuffmanTreeView extends Pane {

    private static final String EMPTY_MESSAGE =
            "Comprima un texto para generar el árbol";

    private final HuffmanTreeLayout layoutEngine = new HuffmanTreeLayout();

    public HuffmanTreeView() {
        getStyleClass().add("huffman-tree-view");
        clear();
    }

    /**
     * Replaces the current drawing. A {@code null} root restores the
     * placeholder.
     */
    public void setTree(HuffmanNode root) {
        getChildren().clear();
        if (root == null) {
            showMessage(EMPTY_MESSAGE);
            return;
        }

        int leafCount = layoutEngine.countLeaves(root);
        if (leafCount > HuffmanTreeLayout.DEFAULT_MAX_LEAVES) {
            showMessage(
                    "El árbol contiene " + leafCount
                            + " símbolos distintos. La visualización se limita a "
                            + HuffmanTreeLayout.DEFAULT_MAX_LEAVES
                            + " hojas para mantenerla legible."
            );
            return;
        }

        HuffmanTreeLayout.LayoutResult layout = layoutEngine.layout(root);
        setCanvasSize(layout.width(), layout.height());
        layout.edges().forEach(this::renderEdge);
        layout.nodes().forEach(this::renderNode);
    }

    public void clear() {
        getChildren().clear();
        showMessage(EMPTY_MESSAGE);
    }

    private void renderEdge(HuffmanTreeLayout.PositionedEdge edge) {
        HuffmanTreeLayout.PositionedNode parent = edge.parent();
        HuffmanTreeLayout.PositionedNode child = edge.child();
        double startY = parent.centerY() + (parent.height() / 2);
        double endY = child.centerY() - (child.height() / 2);

        Line line = new Line(
                parent.centerX(), startY, child.centerX(), endY
        );
        line.getStyleClass().add("tree-edge");
        getChildren().add(line);

        Label bitLabel = new Label(Integer.toString(edge.bit()));
        bitLabel.getStyleClass().add("tree-edge-label");
        bitLabel.applyCss();
        double ratio = 0.46;
        bitLabel.relocate(
                interpolate(parent.centerX(), child.centerX(), ratio) - 10,
                interpolate(startY, endY, ratio) - 12
        );
        getChildren().add(bitLabel);
    }

    private void renderNode(HuffmanTreeLayout.PositionedNode positioned) {
        HuffmanNode node = positioned.node();
        VBox visual = new VBox(2.0);
        visual.setAlignment(Pos.CENTER);
        visual.setMinSize(positioned.width(), positioned.height());
        visual.setPrefSize(positioned.width(), positioned.height());
        visual.setMaxSize(positioned.width(), positioned.height());
        visual.getStyleClass().addAll(
                "tree-node",
                node.isLeaf() ? "tree-leaf" : "tree-internal"
        );

        if (node.isLeaf()) {
            int symbol = node.symbol().orElseThrow();
            Label symbolLabel = new Label(SymbolFormatter.displaySymbol(symbol));
            symbolLabel.getStyleClass().add("tree-symbol");
            Label frequencyLabel = new Label("f = " + node.frequency());
            frequencyLabel.getStyleClass().add("tree-frequency");
            Label unicodeLabel = new Label(SymbolFormatter.unicodeLabel(symbol));
            unicodeLabel.getStyleClass().add("tree-unicode");
            visual.getChildren().addAll(
                    symbolLabel, frequencyLabel, unicodeLabel
            );
        } else {
            Label frequencyLabel = new Label("f = " + node.frequency());
            frequencyLabel.getStyleClass().add("tree-frequency");
            visual.getChildren().add(frequencyLabel);
        }

        visual.relocate(
                positioned.centerX() - (positioned.width() / 2),
                positioned.centerY() - (positioned.height() / 2)
        );
        getChildren().add(visual);
    }

    private void showMessage(String message) {
        setCanvasSize(820.0, 560.0);
        Label placeholder = new Label(message);
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getStyleClass().add("tree-message");
        placeholder.setPrefWidth(520.0);
        placeholder.relocate(150.0, 245.0);
        getChildren().add(placeholder);
    }

    private void setCanvasSize(double width, double height) {
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
    }

    private static double interpolate(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }
}
