package com.finixis;

import com.finixis.controller.ShellController;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.SessionState;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene primaryScene;
    private static MockDataService mockData;
    private static SessionState session;
    private static StackPane root;
    private static ShellController shell;

    @Override
    public void start(Stage stage) throws Exception {
        mockData = new MockDataService();
        session = new SessionState(mockData);

        root = new StackPane();
        primaryScene = new Scene(root, 1280, 820);
        applyTheme(false);
        stage.setTitle("Finixis");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(primaryScene);
        navigate("shell");
        stage.show();
    }

    public static void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
            Parent view = loader.load();
            root.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load /fxml/" + fxml + ".fxml", e);
        }
    }

    public static Scene getScene() { return primaryScene; }
    public static StackPane getRoot() { return root; }
    public static MockDataService getMockData() { return mockData; }
    public static SessionState getSession() { return session; }
    public static ShellController getShell() { return shell; }
    public static void setShell(ShellController s) { shell = s; }

    public static void applyTheme(boolean dark) {
        primaryScene.getStylesheets().clear();
        primaryScene.getStylesheets().add(App.class.getResource(
                dark ? "/css/dark-theme.css" : "/css/light-theme.css").toExternalForm());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
