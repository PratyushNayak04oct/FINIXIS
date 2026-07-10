package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Customer;
import com.finixis.model.Role;
import com.finixis.model.Transaction;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CustomerDetailController implements Initializable, PageController {

    @FXML private Label avatar, nameLabel, contactLabel, sinceLabel, balanceLabel;
    @FXML private Label chipTxns, chipLast, chipOngoing;
    @FXML private Button addCreditBtn, addDebitBtn, recordPaymentBtn, deleteBtn;
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private VBox historyBox;

    private MockDataService data;
    private Customer customer;
    private static final String ALL = "All time";
    private static final String D90 = "Last 90 days";
    private static final String D30 = "Last 30 days";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        dateRangeCombo.getItems().addAll(D30, D90, ALL);
        dateRangeCombo.getSelectionModel().select(ALL);
    }

    public void loadCustomer(int customerId) {
        customer = data.findCustomer(customerId);
        if (customer == null) return;

        avatar.setText(customer.getInitials());
        nameLabel.setText(customer.getName());
        contactLabel.setText(customer.getEmail() + "  ·  " + customer.getPhone());
        sinceLabel.setText("Customer since " + UiUtil.date(customer.getCustomerSince()));

        double bal = customer.getBalance();
        balanceLabel.setText(UiUtil.signedMoney(bal));
        balanceLabel.setStyle(bal >= 0
                ? "-fx-text-fill: -success-600;"
                : "-fx-text-fill: -error-600;");

        List<Transaction> txns = data.transactionsFor(customerId);
        chipTxns.setText(txns.size() + " transactions");
        long ongoing = txns.stream().filter(Transaction::isOngoing).count();
        chipOngoing.setText(ongoing + " ongoing");
        Optional<LocalDate> last = txns.stream().map(Transaction::getDate)
                .max(Comparator.naturalOrder());
        chipLast.setText("Last activity: " + last.map(UiUtil::date).orElse("—"));

        renderHistory();
    }

    private void renderHistory() {
        historyBox.getChildren().clear();
        if (customer == null) return;

        String range = dateRangeCombo.getValue();
        LocalDate cutoff = switch (range) {
            case D30 -> LocalDate.now().minusDays(30);
            case D90 -> LocalDate.now().minusDays(90);
            default -> LocalDate.of(1900, 1, 1);
        };

        List<Transaction> txns = data.transactionsFor(customer.getId()).stream()
                .filter(t -> t.getDate().isAfter(cutoff) || t.getDate().isEqual(cutoff))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .toList();

        // Group by date-range label
        LinkedHashMap<String, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction t : txns) {
            grouped.computeIfAbsent(UiUtil.dateRangeLabel(t.getDate()), k -> new ArrayList<>()).add(t);
        }

        if (txns.isEmpty()) {
            Label empty = new Label("No transactions in this range.");
            empty.setStyle("-fx-text-fill: -text-muted; -fx-padding: 16 0;");
            historyBox.getChildren().add(empty);
            return;
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            Label header = new Label(entry.getKey().toUpperCase());
            header.getStyleClass().add("date-group-header");
            historyBox.getChildren().add(header);

            for (Transaction t : entry.getValue()) {
                historyBox.getChildren().add(buildTxnRow(t));
            }
        }
    }

    private HBox buildTxnRow(Transaction t) {
        HBox row = new HBox(14);
        row.getStyleClass().add("txn-row");
        if (t.isOngoing()) row.getStyleClass().add("txn-row-ongoing");

        VBox left = new VBox(3);
        Label desc = new Label(t.getDescription());
        desc.getStyleClass().add("txn-desc");
        Label meta = new Label(UiUtil.date(t.getDate()) + "  ·  " + t.getType());
        meta.getStyleClass().add("txn-meta");
        left.getChildren().addAll(desc, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(3);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label amt = new Label(UiUtil.signedMoney(t.getType() == Transaction.Type.PAYMENT ? -t.getAmount() : t.getAmount()));
        amt.getStyleClass().add("txn-amount");
        amt.setStyle(t.getType() == Transaction.Type.PAYMENT
                ? "-fx-text-fill: -success-600;" : "-fx-text-fill: -text;");
        Label badge = new Label(t.isOngoing() ? "ONGOING" : "SETTLED");
        badge.getStyleClass().add(t.isOngoing() ? "chip,chip-warning" : "chip,chip-success");
        right.getChildren().addAll(amt, badge);

        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    @FXML private void onDateRange() { renderHistory(); }

    @FXML private void onBack() { App.getShell().navigate("accounts"); }

    @FXML private void onAddCredit() {
        Role r = App.getSession().getCurrentRole();
        if (!Permissions.canAddCredit(r)) return;
        Dialogs.info("Add Credit", "This would open the Add Credit dialog for " + customer.getName() + " (UI-only prototype).");
    }

    @FXML private void onAddDebit() {
        Dialogs.info("Add Debit", "This would open the Add Debit dialog for " + customer.getName() + " (UI-only prototype).");
    }

    @FXML private void onRecordPayment() {
        Dialogs.info("Record Payment", "This would open the Record Payment dialog for " + customer.getName() + " (UI-only prototype).");
    }

    @FXML private void onDelete() {
        Role r = App.getSession().getCurrentRole();
        if (!Permissions.canDeleteCustomer(r)) return;
        Dialogs.deleteCustomer(customer.getName());
    }

    @Override
    public void applyRole(Role role) {
        addCreditBtn.setDisable(!Permissions.canAddCredit(role));
        addCreditBtn.setOpacity(Permissions.canAddCredit(role) ? 1 : 0.4);
        addDebitBtn.setDisable(!Permissions.canRecordPayment(role));
        addDebitBtn.setOpacity(Permissions.canRecordPayment(role) ? 1 : 0.4);
        recordPaymentBtn.setDisable(!Permissions.canRecordPayment(role));
        recordPaymentBtn.setOpacity(Permissions.canRecordPayment(role) ? 1 : 0.4);
        deleteBtn.setDisable(!Permissions.canDeleteCustomer(role));
        deleteBtn.setOpacity(Permissions.canDeleteCustomer(role) ? 1 : 0.4);
    }
}
