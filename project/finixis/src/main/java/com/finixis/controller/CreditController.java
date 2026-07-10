package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Credit;
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
import java.util.*;
import java.util.stream.Collectors;

public class CreditController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo, sortCombo;
    @FXML private Button addCreditBtn;
    @FXML private TableView<Credit> table;
    @FXML private TableColumn<Credit, String> customerCol, descCol, statusCol;
    @FXML private TableColumn<Credit, Double> amountCol;
    @FXML private TableColumn<Credit, LocalDate> issuedCol, dueCol;
    @FXML private TableColumn<Credit, Credit> actionCol;

    private MockDataService data;
    private List<Credit> allCredits;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        allCredits = new ArrayList<>(data.getCredits());

        statusCombo.getItems().addAll("All", "Pending", "Settled");
        statusCombo.getSelectionModel().select(0);
        sortCombo.getItems().addAll("Newest", "Oldest", "Amount High", "Amount Low");
        sortCombo.getSelectionModel().select(0);

        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        issuedCol.setCellValueFactory(new PropertyValueFactory<>("dateIssued"));
        dueCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("settled"));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(""); return; }
                setText(UiUtil.money(v));
                setStyle("-fx-font-weight:700;");
            }
        });

        issuedCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });
        dueCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? "" : UiUtil.date(d));
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(""); setGraphic(null); return; }
                boolean settled = Boolean.parseBoolean(s);
                Label chip = new Label(settled ? "Settled" : "Pending");
                chip.getStyleClass().add(settled ? "chip,chip-success" : "chip,chip-warning");
                setGraphic(chip);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button settleBtn = new Button("Mark Settled");
            { settleBtn.getStyleClass().add("btn,btn-secondary");
              settleBtn.setOnAction(e -> {
                  Credit c = getTableView().getItems().get(getIndex());
                  if (Permissions.canMarkCreditSettled(App.getSession().getCurrentRole())) {
                      c.setSettled(true);
                      Dialogs.markSettled("credit");
                      refresh();
                  }
              }); }
            @Override protected void updateItem(Credit c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setGraphic(null); return; }
                settleBtn.setDisable(c.isSettled());
                setGraphic(c.isSettled() ? null : settleBtn);
            }
        });

        refresh();
    }

    private List<Credit> filtered() {
        String q = searchField.getText().toLowerCase().trim();
        String status = statusCombo.getValue();
        List<Credit> list = allCredits.stream()
                .filter(c -> q.isEmpty() || c.getCustomerName().toLowerCase().contains(q)
                        || c.getDescription().toLowerCase().contains(q))
                .filter(c -> status.equals("All") || (status.equals("Pending") && !c.isSettled())
                        || (status.equals("Settled") && c.isSettled()))
                .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        list.sort(switch (sort) {
            case "Oldest" -> Comparator.comparing(Credit::getDateIssued);
            case "Amount High" -> Comparator.comparing(Credit::getAmount).reversed();
            case "Amount Low" -> Comparator.comparing(Credit::getAmount);
            default -> Comparator.comparing(Credit::getDateIssued).reversed();
        });
        return list;
    }

    private void refresh() { table.getItems().setAll(filtered()); }

    @FXML private void onSearch() { refresh(); }
    @FXML private void onFilter() { refresh(); }
    @FXML private void onSort() { refresh(); }

    @FXML private void onAddCredit() {
        if (!Permissions.canAddCredit(App.getSession().getCurrentRole())) return;
        Dialogs.info("Add New Credit", "This would open the Add Credit dialog (UI-only prototype).");
    }

    @Override
    public void applyRole(Role role) {
        addCreditBtn.setDisable(!Permissions.canAddCredit(role));
        addCreditBtn.setOpacity(Permissions.canAddCredit(role) ? 1 : 0.4);
    }
}
