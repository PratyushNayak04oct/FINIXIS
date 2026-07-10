package com.finixis.controller;

import com.finixis.App;
import com.finixis.model.Role;
import com.finixis.model.User;
import com.finixis.viewmodel.UiUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class AccountController implements Initializable, PageController {

    @FXML private Label avatar, nameLabel, roleLabel;
    @FXML private TextField emailField, phoneField;
    @FXML private Button themeBtn;

    private User currentUser;
    private boolean dark = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = App.getMockData().getUsers().get(0);
        avatar.setText(initials(currentUser.getName()));
        nameLabel.setText(currentUser.getName());
        roleLabel.setText(currentUser.getRole().getDisplay());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
    }

    private String initials(String name) {
        String[] p = name.trim().split("\\s+");
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    @FXML private void onSave() {
        currentUser.setEmail(emailField.getText());
        currentUser.setPhone(phoneField.getText());
        UiUtil.toast(App.getRoot(), "Profile saved (in-memory only)");
    }

    @FXML private void onCancel() {
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
    }

    @FXML private void onToggleTheme() {
        dark = !dark;
        App.applyTheme(dark);
        themeBtn.setText(dark ? "Switch to Light" : "Switch to Dark");
    }

    @Override
    public void applyRole(Role role) {}
}
