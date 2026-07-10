package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Transaction;
import com.finixis.service.AppServices;
import com.finixis.service.CustomerService;
import com.finixis.service.InventoryService;
import com.finixis.service.TransactionService;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable, PageController {

    @FXML private Label todayLabel, pendingCredits, pendingCreditsCount,
            pendingDebits, pendingDebitsCount, lowStock, totalCustomers;
    @FXML private ListView<String> recentList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CustomerService    customers    = AppServices.customers();
        InventoryService   inventory    = AppServices.inventory();
        TransactionService transactions = AppServices.transactions();

        todayLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        // Pending credits = outstanding balance on unpaid credit transactions
        double creditTotal = transactions.totalCreditOutstanding();
        long   creditCount = transactions.getAllCredits().stream().filter(Transaction::isOngoing).count();
        pendingCredits.setText(UiUtil.money(creditTotal));
        pendingCreditsCount.setText(creditCount + " open");

        // Pending debits = money we owe customers (negative balance side)
        double debitTotal = transactions.totalDebits();
        long   debitCount = transactions.getAllDebits().size();
        pendingDebits.setText(UiUtil.money(debitTotal));
        pendingDebitsCount.setText(debitCount + " entries");

        lowStock.setText(String.valueOf(inventory.getLowStockCount()));
        totalCustomers.setText(String.valueOf(customers.getAll().size()));

        List<Transaction> recent = transactions.getAll().stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(6).toList();
        recentList.getItems().addAll(recent.stream().map(t ->
                UiUtil.date(t.getDate()) + "  —  " + t.getCustomerName() + "  ·  "
                        + t.getType() + "  ·  " + UiUtil.money(t.getAmount())
                        + (t.isOngoing() ? "   [pending]" : "")).toList());
    }

    @FXML private void goAccounts()     { App.getShell().navigate("accounts"); }
    @FXML private void goCredit()       { App.getShell().navigate("credit"); }
    @FXML private void goInventory()    { App.getShell().navigate("inventory"); }
    @FXML private void goTransactions() { App.getShell().navigate("transactions"); }
    @FXML private void goReports()      { App.getShell().navigate("reports"); }
    @FXML private void onCreateInvoice() {
        Dialogs.info("Create Invoice",
                "Navigate to the Transaction History page and click 'Create Invoice' to generate a real PDF/Excel invoice file.");
    }
}
