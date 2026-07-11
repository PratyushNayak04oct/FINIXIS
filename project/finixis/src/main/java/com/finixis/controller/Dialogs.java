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

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central place for all dialogs. Each dialog registers its Scene with ThemeManager so
 * the live theme toggle applies to open dialogs correctly.
 */
public final class Dialogs {
    private Dialogs() {}

    // ─── Generic / system dialogs ─────────────────────────────────────────────

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

    public static void showFileDownloadedDialog(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Downloaded");
        alert.setHeaderText("File saved successfully");
        alert.setContentText("Your file has been saved to:\n\n"
                + (file != null ? file.getAbsolutePath() : "Unknown location")
                + "\n\nClick Close to dismiss.");
        alert.getButtonTypes().setAll(ButtonType.CLOSE);
        applyThemeOnShow(alert);
        alert.showAndWait();
    }

    public static void markSettled(String what) {
        boolean ok = confirm("Mark as Settled", "Mark this " + what + " as settled?",
                "Settled items will no longer appear as pending.");
        if (ok) UiUtil.toast(App.getRoot(), what + " marked as settled");
    }

    // ─── Add Customer dialog ──────────────────────────────────────────────────

    public static void showAddCustomer(Consumer<Customer> onSaved) {
        Stage stage = buildDialogStage("Add Customer", 460, 400);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Add Customer");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Enter the new customer's details below.");
        sub.setStyle("-fx-text-fill: -text-muted;");

        TextField nameField  = styledField("Full name");
        TextField phoneField = styledField("Phone number");
        TextField emailField = styledField("Email address");
        TextField addrField  = styledField("Address");

