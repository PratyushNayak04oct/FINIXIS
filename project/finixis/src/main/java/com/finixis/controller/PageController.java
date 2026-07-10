package com.finixis.controller;

import com.finixis.model.Role;

/**
 * Common interface for page controllers so the shell can re-apply role permissions
 * whenever the role switcher changes or a page is (re)loaded.
 */
public interface PageController {
    void applyRole(Role role);
}
