package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.model.Transaction;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionsController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeCombo, dateRangeCombo;
    @FXML private VBox groupedBox;
    @FXML private Button exportBtn, reportBtn, invoiceBtn, addTxnBtn;

    private MockDataService data;
    private static final String ALL = "All time", D90 = "Last 90 days", D30 = "Last 30 days";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        typeCombo.getItems().addAll("All Types", "Credit", "Debit", "Payment");
        typeCombo.getSelectionModel().select(0);
        dateRangeCombo.getItems().addAll(D30, D90, ALL);
        dateRangeCombo.getSelectionModel().select(ALL);
        render();
    }

    private void render() {
        groupedBox.getChildren().clear();
        String q = searchField.getText().toLowerCase().trim();
        String type = typeCombo.getValue();
        LocalDate cutoff = switch (dateRangeCombo.getValue()) {
            case D30 -> LocalDate.now().minusDays(30);
            case D90 -> LocalDate.now().minusDays(90);
            default -> LocalDate.of(1900, 1, 1);
        };

        List<Transaction> txns = data.getTransactions().stream()
                .filter(t -> q.isEmpty() || t.getCustomerName().toLowerCase().contains(q)
                        || t.getDescription().toLowerCase().contains(q))
                .filter(t -> type.equals("All Types") || t.getType().name().equalsIgnoreCase(type))
                .filter(t -> t.getDate().isAfter(cutoff) || t.getDate().isEqual(cutoff))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());

        LinkedHashMap<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction t : txns) {
            grouped.computeIfAbsent(UiUtil.dateRangeLabel(t.getDate()), k -> new ArrayList<>()).add(t);
        }

        if (txns.isEmpty()) {
            Label empty = new Label("No transactions found.");
            empty.setStyle("-fx-text-fill: -text-muted; -fx-padding: 16 0;");
            groupedBox.getChildren().add(empty);
            return;
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            Label header = new Label(entry.getKey().toUpperCase() + "  (" + entry.getValue().size() + ")");
            header.getStyleClass().add("date-group-header");
            groupedBox.getChildren().add(header);
            for (Transaction t : entry.getValue()) {
                groupedBox.getChildren().add(buildRow(t));
            }
        }
    }

    private HBox buildRow(Transaction t) {
        HBox row = new HBox(14);
        row.getStyleClass().add("txn-row");
        if (t.isOngoing()) row.getStyleClass().add("txn-row-ongoing");
        row.setOnMouseClicked(e -> App.getShell().openCustomer(t.getCustomerId()));

        VBox left = new VBox(3);
        Label desc = new Label(t.getDescription());
        desc.getStyleClass().add("txn-desc");
        Label meta = new Label(t.getCustomerName() + "  ·  " + UiUtil.date(t.getDate()) + "  ·  " + t.getType());
        meta.getStyleClass().add("txn-meta");
        left.getChildren().addAll(desc, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(3);
        right.setAlignment(Pos.CENTER_RIGHT);
        Label amt = new Label(UiUtil.signedMoney(t.getType() == Transaction.Type.PAYMENT ? -t.getAmount() : t.getAmount()));
        amt.getStyleClass().add("txn-amount");
        amt.setStyle(t.getType() == Transaction.Type.PAYMENT ? "-fx-text-fill: -success-600;" : "-fx-text-fill: -text;");
        Label badge = new Label(t.isOngoing() ? "ONGOING" : "SETTLED");
        badge.getStyleClass().add(t.isOngoing() ? "chip,chip-warning" : "chip,chip-success");
        right.getChildren().addAll(amt, badge);

        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    @FXML private void onSearch() { render(); }
    @FXML private void onFilter() { render(); }

    @FXML private void onAddTxn() {
        if (!Permissions.canAddTransaction(App.getSession().getCurrentRole())) return;
        Dialogs.info("New Transaction", "This would open the Add New Transaction dialog (UI-only prototype).");
    }
    @FXML private void onExport() {
        if (!Permissions.canExportOrReport(App.getSession().getCurrentRole())) return;
        Dialogs.generated("Report");
    }
    @FXML private void onReport() {
        if (!Permissions.canExportOrReport(App.getSession().getCurrentRole())) return;
        Dialogs.generated("Report");
    }
    @FXML private void onInvoice() {
        if (!Permissions.canExportOrReport(App.getSession().getCurrentRole())) return;
        Dialogs.generated("Invoice");
    }

    @Override
    public void applyRole(Role role) {
        boolean canExp = Permissions.canExportOrReport(role);
        exportBtn.setDisable(!canExp);
        exportBtn.setOpacity(canExp ? 1 : 0.4);
        reportBtn.setDisable(!canExp);
        reportBtn.setOpacity(canExp ? 1 : 0.4);
        invoiceBtn.setDisable(!canExp);
        invoiceBtn.setOpacity(canExp ? 1 : 0.4);
        addTxnBtn.setDisable(!Permissions.canAddTransaction(role));
        addTxnBtn.setOpacity(Permissions.canAddTransaction(role) ? 1 : 0.4);
    }
}
