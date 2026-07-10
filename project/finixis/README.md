# Finixis — UI-Only MVP Prototype (Customer Demo)

A clickable, visually complete JavaFX prototype for customer sign-off. **No real
database, no login/auth, no business rules** — every screen is populated with
realistic mock data so the customer can react to layout, flow, and both themes
before real functionality is built.

## What's mocked vs. real

| Aspect | Status |
|---|---|
| Database / persistence | **Mocked** — in-memory `MockDataService` creates static sample data at startup. Not a future DB layer. |
| Login / authentication | **Removed for this MVP** — app launches straight into Home. |
| Role-based access | **Simulated** — a role switcher in the top bar toggles Admin/Manager/Employee views (hides/disables buttons per the permission matrix). No real security. |
| Save / Delete / Add Credit / etc. | **Simulated** — buttons are present and clickable; actions either update the in-memory mock data (so the UI visibly reacts) or show a placeholder confirmation dialog/toast. |
| Export / Generate Report / Create Invoice | **Simulated** — dialogs open and show a "generated/created" confirmation. No real PDF/Excel files. |
| Light / Dark theme | **Fully working** — toggle in the top bar or on the Account/Profile page. Every screen supports both. |
| Reports chart | **Real JavaFX chart** populated with mock numbers. |

## Tech stack (identical to production target — code is reusable later)

- Java 21, JavaFX 21, FXML for layouts
- Maven build (`mvn clean javafx:run`)
- Simple MVVM structure (View/Controller + lightweight ViewModel with mock data service)
- Ikonli (FontAwesome5 pack) for icons
- JavaFX Charts for the Reports page

## How to run

```bash
mvn clean javafx:run
```

The app launches directly into the Home / Welcome page — there is no login step.

## Pages (all navigable, all populated with mock data)

1. **Home / Welcome** — launch screen, dashboard summary (pending credits/debits,
   low-stock, customer count), quick actions, recent activity.
2. **Accounts / Customers** — search bar, ~12 mock customers, Open button.
3. **Dedicated Customer Page (redesigned)** — customer summary card (avatar,
   contact, customer-since, large color-coded balance), stat chips (total
   transactions, last activity, ongoing items), grouped action buttons
   (Add Credit / Add Debit / Record Payment), visually separated Delete
   (Admin-only), and date-grouped transaction history with date-range filter
   and ongoing/pending badges.
4. **Credit** — pending credits list, search/filter/sort, Add New Credit,
   Mark as Settled.
5. **Inventory** — ~12 items with low-stock badges, Add/Edit/Delete icons,
   Stock In/Out.
6. **Transactions** — chronological list grouped by date (Today / This Week /
   This Month / Earlier), filters, row tap navigates to that customer's
   detail page, New Transaction / Generate Report / Create Invoice / Export.
7. **Account / Profile** — read-only user info, theme toggle, editable
   phone/email (in-memory only).
8. **Reports / Summary** — date range selector, JavaFX bar chart with mock
   totals, export (mock confirmation).
9. **Admin: User Management** — mock user list with role tags and status
   badges, Add/Edit User dialog.

## Role switcher

A dropdown in the top nav bar ("Viewing as: Admin / Manager / Employee") lets
the customer preview how each role's view differs. Buttons and menu items are
hidden/disabled per the permission matrix below — this is a **UI simulation**,
not real access control.

| Action | Admin | Manager | Employee |
|---|---|---|---|
| Add/Edit/Delete Customer | Y/Y/Y | Y/Y/N | N/N/N |
| Add/Edit/Delete Credit | Y/Y/Y | Y/Y/N | Y/N/N |
| Mark Credit Settled | Y | Y | N |
| Record Payment/Debit | Y | Y | Y |
| Edit/Delete Payment | Y/Y | Y/N | N/N |
| Add/Edit/Delete Inventory Item | Y/Y/Y | Y/Y/N | N/N/N |
| Stock In/Out | Y | Y | Y |
| Add/Edit/Delete Transaction | Y/Y/Y | Y/Y/N | Y/N/N |
| Export/Generate Report/Create Invoice | Y | Y | N |
| Reports/Summary Page Access | Y | Y | N |
| User Management (all actions) | Y | N | N |
| Toggle Light/Dark Mode | Y | Y | Y |

## Themes

Both light and dark themes use CSS variables (semantic color tokens) defined in
`src/main/resources/css/light-theme.css` and `dark-theme.css`. The toggle is
available in the top bar and on the Account/Profile page. Every screen and
dialog is designed to look correct in both modes.

## Project structure

```
src/main/java/com/finixis/
  App.java                      — entry point, theme + navigation
  model/                        — POJOs (User, Customer, Credit, Transaction, InventoryItem, Invoice, Role)
  viewmodel/
    MockDataService.java        — realistic in-memory sample data
    SessionState.java           — current simulated role
    Permissions.java            — permission matrix (UI simulation)
    UiUtil.java                 — formatting + toast helpers
  controller/
    ShellController.java         — top bar, side menu, role switcher, page host
    PageController.java          — common interface for page controllers
    Dialogs.java                 — placeholder confirmation dialogs
    HomeController, AccountsController, CustomerDetailController,
    CreditController, InventoryController, TransactionsController,
    AccountController, ReportsController, UsersController
src/main/resources/
  fxml/                         — one .fxml per screen
  css/                          — light-theme.css, dark-theme.css
```

## Next steps (not part of this MVP)

Once the customer approves this UI, later steps can layer in real persistence
(Supabase/PostgreSQL), real authentication, real export, and real permission
enforcement — the screens themselves shouldn't need to change.
