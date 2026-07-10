package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Customer;
import com.finixis.model.InventoryItem;
import com.finixis.model.Transaction;
import com.finixis.model.TransactionLineItem;
import com.finixis.service.AppServices;
import com.finixis.viewmodel.ThemeManager;
import com.finixis.viewmodel.UiUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central place for all dialogs. Every dialog registers its Scene with ThemeManager
 * so the live theme toggle applies to open dialogs correctly.
 */
public final class Dialogs {
    private Dialogs() {}

    // ─── Generic / system dialogs ──────────────────────────────────────────────

    public static boolean confirm(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        applyThemeOnShow(a);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        applyThemeOnShow(a);
        a.showAndWait();
    }

    public static void signOut(StackPane root) {
        boolean ok = confirm("Sign Out", "Sign out of Finixis?",
                "This is a UI-only prototype with no real session. This is a simulated sign-out.");
        if (ok) UiUtil.toast(root, "Signed out (simulated)");
    }

    // ─── Add Customer dialog ──────────────────────────────────────────────────

    public static void showAddCustomer(Consumer<Customer> onSaved) {
        Stage stage = buildDialogStage("Add New Customer", 480, 420);
        VBox root = dialogRoot();

        Label title = sectionTitle("Add New Customer");
        TextField nameField    = styledField("Full name");
        TextField phoneField   = styledField("+91-XXXXXXXXXX");
        TextField emailField   = styledField("email@example.com");
        TextField addressField = styledField("City / Address");
        Label err = errLabel();

        Button cancelBtn  = new Button("Cancel");  cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Add Customer"); confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                title, new Separator(),
                labeledField("Customer Name *", nameField),
                labeledField("Phone", phoneField),
                labeledField("Email", emailField),
                labeledField("Location", addressField),
                err,
                actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Customer name is required."); return; }
            try {
                Customer saved = AppServices.customers().addCustomer(
                        name, phoneField.getText().trim(),
                        emailField.getText().trim(), addressField.getText().trim());
                stage.close();
                if (onSaved != null) onSaved.accept(saved);
            } catch (Exception ex) {
                err.setText("Error: " + ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // ─── Add Debit dialog ──────────────────────────────────────────────────────

    public static void showAddDebit(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Add Debit — " + customer.getName(), 460, 320);
        VBox root = dialogRoot();

        Label title = sectionTitle("Add Debit");
        Label sub   = new Label("Record an amount owed TO " + customer.getName());
        sub.getStyleClass().add("text-muted");

        TextField amountField = styledField("0.00");
        TextField notesField  = styledField("Optional notes");
        Label err = errLabel();

        Button cancelBtn  = new Button("Cancel");        cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Confirm Debit"); confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                labeledField("Customer Name", readonlyField(customer.getName())),
                labeledField("Amount (₹) *", amountField),
                labeledField("Notes", notesField),
                err,
                actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid positive amount.");
                return;
            }
            try {
                AppServices.transactions().addDebit(
                        customer.getId(), customer.getName(),
                        amount, notesField.getText().trim());
                stage.close();
                if (onConfirm != null) onConfirm.run();
            } catch (Exception ex) {
                err.setText("Error saving: " + ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // ─── Record Payment dialog ─────────────────────────────────────────────────

    public static void showRecordPayment(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Record Payment — " + customer.getName(), 600, 560);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        VBox root = dialogRoot();
        scroll.setContent(root);

        Label title = sectionTitle("Record Payment");
        Label sub   = new Label("Customer: " + customer.getName() + "  ·  Select items and enter amount paid.");
        sub.getStyleClass().add("text-muted");

        List<InventoryItem> inventory = AppServices.inventory().getAll();

        VBox itemsBox = new VBox(10);
        Label totalAmountLabel = new Label("₹0.00");
        totalAmountLabel.getStyleClass().addAll("font-bold", "text-primary");
        totalAmountLabel.setStyle("-fx-font-size:16px;");

        TextField paidField = styledField("0.00");
        Label remainingLabel = new Label("₹0.00");
        remainingLabel.getStyleClass().add("font-bold");
        remainingLabel.setStyle("-fx-font-size:14px;");
        Label err = errLabel();

        List<ComboBox<InventoryItem>> itemCombos = new ArrayList<>();
        List<TextField>              qtyFields   = new ArrayList<>();

        Runnable updateTotals = () -> {
            double total = 0;
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item != null) {
                    try {
                        int qty = Integer.parseInt(qtyFields.get(i).getText().trim());
                        if (qty > 0) total += item.getUnitPrice() * qty;
                    } catch (NumberFormatException ignored) {}
                }
            }
            double t = total;
            totalAmountLabel.setText(UiUtil.money(t));
            double paid = 0;
            try { paid = Double.parseDouble(paidField.getText().trim()); } catch (Exception ignored) {}
            double remaining = t - paid;
            remainingLabel.setText(UiUtil.money(remaining));
            remainingLabel.getStyleClass().removeAll("text-error", "text-success");
            remainingLabel.getStyleClass().add(remaining > 0 ? "text-error" : "text-success");
        };

        Runnable[] addRowRef = new Runnable[1];
        addRowRef[0] = () -> {
            ComboBox<InventoryItem> combo = new ComboBox<>();
            combo.getItems().addAll(inventory);
            combo.setPromptText("Select item…");
            combo.setPrefWidth(260);
            combo.getStyleClass().add("combo");
            combo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(InventoryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null
                            : item.getName() + "  (" + UiUtil.money(item.getUnitPrice()) + "/unit)");
                }
            });
            combo.setButtonCell(combo.getCellFactory().call(null));
            combo.valueProperty().addListener((obs, o, n) -> updateTotals.run());

            TextField qtyField = styledField("1");
            qtyField.setPrefWidth(80);
            qtyField.textProperty().addListener((obs, o, n) -> updateTotals.run());

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().addAll("btn", "btn-secondary");
            removeBtn.setStyle("-fx-padding: 4 8;");

            HBox row = new HBox(10, combo, new Label("Qty:"), qtyField, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);

            itemCombos.add(combo);
            qtyFields.add(qtyField);
            itemsBox.getChildren().add(row);

            removeBtn.setOnAction(e -> {
                int idx = itemsBox.getChildren().indexOf(row);
                if (idx >= 0 && itemsBox.getChildren().size() > 1) {
                    itemsBox.getChildren().remove(row);
                    itemCombos.remove(idx);
                    qtyFields.remove(idx);
                    updateTotals.run();
                }
            });
        };
        addRowRef[0].run();

        Button addMoreBtn = new Button("+ Add another item");
        addMoreBtn.getStyleClass().addAll("btn", "btn-ghost");
        addMoreBtn.setOnAction(e -> { addRowRef[0].run(); updateTotals.run(); });
        paidField.textProperty().addListener((obs, o, n) -> updateTotals.run());

        Label itemsLabel = new Label("ITEMS");
        itemsLabel.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill: -neutral-400;");

        HBox totalRow = new HBox(12, new Label("Total Amount:"), totalAmountLabel);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        HBox remainingRow = new HBox(12, new Label("Remaining Amount:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        Button cancelBtn  = new Button("Cancel");           cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Confirm Payment");  confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                itemsLabel, itemsBox, addMoreBtn, new Separator(),
                totalRow, labeledField("Paid Amount (₹)", paidField), remainingRow,
                err, actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, scroll);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            boolean hasItem = itemCombos.stream().anyMatch(c -> c.getValue() != null);
            if (!hasItem) { err.setText("Please select at least one item."); return; }
            double paid;
            try {
                paid = Double.parseDouble(paidField.getText().trim());
                if (paid < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid paid amount.");
                return;
            }

            List<TransactionLineItem> lineItems = new ArrayList<>();
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item != null) {
                    int qty = 1;
                    try { qty = Math.max(1, Integer.parseInt(qtyFields.get(i).getText().trim())); }
                    catch (NumberFormatException ignored) {}
                    lineItems.add(new TransactionLineItem(item.getId(), item.getName(), qty, item.getUnitPrice()));
                }
            }

