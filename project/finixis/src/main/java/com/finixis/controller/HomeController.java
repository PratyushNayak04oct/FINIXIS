package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Credit;
import com.finixis.model.InventoryItem;
import com.finixis.model.Role;
import com.finixis.model.Transaction;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable, PageController {

    @FXML private Label roleLine, pendingCredits, pendingCreditsCount,
            pendingDebits, pendingDebitsCount, lowStock, totalCustomers;
    @FXML private ListView<String> recentList;
    @FXML private Button qaAccounts, qaCredit, qaInventory, qaTxn, qaReport, qaInvoice;

    private MockDataService data;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        roleLine.setText("You are viewing as " + App.getSession().getCurrentRole().getDisplay());

        double credits = data.getCredits().stream().filter(c -> !c.isSettled()).mapToDouble(Credit::getAmount).sum();
        long creditCount = data.getCredits().stream().filter(c -> !c.isSettled()).count();
        pendingCredits.setText(UiUtil.money(credits));
        pendingCreditsCount.setText(creditCount + " open");

        double debits = data.getCustomers().stream().mapToDouble(c -> Math.min(c.getBalance(), 0)).sum();
        long debitCount = data.getCustomers().stream().filter(c -> c.getBalance() < 0).count();
        pendingDebits.setText(UiUtil.money(Math.abs(debits)));
        pendingDebitsCount.setText(debitCount + " customers");

        long low = data.getInventory().stream().filter(InventoryItem::isLowStock).count();
        lowStock.setText(String.valueOf(low));

        totalCustomers.setText(String.valueOf(data.getCustomers().size()));

        List<Transaction> recent = data.getTransactions().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(6).toList();
        recentList.getItems().addAll(recent.stream().map(t ->
                UiUtil.date(t.getDate()) + "  —  " + t.getCustomerName() + "  ·  "
                        + t.getType() + "  ·  " + UiUtil.money(t.getAmount())
                        + (t.isOngoing() ? "   [ongoing]" : "")).toList());
    }

    @Override
    public void applyRole(Role role) {
        roleLine.setText("You are viewing as " + role.getDisplay());
        qaReport.setDisable(!Permissions.canExportOrReport(role));
        qaInvoice.setDisable(!Permissions.canExportOrReport(role));
    }

    @FXML private void goAccounts() { App.getShell().navigate("accounts"); }
    @FXML private void goCredit() { App.getShell().navigate("credit"); }
    @FXML private void goInventory() { App.getShell().navigate("inventory"); }
    @FXML private void goTransactions() { App.getShell().navigate("transactions"); }
    @FXML private void goReports() { App.getShell().navigate("reports"); }
    @FXML private void onCreateInvoice() {
        if (Permissions.canExportOrReport(App.getSession().getCurrentRole())) {
            Dialogs.generated("Invoice");
        }
    }
}
