package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.viewmodel.Permissions;
import com.finixis.viewmodel.SessionState;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ShellController implements Initializable {

    @FXML private ComboBox<Role> roleCombo;
    @FXML private Button themeBtn;
    @FXML private VBox sideMenu;
    @FXML private StackPane contentHost;
    @FXML private Button navHome, navAccounts, navCredit, navInventory,
            navTransactions, navReports, navUsers;

    private Button currentNav;
    private boolean dark = false;
    private PageController contentHolderController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        App.setShell(this);
        roleCombo.getItems().addAll(Role.values());
        roleCombo.getSelectionModel().select(App.getSession().getCurrentRole());

        SessionState s = App.getSession();
        s.currentRoleProperty().addListener((obs, o, n) -> applyRolePermissions(n));

        currentNav = navHome;
        applyRolePermissions(s.getCurrentRole());
        loadPage("home");
    }

    private void applyRolePermissions(Role r) {
        navReports.setDisable(!Permissions.canAccessReports(r));
        navReports.setOpacity(Permissions.canAccessReports(r) ? 1 : 0.4);
        navUsers.setDisable(!Permissions.canManageUsers(r));
        navUsers.setOpacity(Permissions.canManageUsers(r) ? 1 : 0.4);
        if (contentHolderController != null) contentHolderController.applyRole(r);
    }

    private void loadPage(String name) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + name + ".fxml"));
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
            contentHolderController = loader.getController();
            if (contentHolderController != null) {
                contentHolderController.applyRole(App.getSession().getCurrentRole());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load page: " + name, e);
        }
    }

    @FXML private void toggleMenu() { sideMenu.setVisible(!sideMenu.isVisible()); }

    @FXML private void onRoleChanged() {
        Role r = roleCombo.getSelectionModel().getSelectedItem();
        App.getSession().setCurrentRole(r);
        UiUtil.toast(contentHost, "Now viewing as " + r.getDisplay());
    }

    @FXML private void onToggleTheme() {
        dark = !dark;
        App.applyTheme(dark);
        themeBtn.setText(dark ? "Light" : "Dark");
    }

    @FXML private void goAccount() { setActiveNav(null); loadPage("account"); }
    @FXML private void goHome() { setActiveNav(navHome); loadPage("home"); }
    @FXML private void goAccounts() { setActiveNav(navAccounts); loadPage("accounts"); }
    @FXML private void goCredit() { setActiveNav(navCredit); loadPage("credit"); }
    @FXML private void goInventory() { setActiveNav(navInventory); loadPage("inventory"); }
    @FXML private void goTransactions() { setActiveNav(navTransactions); loadPage("transactions"); }
    @FXML private void goReports() { if (!navReports.isDisable()) { setActiveNav(navReports); loadPage("reports"); } }
    @FXML private void goUsers() { if (!navUsers.isDisable()) { setActiveNav(navUsers); loadPage("users"); } }

    @FXML private void onSignOut() { Dialogs.signOut(contentHost); }

    private void setActiveNav(Button nav) {
        if (currentNav != null) currentNav.getStyleClass().remove("menu-active");
        currentNav = nav;
        if (nav != null) nav.getStyleClass().add("menu-active");
    }

    public void navigate(String pageName) {
        Button target = switch (pageName) {
            case "home" -> navHome;
            case "accounts" -> navAccounts;
            case "credit" -> navCredit;
            case "inventory" -> navInventory;
            case "transactions" -> navTransactions;
            case "reports" -> navReports;
            case "users" -> navUsers;
            default -> null;
        };
        setActiveNav(target);
        loadPage(pageName);
    }

    public void openCustomer(int customerId) {
        loadPage("customer_detail");
        if (contentHolderController instanceof CustomerDetailController cdc) {
            cdc.loadCustomer(customerId);
            cdc.applyRole(App.getSession().getCurrentRole());
        }
    }

    public StackPane getContentHost() { return contentHost; }
}
