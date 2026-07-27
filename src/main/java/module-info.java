module com.example.huffmanvisualizer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.example.huffmanvisualizer to javafx.fxml;
    exports com.example.huffmanvisualizer;
}