            try {
                Transaction tx = AppServices.transactions().recordPayment(
                        customer.getId(), customer.getName(), lineItems, paid, null);
                // Adjust stock: reduce available quantity for each item
                for (TransactionLineItem li : lineItems) {
                    AppServices.inventory().adjustStock(li.getItemId(), -li.getQuantity());
                }
                stage.close();
                if (onConfirm != null) onConfirm.run();
            } catch (Exception ex) {
                err.setText("Error saving: " + ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // ─── Add Inventory Item dialog ─────────────────────────────────────────────

    public static void showAddItem(Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Add New Item", 440, 360);
        VBox root = dialogRoot();

        Label title = sectionTitle("Add New Inventory Item");
        TextField nameField  = styledField("Item name");
        TextField qtyField   = styledField("0");
        TextField priceField = styledField("0.00");
        Label err = errLabel();

        Button cancelBtn  = new Button("Cancel");   cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Add Item");  confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                title, new Separator(),
                labeledField("Item Name *", nameField),
                labeledField("Quantity *", qtyField),
                labeledField("Unit Price (₹) *", priceField),
                err,
                actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Item name is required."); return; }
            int qty;
            double price;
            try { qty = Integer.parseInt(qtyField.getText().trim()); if (qty < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Enter a valid quantity (≥ 0)."); return; }
            try { price = Double.parseDouble(priceField.getText().trim()); if (price < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Enter a valid price (≥ 0)."); return; }

            InventoryItem item = new InventoryItem();
            item.setName(name); item.setQuantity(qty); item.setUnitPrice(price);
            stage.close();
            if (onSaved != null) onSaved.accept(item);
        });
        stage.showAndWait();
    }

    // ─── Edit Inventory Item dialog ────────────────────────────────────────────

    public static void showEditItem(InventoryItem existing, Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Edit Item", 440, 360);
        VBox root = dialogRoot();

        Label title = sectionTitle("Edit Item");
        TextField nameField  = styledField("Item name");  nameField.setText(existing.getName());
        TextField qtyField   = styledField("0");          qtyField.setText(String.valueOf(existing.getQuantity()));
        TextField priceField = styledField("0.00");        priceField.setText(String.valueOf(existing.getUnitPrice()));
        Label err = errLabel();

        Button cancelBtn  = new Button("Cancel");    cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Save Changes"); confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                title, new Separator(),
                labeledField("Item Name *", nameField),
                labeledField("Quantity *", qtyField),
                labeledField("Unit Price (₹) *", priceField),
                err,
                actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Item name is required."); return; }
            int qty;
            double price;
            try { qty = Integer.parseInt(qtyField.getText().trim()); if (qty < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Enter a valid quantity (≥ 0)."); return; }
            try { price = Double.parseDouble(priceField.getText().trim()); if (price < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Enter a valid price (≥ 0)."); return; }

            existing.setName(name); existing.setQuantity(qty); existing.setUnitPrice(price);
            stage.close();
            if (onSaved != null) onSaved.accept(existing);
        });
        stage.showAndWait();
    }

    // ─── Stock In / Out dialog ─────────────────────────────────────────────────

    public static void showStockAdjust(List<InventoryItem> items, BiConsumer<Integer, Integer> onConfirm) {
        Stage stage = buildDialogStage("Stock In / Stock Out", 460, 340);
        VBox root = dialogRoot();

        Label title = sectionTitle("Adjust Stock");

        ComboBox<InventoryItem> itemCombo = new ComboBox<>();
        itemCombo.getItems().addAll(items);
        itemCombo.setPromptText("Select item…");
        itemCombo.setPrefWidth(320);
        itemCombo.getStyleClass().add("combo");
        itemCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (qty: " + item.getQuantity() + ")");
            }
        });
        itemCombo.setButtonCell(itemCombo.getCellFactory().call(null));

        ToggleGroup tg = new ToggleGroup();
        RadioButton inBtn  = new RadioButton("Stock In");   inBtn.setToggleGroup(tg);  inBtn.setSelected(true);
        RadioButton outBtn = new RadioButton("Stock Out");  outBtn.setToggleGroup(tg);
        HBox dirRow = new HBox(20, inBtn, outBtn);

        TextField deltaField = styledField("0");
        Label err = errLabel();

        Button cancelBtn  = new Button("Cancel");  cancelBtn.getStyleClass().addAll("btn","btn-secondary");
        Button confirmBtn = new Button("Apply");   confirmBtn.getStyleClass().add("btn");

        root.getChildren().addAll(
                title, new Separator(),
                labeledField("Item", itemCombo),
                labeledField("Direction", dirRow),
                labeledField("Quantity", deltaField),
                err,
                actionBar(cancelBtn, confirmBtn));

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);
        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            InventoryItem item = itemCombo.getValue();
            if (item == null) { err.setText("Please select an item."); return; }
            int delta;
            try { delta = Integer.parseInt(deltaField.getText().trim()); if (delta <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { err.setText("Enter a positive quantity."); return; }
            int signedDelta = outBtn.isSelected() ? -delta : delta;
            if (signedDelta < 0 && item.getQuantity() + signedDelta < 0) {
                err.setText("Not enough stock. Available: " + item.getQuantity()); return;
            }
            stage.close();
            if (onConfirm != null) onConfirm.accept(item.getId(), signedDelta);
        });
        stage.showAndWait();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static Stage buildDialogStage(String title, double width, double height) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(title);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        return stage;
    }

    private static Scene buildScene(Stage stage, javafx.scene.Parent content) {
        Scene scene = new Scene(content, stage.getMinWidth(), stage.getMinHeight());
        ThemeManager.register(scene);
        ThemeManager.apply(scene);
        stage.setOnHidden(e -> ThemeManager.unregister(scene));
        return scene;
    }

    private static void applyThemeOnShow(Alert alert) {
        alert.setOnShowing(e -> {
            Scene scene = alert.getDialogPane().getScene();
            if (scene != null) { ThemeManager.register(scene); ThemeManager.apply(scene); }
        });
        alert.setOnHidden(e -> {
            Scene scene = alert.getDialogPane().getScene();
            if (scene != null) ThemeManager.unregister(scene);
        });
    }

    private static VBox dialogRoot() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("dialog-root");
        return box;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        return l;
    }

    private static Label errLabel() {
        Label l = new Label();
        l.getStyleClass().add("text-error");
        l.setStyle("-fx-font-size:12px;");
        return l;
    }

    private static HBox actionBar(Button... buttons) {
        HBox bar = new HBox(12, buttons);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private static VBox labeledField(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("text-muted");
        lbl.setStyle("-fx-font-size:12px;");
        return new VBox(5, lbl, field);
    }

    private static TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("field");
        f.setPrefWidth(300);
        return f;
    }

    private static TextField readonlyField(String value) {
        TextField f = styledField("");
        f.setText(value);
        f.setEditable(false);
        f.setStyle("-fx-background-color: -surface-2; -fx-opacity: 0.85;");
        return f;
    }
}
