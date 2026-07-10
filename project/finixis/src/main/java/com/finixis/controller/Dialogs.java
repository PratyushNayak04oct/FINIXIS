package com.finixis.controller;

import com.finixis.viewmodel.UiUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;

import java.util.Optional;

/**
 * Central place for the MVP's placeholder confirmation dialogs. None of these do
 * real work — they only simulate the action so the customer can see the flow.
 */
public final class Dialogs {

    private Dialogs() {}

    private static StackPane root() {
        return com.finixis.App.getRoot();
    }

    public static boolean confirm(String title, String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(message);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    public static void generated(String what) {
        info(what + " generated", "This is a UI-only prototype — no real " + what.toLowerCase()
                + " file is produced. In the production app this action would create the file.");
    }

    public static void signOut(StackPane root) {
        boolean ok = confirm("Sign Out", "Sign out of Finixis?",
                "This is a UI-only prototype with no real session, so this is a no-op confirmation.");
        if (ok) UiUtil.toast(root, "Signed out (simulated)");
    }

    public static void deleteCustomer(String name) {
        confirm("Delete Customer", "Delete " + name + "?",
                "This would permanently remove the customer and their history. (Admin only — simulated.)");
    }

    public static void markSettled(String what) {
        boolean ok = confirm("Mark as Settled", "Mark this " + what + " as settled?",
                "Settled items move to the past/completed section of the history.");
        if (ok) UiUtil.toast(root(), what + " marked as settled (simulated)");
    }
}
