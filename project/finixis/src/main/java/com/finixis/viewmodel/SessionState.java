package com.finixis.viewmodel;

import com.finixis.model.Role;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Holds the current simulated role and theme. Since there is no real auth/session
 * in this MVP, the role is chosen via the role switcher and only drives the UI.
 */
public class SessionState {
    private final ObjectProperty<Role> currentRole = new SimpleObjectProperty<>(Role.ADMIN);
    private final MockDataService mockData;

    public SessionState(MockDataService mockData) {
        this.mockData = mockData;
    }

    public ObjectProperty<Role> currentRoleProperty() { return currentRole; }
    public Role getCurrentRole() { return currentRole.get(); }
    public void setCurrentRole(Role role) { currentRole.set(role); }

    public MockDataService getMockData() { return mockData; }
}
