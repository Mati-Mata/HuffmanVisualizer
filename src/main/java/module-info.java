module com.mati.huffman {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    exports com.mati.huffman;
    opens com.mati.huffman.controller to javafx.fxml;
}
