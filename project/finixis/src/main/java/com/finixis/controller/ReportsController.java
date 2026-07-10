package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.model.Transaction;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ReportsController implements Initializable, PageController {

    @FXML private ComboBox<String> rangeCombo;
    @FXML private Label totalCredits, totalDebits, netLabel;
    @FXML private Button exportBtn;
    @FXML private BarChart<String, Number> chart;

    private MockDataService data;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();
        rangeCombo.getItems().addAll("Last 30 days", "Last 90 days", "This Year");
        rangeCombo.getSelectionModel().select("Last 90 days");

        double credits = data.getTransactions().stream()
                .filter(t -> t.getType() == Transaction.Type.CREDIT).mapToDouble(Transaction::getAmount).sum();
        double payments = data.getTransactions().stream()
                .filter(t -> t.getType() == Transaction.Type.PAYMENT).mapToDouble(Transaction::getAmount).sum();
        double debits = data.getTransactions().stream()
                .filter(t -> t.getType() == Transaction.Type.DEBIT).mapToDouble(Transaction::getAmount).sum();

        totalCredits.setText(UiUtil.money(credits));
        totalDebits.setText(UiUtil.money(debits + payments));
        double net = credits - payments - debits;
        netLabel.setText(UiUtil.signedMoney(net));
        netLabel.setStyle(net >= 0 ? "-fx-text-fill: -success-600;" : "-fx-text-fill: -error-600;");

        // mock monthly chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        chart.setTitle("Credits vs Payments by Month");
        XYChart.Series<String, Number> creditSeries = new XYChart.Series<>();
        creditSeries.setName("Credits");
        creditSeries.getData().addAll(
                new XYChart.Data<>("Jan", 12400), new XYChart.Data<>("Feb", 9800),
                new XYChart.Data<>("Mar", 15600), new XYChart.Data<>("Apr", 11200),
                new XYChart.Data<>("May", 8900), new XYChart.Data<>("Jun", 14200),
                new XYChart.Data<>("Jul", 18900));
        XYChart.Series<String, Number> paySeries = new XYChart.Series<>();
        paySeries.setName("Payments");
        paySeries.getData().addAll(
                new XYChart.Data<>("Jan", 8000), new XYChart.Data<>("Feb", 11200),
                new XYChart.Data<>("Mar", 6400), new XYChart.Data<>("Apr", 9800),
                new XYChart.Data<>("May", 7200), new XYChart.Data<>("Jun", 11500),
                new XYChart.Data<>("Jul", 9300));
        chart.getData().addAll(creditSeries, paySeries);
    }

    @FXML private void onExport() {
        if (!Permissions.canExportOrReport(App.getSession().getCurrentRole())) return;
        Dialogs.generated("Report");
    }

    @Override
    public void applyRole(Role role) {
        exportBtn.setDisable(!Permissions.canExportOrReport(role));
        exportBtn.setOpacity(Permissions.canExportOrReport(role) ? 1 : 0.4);
    }
}
