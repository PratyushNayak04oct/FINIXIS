module com.finixis {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.finixis to javafx.fxml;
    opens com.finixis.controller to javafx.fxml;
    opens com.finixis.viewmodel to javafx.fxml;

    exports com.finixis;
}