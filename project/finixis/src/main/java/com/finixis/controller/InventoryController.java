package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.InventoryItem;
import com.finixis.model.Role;
import com.finixis.viewmodel.MockDataService;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.UiUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InventoryController implements Initializable, PageController {

    @FXML private TextField searchField;
    @FXML private Label lowStockLabel;
    @FXML private Button addItemBtn, stockBtn;
    @FXML private TableView<InventoryItem> table;
    @FXML private TableColumn<InventoryItem, String> nameCol, skuCol, catCol;
    @FXML private TableColumn<InventoryItem, Number> qtyCol;
    @FXML private TableColumn<InventoryItem, Double> priceCol;
    @FXML private TableColumn<InventoryItem, InventoryItem> stockCol, actionCol;

    private MockDataService data;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = App.getMockData();

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        stockCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));

        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : UiUtil.money(v));
            }
        });

        stockCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label chip = new Label(item.isLowStock() ? "Low Stock" : "In Stock");
                chip.getStyleClass().add(item.isLowStock() ? "chip,chip-error" : "chip,chip-success");
                setGraphic(chip);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            { editBtn.getStyleClass().add("icon-btn");
              editBtn.setGraphic(new FontIcon("fas-pencil-alt"));
              delBtn.getStyleClass().add("icon-btn,icon-btn-danger");
              delBtn.setGraphic(new FontIcon("fas-trash"));
              editBtn.setOnAction(e -> {
                  InventoryItem it = getTableView().getItems().get(getIndex());
                  if (Permissions.canEditInventoryItem(App.getSession().getCurrentRole()))
                      Dialogs.info("Edit Item", "This would open the Edit Item dialog for " + it.getName() + " (UI-only prototype).");
              });
              delBtn.setOnAction(e -> {
                  InventoryItem it = getTableView().getItems().get(getIndex());
                  if (Permissions.canDeleteInventoryItem(App.getSession().getCurrentRole()))
                      Dialogs.deleteCustomer(it.getName());
              }); }
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(4, editBtn, delBtn);
                setGraphic(box);
            }
        });

        long low = data.getInventory().stream().filter(InventoryItem::isLowStock).count();
        lowStockLabel.setText(low + " items need reorder");
        refresh();
    }

    private void refresh() {
        String q = searchField.getText().toLowerCase().trim();
        List<InventoryItem> list = data.getInventory().stream()
                .filter(i -> q.isEmpty() || i.getName().toLowerCase().contains(q)
                        || i.getSku().toLowerCase().contains(q))
                .toList();
        table.getItems().setAll(list);
    }

    @FXML private void onSearch() { refresh(); }

    @FXML private void onAddItem() {
        if (!Permissions.canAddInventoryItem(App.getSession().getCurrentRole())) return;
        Dialogs.info("Add New Item", "This would open the Add New Item dialog (UI-only prototype).");
    }

    @FXML private void onStock() {
        if (!Permissions.canStockInOut(App.getSession().getCurrentRole())) return;
        Dialogs.info("Stock In / Stock Out", "This would open the Stock In/Out dialog (UI-only prototype).");
    }

    @Override
    public void applyRole(Role role) {
        addItemBtn.setDisable(!Permissions.canAddInventoryItem(role));
        addItemBtn.setOpacity(Permissions.canAddInventoryItem(role) ? 1 : 0.4);
        stockBtn.setDisable(!Permissions.canStockInOut(role));
        stockBtn.setOpacity(Permissions.canStockInOut(role) ? 1 : 0.4);
    }
}
