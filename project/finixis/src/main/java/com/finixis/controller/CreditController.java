package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Credit;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.TransactionService;
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

/**
 * Credits page — shows all Transaction_Credit rows (Credit = money customer owes us).
 * Mapped from Transaction objects (type=CREDIT) retrieved from DB.
 */
public class CreditController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo, sortCombo;
    @FXML private Button addCreditBtn;
    @FXML private TableView<Credit> table;
    @FXML private TableColumn<Credit, String> customerCol, descCol;
    @FXML private TableColumn<Credit, Boolean> statusCol;
    @FXML private TableColumn<Credit, Double> amountCol;
    @FXML private TableColumn<Credit, LocalDate> issuedCol, dueCol;
    @FXML private TableColumn<Credit, Credit> actionCol;

    private TransactionService txnService;
    private List<Credit> allCredits;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txnService = AppServices.transactions();

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
                setText(empty || d == null ? "—" : UiUtil.date(d));
            }
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean settled, boolean empty) {
                super.updateItem(settled, empty);
                if (empty || settled == null) { setText(""); setGraphic(null); return; }
                Label chip = new Label(settled ? "Settled" : "Pending");
                chip.getStyleClass().addAll("chip", settled ? "chip-success" : "chip-warning");
                setGraphic(chip);
                setText(null);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button settleBtn = new Button("Mark Settled");
            {
                settleBtn.getStyleClass().addAll("btn", "btn-secondary");
                settleBtn.setOnAction(e -> {
                    Credit c = getTableView().getItems().get(getIndex());
                    if (!c.isSettled()) {
                        boolean ok = Dialogs.confirm("Mark Settled",
                                "Mark this credit as fully settled?",
                                "This will update the balance and mark the transaction as All Cleared.");
                        if (ok) {
                            txnService.markSettled(c.getId());
                            c.setSettled(true);
                            loadFromDb();
                        }
                    }
                });
            }
            @Override protected void updateItem(Credit c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null || c.isSettled()) { setGraphic(null); return; }
                setGraphic(settleBtn);
            }
        });

        loadFromDb();
    }

    private void loadFromDb() {
        allCredits = txnService.getAllCredits().stream()
                .map(this::toCredit)
                .collect(Collectors.toList());
        refresh();
    }

    /** Map a Transaction (type=CREDIT) to the Credit view model. */
    private Credit toCredit(Transaction t) {
        Credit c = new Credit();
        c.setId(t.getId());
        c.setCustomerId(t.getCustomerId());
        c.setCustomerName(t.getCustomerName());
        c.setAmount(t.getAmount());
        c.setDescription(t.getDescription());
        c.setDateIssued(t.getDate());
        c.setDueDate(null); // no due date in DB schema — shown as "—"
        c.setSettled(!t.isOngoing());
        return c;
    }

    private List<Credit> filtered() {
        String q = searchField.getText().toLowerCase().trim();
        String status = statusCombo.getValue();
        List<Credit> list = allCredits.stream()
                .filter(c -> q.isEmpty()
                        || (c.getCustomerName() != null && c.getCustomerName().toLowerCase().contains(q))
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)))
                .filter(c -> switch (status) {
                    case "Pending" -> !c.isSettled();
                    case "Settled" -> c.isSettled();
                    default -> true;
                })
                .collect(Collectors.toList());

        list.sort(switch (sortCombo.getValue()) {
            case "Oldest"      -> Comparator.comparing(Credit::getDateIssued);
            case "Amount High" -> Comparator.comparing(Credit::getAmount).reversed();
            case "Amount Low"  -> Comparator.comparing(Credit::getAmount);
            default            -> Comparator.comparing(Credit::getDateIssued).reversed();
        });
        return list;
    }

    private void refresh() { table.getItems().setAll(filtered()); }

    @FXML private void onSearch() { refresh(); }
    @FXML private void onFilter() { refresh(); }
    @FXML private void onSort()   { refresh(); }

    @FXML private void onAddCredit() {
        Dialogs.info("Add New Credit",
                "Credits are created automatically when you record a payment on a customer's page.\n\n"
                + "Navigate to Accounts → open a customer → Record Payment to add a credit entry.");
    }
}
