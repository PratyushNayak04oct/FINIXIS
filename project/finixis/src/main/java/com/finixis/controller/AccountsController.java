package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Customer;
import com.finixis.model.Role;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AccountsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Button addCustomerBtn;
    @FXML private TableView<Customer> table;
    @FXML private TableColumn<Customer, String> nameCol, contactCol;
    @FXML private TableColumn<Customer, LocalDate> sinceCol;
    @FXML private TableColumn<Customer, Double> balanceCol;
    @FXML private TableColumn<Customer, Customer> actionCol;

    private MockDataService data;
    private List<Customer> allCustomers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        allCustomers = data.getCustomers().stream()
                .sorted(Comparator.comparing(Customer::getName)).collect(Collectors.toList());

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        contactCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        sinceCol.setCellValueFactory(new PropertyValueFactory<>("customerSince"));
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        sinceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });

        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(""); return; }
                setText(UiUtil.signedMoney(v));
                setStyle(v >= 0 ? "-fx-text-fill: -success-600; -fx-font-weight:700;" : "-fx-text-fill: -error-600; -fx-font-weight:700;");
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            { openBtn.getStyleClass().add("btn,btn-secondary");
              openBtn.setOnAction(e -> App.getShell().openCustomer(getTableView().getItems().get(getIndex()).getId())); }
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setGraphic(empty || c == null ? null : openBtn);
            }
        });

        refresh(allCustomers);
    }

    private void refresh(List<Customer> list) {
        table.getItems().setAll(list);
        countLabel.setText(list.size() + " customers");
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) { refresh(allCustomers); return; }
        refresh(allCustomers.stream().filter(c ->
                c.getName().toLowerCase().contains(q) ||
                c.getEmail().toLowerCase().contains(q) ||
                c.getPhone().toLowerCase().contains(q)).toList());
    }

    @FXML private void onAddCustomer() {
        if (!Permissions.canAddCustomer(App.getSession().getCurrentRole())) return;
        Dialogs.info("Add Customer", "This would open the Add Customer dialog (UI-only prototype).");
    }

    @Override
    public void applyRole(Role role) {
        addCustomerBtn.setDisable(!Permissions.canAddCustomer(role));
        addCustomerBtn.setOpacity(Permissions.canAddCustomer(role) ? 1 : 0.4);
    }
}
