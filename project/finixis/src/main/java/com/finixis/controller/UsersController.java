package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.model.User;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class UsersController implements Initializable, PageController {

    @FXML private Button addUserBtn;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> nameCol, emailCol, phoneCol;
    @FXML private TableColumn<User, Role> roleCol;
    @FXML private TableColumn<User, Boolean> statusCol;
    @FXML private TableColumn<User, User> actionCol;

    private MockDataService data;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }
                Label chip = new Label(r.getDisplay());
                chip.getStyleClass().add(switch (r) {
                    case ADMIN -> "chip,chip-error";
                    case MANAGER -> "chip,chip-primary";
                    case EMPLOYEE -> "chip,chip-success";
                });
                setGraphic(chip);
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); return; }
                Label chip = new Label(a ? "Active" : "Inactive");
                chip.getStyleClass().add(a ? "chip,chip-success" : "chip,chip-warning");
                setGraphic(chip);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            { editBtn.getStyleClass().add("icon-btn");
              editBtn.setGraphic(new FontIcon("fas-pencil-alt"));
              editBtn.setOnAction(e -> {
                  User u = getTableView().getItems().get(getIndex());
                  if (Permissions.canManageUsers(App.getSession().getCurrentRole()))
                      Dialogs.info("Edit User", "This would open the Add/Edit User dialog for " + u.getName() + " (UI-only prototype).");
              }); }
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setGraphic(empty || u == null ? null : editBtn);
            }
        });

        table.getItems().setAll(data.getUsers());
    }

    @FXML private void onAddUser() {
        if (!Permissions.canManageUsers(App.getSession().getCurrentRole())) return;
        Dialogs.info("Add New User", "This would open the Add/Edit User dialog (UI-only prototype).");
    }

    @Override
    public void applyRole(Role role) {
        boolean can = Permissions.canManageUsers(role);
        addUserBtn.setDisable(!can);
        addUserBtn.setOpacity(can ? 1 : 0.4);
    }
}
