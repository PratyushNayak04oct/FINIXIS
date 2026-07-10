module com.finixis {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.desktop;
    requires java.sql;

    // Database stack
    requires com.zaxxer.hikari;   // HikariCP connection pool
    requires java.sql;            // JDBC API
    requires com.h2database;      // H2 embedded driver + Trigger API

    // SLF4J (required by HikariCP)
    requires org.slf4j;

    opens com.finixis          to javafx.fxml;
    opens com.finixis.controller to javafx.fxml;
    opens com.finixis.viewmodel  to javafx.fxml;
    opens com.finixis.model      to javafx.base, javafx.fxml;

    // Open db package to H2 so it can instantiate our BalanceTrigger via reflection
    opens com.finixis.db to com.h2database;

    exports com.finixis;
    exports com.finixis.service;
}