        VBox form = new VBox(12,
                labeledField("Name *", nameField),
                labeledField("Phone", phoneField),
                labeledField("Email", emailField),
                labeledField("Address", addrField));

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Add Customer");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err, btns);

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            Customer saved = AppServices.customers().addCustomer(
                    name,
                    phoneField.getText().trim(),
                    emailField.getText().trim(),
                    addrField.getText().trim());
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        stage.showAndWait();
    }

    // ─── Add Debit dialog ────────────────────────────────────────────────────

    public static void showAddDebit(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Add Debit — " + customer.getName(), 460, 340);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Add Debit");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Record an amount owed to " + customer.getName());
        sub.setStyle("-fx-text-fill: -text-muted;");
        VBox header = new VBox(4, title, sub);

        VBox nameRow   = labeledField("Customer Name", readonlyField(customer.getName()));
        TextField amountField = styledField("0.00");
        VBox amountRow = labeledField("Amount (₹)", amountField);

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Confirm Debit");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, new Separator(), nameRow, amountRow, err, btns);

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
            AppServices.transactions().addDebit(
                    customer.getId(), customer.getName(), amount, "Debit added manually");
            stage.close();
            if (onConfirm != null) onConfirm.run();
        });

        stage.showAndWait();
    }

    // ─── Record Payment dialog ────────────────────────────────────────────────

    public static void showRecordPayment(Customer customer, Runnable onConfirm) {
        Stage stage = buildDialogStage("Record Payment — " + customer.getName(), 580, 540);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");
        scroll.setContent(root);

        Label title = new Label("Record Payment");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Customer: " + customer.getName() + "  ·  Select items and enter amount paid.");
        sub.setStyle("-fx-text-fill: -text-muted;");

        VBox itemsBox = new VBox(10);
        List<InventoryItem> inventory = AppServices.inventory().getAll();

        Label totalAmountLabel = new Label("₹0.00");
        totalAmountLabel.setStyle("-fx-font-weight:700; -fx-font-size:16px; -fx-text-fill: -primary-600;");

        TextField paidField = styledField("0.00");

        Label remainingLabel = new Label("₹0.00");
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px;");

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        List<ComboBox<InventoryItem>> itemCombos = new ArrayList<>();
        List<TextField> qtyFields = new ArrayList<>();

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
            remainingLabel.setStyle(remaining > 0
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };

        Runnable[] addRowRef = new Runnable[1];
        addRowRef[0] = () -> {
            ComboBox<InventoryItem> combo = new ComboBox<>();
            combo.getItems().addAll(inventory);
            combo.setPromptText("Select item…");
            combo.setPrefWidth(220);
            combo.getStyleClass().add("combo");
            combo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(InventoryItem item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null
                            : item.getName() + "  (" + UiUtil.money(item.getUnitPrice()) + "/unit)");
                }
            });
            combo.setButtonCell(combo.getCellFactory().call(null));

            TextField qtyField = styledField("0");
            qtyField.setText("0");
            qtyField.setPrefWidth(70);

            // Availability label updates live
            Label availLabel = new Label();
            availLabel.setStyle("-fx-font-size:11px;");

            Runnable updateAvail = () -> {
                InventoryItem sel = combo.getValue();
                if (sel == null) { availLabel.setText(""); return; }
                InventoryItem fresh = AppServices.inventory().getById(sel.getId()).orElse(sel);
                int avail = fresh.getQuantity();
                int reqQty = 0;
                try { reqQty = Integer.parseInt(qtyField.getText().trim()); } catch (NumberFormatException ignored) {}
                if (reqQty > avail) {
                    availLabel.setText("Out of Stock (" + avail + " left)");
                    availLabel.setStyle("-fx-font-size:11px; -fx-text-fill: -error-600;");
                } else {
                    availLabel.setText("Available: " + avail + " units");
                    availLabel.setStyle("-fx-font-size:11px; -fx-text-fill: -success-600;");
                }
            };

            combo.valueProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotals.run(); });
            qtyField.textProperty().addListener((obs, o, n) -> { updateAvail.run(); updateTotals.run(); });

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().addAll("btn", "btn-secondary");
            removeBtn.setStyle("-fx-padding: 4 8;");

            HBox row = new HBox(8, combo, new Label("Qty:"), qtyField, availLabel, removeBtn);
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

        VBox paidRow = labeledField("Paid Amount (₹)", paidField);

        HBox remainingRow = new HBox(12, new Label("Remaining Amount:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Confirm Payment");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
                new VBox(4, title, sub), new Separator(),
                itemsLabel, itemsBox, addMoreBtn,
                new Separator(),
                totalRow, paidRow, remainingRow,
                err, btns);

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
            StringBuilder desc = new StringBuilder();
            for (int i = 0; i < itemCombos.size(); i++) {
                InventoryItem item = itemCombos.get(i).getValue();
                if (item != null) {
                    int qty = 1;
                    try { qty = Math.max(1, Integer.parseInt(qtyFields.get(i).getText().trim())); }
                    catch (NumberFormatException ignored) {}
                    lineItems.add(new TransactionLineItem(
                            item.getId(), item.getName(), qty, item.getUnitPrice()));
                    if (!desc.isEmpty()) desc.append(", ");
                    desc.append(qty).append("x ").append(item.getName());
                }
            }

            AppServices.transactions().recordPayment(
                    customer.getId(), customer.getName(), lineItems, paid, "Payment: " + desc);
            stage.close();
            if (onConfirm != null) onConfirm.run();
        });

        stage.showAndWait();
    }

    // ─── Add Inventory Item dialog ────────────────────────────────────────────

    public static void showAddItem(Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Add Inventory Item", 440, 360);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Add Item");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Enter the details for the new inventory item.");
        sub.setStyle("-fx-text-fill: -text-muted;");

        TextField nameField  = styledField("Item name");
        TextField qtyField   = styledField("0");
        TextField priceField = styledField("0.00");

        VBox form = new VBox(12,
                labeledField("Name *", nameField),
                labeledField("Quantity", qtyField),
                labeledField("Unit Price (₹)", priceField));

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Add Item");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err, btns);

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                if (qty < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Quantity must be a non-negative integer.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
                if (price < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Unit price must be a non-negative number.");
                return;
            }
            InventoryItem saved = AppServices.inventory().addItem(name, qty, price);
            stage.close();
            if (onSaved != null) onSaved.accept(saved);
        });

        stage.showAndWait();
    }

    // ─── Edit Inventory Item dialog ───────────────────────────────────────────

    public static void showEditItem(InventoryItem item, Consumer<InventoryItem> onSaved) {
        Stage stage = buildDialogStage("Edit Item — " + item.getName(), 440, 360);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Edit Item");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Update the details for \"" + item.getName() + "\".");
        sub.setStyle("-fx-text-fill: -text-muted;");

        TextField nameField  = styledField("Item name");
        nameField.setText(item.getName());
        TextField qtyField   = styledField("0");
        qtyField.setText(String.valueOf(item.getQuantity()));
        TextField priceField = styledField("0.00");
        priceField.setText(String.valueOf(item.getUnitPrice()));

        VBox form = new VBox(12,
                labeledField("Name *", nameField),
                labeledField("Quantity", qtyField),
                labeledField("Unit Price (₹)", priceField));

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Save Changes");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err, btns);

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { err.setText("Name is required."); return; }
            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                if (qty < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Quantity must be a non-negative integer.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
                if (price < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Unit price must be a non-negative number.");
                return;
            }
            item.setName(name);
            item.setQuantity(qty);
            item.setUnitPrice(price);
            InventoryItem updated = AppServices.inventory().updateItem(item);
            stage.close();
            if (onSaved != null) onSaved.accept(updated);
        });

        stage.showAndWait();
    }

    // ─── Stock Adjustment dialog ──────────────────────────────────────────────

    public static void showStockAdjust(List<InventoryItem> items, BiConsumer<Integer, Integer> onAdjust) {
        Stage stage = buildDialogStage("Adjust Stock", 440, 320);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Stock Adjustment");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label sub = new Label("Select an item and enter the stock change amount.");
        sub.setStyle("-fx-text-fill: -text-muted;");

        ComboBox<InventoryItem> itemCombo = new ComboBox<>();
        itemCombo.getItems().addAll(items);
        itemCombo.setPromptText("Select item…");
        itemCombo.setPrefWidth(300);
        itemCombo.getStyleClass().add("combo");
        itemCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InventoryItem it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.getName() + " (Qty: " + it.getQuantity() + ")");
            }
        });
        itemCombo.setButtonCell(itemCombo.getCellFactory().call(null));

        TextField deltaField = styledField("e.g. +10 or -5");

        VBox form = new VBox(12,
                labeledField("Item", itemCombo),
                labeledField("Quantity Change (+/-)", deltaField));

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button confirmBtn = new Button("Adjust Stock");
        confirmBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, confirmBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(new VBox(4, title, sub), new Separator(), form, err, btns);

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        confirmBtn.setOnAction(e -> {
            InventoryItem selected = itemCombo.getValue();
            if (selected == null) { err.setText("Please select an item."); return; }
            int delta;
            try {
                String text = deltaField.getText().trim().replace("+", "");
                delta = Integer.parseInt(text);
                if (delta == 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Enter a non-zero integer (e.g. +10 or -5).");
                return;
            }
            stage.close();
            if (onAdjust != null) onAdjust.accept(selected.getId(), delta);
        });

        stage.showAndWait();
    }

    // ─── Add Credit Info dialog ───────────────────────────────────────────────

    public static void showAddCreditInfo() {
        Stage stage = buildDialogStage("Add New Credit", 480, 260);
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Add New Credit");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");
        Label msg = new Label(
                "Credits are created automatically when you record a payment on a customer's page.\n\n"
                + "Navigate to Accounts → open a customer → Record Payment to add a credit entry.");
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: -text-muted;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button goBtn = new Button("Go to Customers Page");
        goBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, goBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(new VBox(4, title, msg), new Separator(), btns);

        Scene scene = buildScene(stage, root);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        goBtn.setOnAction(e -> {
            stage.close();
            App.getShell().navigate("accounts");
        });

        stage.showAndWait();
    }

    // ─── Edit Transaction dialog ──────────────────────────────────────────────

    public static void showEditTransaction(Transaction t, Runnable onSave) {
        Stage stage = buildDialogStage("Edit Transaction", 520, 480);
        stage.setResizable(false);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(600);
        scroll.getStyleClass().add("scroll-pane");

        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dialog-root");
        scroll.setContent(root);

        Label title = new Label("Edit Transaction");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");

        // Read-only info
        VBox infoForm = new VBox(12,
                labeledField("Description / Items", readonlyField(
                        t.getDescription() != null ? t.getDescription() : t.getType().name())),
                labeledField("Original Total Amount (₹)", readonlyField(UiUtil.money(t.getAmount()))),
                labeledField("Transaction Date", readonlyField(UiUtil.date(t.getDate()))),
                labeledField("Current Balance (₹)", readonlyField(UiUtil.money(t.getBalance()))));

        // Editable pay later amount
        TextField payLaterField = styledField("0");
        payLaterField.setText("0");

        // Payment date picker
        DatePicker paymentDatePicker = new DatePicker(LocalDate.now());
        paymentDatePicker.getStyleClass().add("date-picker");
        paymentDatePicker.setPrefWidth(300);

        // Live remaining balance label
        Label remainingLabel = new Label(UiUtil.money(t.getBalance()));
        remainingLabel.setStyle("-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");

        Runnable updateRemaining = () -> {
            double payAmt = 0;
            try { payAmt = Double.parseDouble(payLaterField.getText().trim()); }
            catch (NumberFormatException ignored) {}
            double newBal = t.getBalance() - payAmt;
            remainingLabel.setText(UiUtil.money(Math.max(0, newBal)));
            remainingLabel.setStyle(newBal > 0
                    ? "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -error-600;"
                    : "-fx-font-weight:700; -fx-font-size:14px; -fx-text-fill: -success-600;");
        };

        payLaterField.textProperty().addListener((obs, o, n) -> updateRemaining.run());

        VBox editForm = new VBox(12,
                labeledField("Pay Later Amount (₹)", payLaterField),
                labeledField("Payment Date", paymentDatePicker));

        HBox remainingRow = new HBox(12, new Label("Remaining Balance:"), remainingLabel);
        remainingRow.setAlignment(Pos.CENTER_LEFT);
        remainingRow.setStyle("-fx-background-color: -surface-2; -fx-padding: 10 14; -fx-background-radius: 8;");

        Label err = new Label();
        err.setStyle("-fx-text-fill: -error-600; -fx-font-size:12px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn");

        HBox btns = new HBox(12, cancelBtn, saveBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, new Separator(), infoForm, new Separator(),
                editForm, remainingRow, err, btns);

        Scene scene = buildScene(stage, scroll);
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());
        saveBtn.setOnAction(e -> {
            double payAmt;
            try {
                payAmt = Double.parseDouble(payLaterField.getText().trim());
                if (payAmt < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err.setText("Please enter a valid non-negative amount.");
                return;
            }
            if (payAmt == 0) { stage.close(); return; }
            LocalDate payDate = paymentDatePicker.getValue() != null
                    ? paymentDatePicker.getValue() : LocalDate.now();
            AppServices.transactions().recordPartialPayment(t.getId(), payAmt, payDate);
            stage.close();
            if (onSave != null) onSave.run();
        });

        stage.showAndWait();
    }

    // ─── View Transaction dialog ──────────────────────────────────────────────

    public static void showViewTransaction(Transaction t) {
        Stage stage = buildDialogStage("Transaction Details", 520, 400);
        stage.setResizable(false);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(600);
        scroll.getStyleClass().add("scroll-pane");

        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dialog-root");
        scroll.setContent(root);

        Label title = new Label("Transaction Details");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:700;");

        VBox form = new VBox(12,
                labeledField("Customer", readonlyField(t.getCustomerName() != null ? t.getCustomerName() : "—")),
                labeledField("Type", readonlyField(t.getType().name())),
                labeledField("Total Amount (₹)", readonlyField(UiUtil.money(t.getAmount()))),
                labeledField("Paid Amount (₹)", readonlyField(UiUtil.money(t.getPaidAmount()))),
                labeledField("Remaining Balance (₹)", readonlyField(UiUtil.money(t.getBalance()))),
                labeledField("Date", readonlyField(UiUtil.date(t.getDate()))),
                labeledField("Status", readonlyField(t.isOngoing() ? "Pending" : "All Cleared")),
                labeledField("Description / Items", readonlyField(
                        t.getDescription() != null ? t.getDescription() : "—")));

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");

        HBox btns = new HBox(closeBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, new Separator(), form, btns);

        Scene scene = buildScene(stage, scroll);
        stage.setScene(scene);

        closeBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Stage buildDialogStage(String title, double width, double height) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(App.getScene().getWindow());
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.setResizable(false);
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
            if (scene != null) {
                ThemeManager.register(scene);
                ThemeManager.apply(scene);
            }
        });
        alert.setOnHidden(e -> {
            Scene scene = alert.getDialogPane().getScene();
            if (scene != null) ThemeManager.unregister(scene);
        });
    }

    private static VBox labeledField(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill: -text-muted;");
        VBox box = new VBox(5, lbl, field);
        return box;
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
