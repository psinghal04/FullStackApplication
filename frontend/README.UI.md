# UI Modernization Notes

## Stack
- Angular 19 standalone architecture
- Tailwind CSS with PostCSS and tokenized design system
- Angular Material for navigation, forms, inputs, and feedback states

## Implemented Foundations
- Global design tokens in `src/styles/design-system/tokens.css` with `data-theme="light|dark"`
- Shared utilities in `src/styles/design-system/utilities.css`
- Typography defaults in `src/styles/design-system/typography.css`
- Tailwind configured through `tailwind.config.js` and `postcss.config.js`

## Shell and Navigation
- `AppShellComponent` provides:
  - top toolbar with username/roles
  - collapsible side nav
  - role-aware links (HR routes only for `HR_ADMIN`)
  - skip-to-content accessibility link
  - persisted theme toggle via `ThemeService`

## Routing and Performance
- Route groups split and lazy loaded:
  - `src/app/routing/employee.routes.ts`
  - `src/app/routing/hr.routes.ts`
- Selective preloading enabled with `SelectivePreloadingStrategy`

## Modernized Views
- HR Search (`HrEmployeeSearchComponent`)
  - Material search field/button
  - responsive results table in a card layout
  - loading spinner + skeleton rows
- Employee Profile (`EmployeeProfilePageComponent`)
  - card layout, Material form controls
  - immutable email display
  - loading skeleton and save progress indicator
- Employee Form (`EmployeeFormComponent`)
  - responsive two-column Material form
  - immutable email display with hint

## Test Coverage Updated
- Added `app-shell.component.spec.ts`
- Updated existing component specs to include no-op animations for Material

## Local Development
From `frontend/`:
- Install deps: `npm install`
- Run dev server: `npm start`
- Run tests: `npm test`
- Build production bundle: `npm run build`
