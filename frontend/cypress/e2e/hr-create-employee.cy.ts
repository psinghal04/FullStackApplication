describe('HR creates employee then employee updates contact (example flow)', () => {
  it('HR updates employee details, then employee updates contact', () => {
    const employeeSummary = {
      id: 'f5e4c0f6-3f77-4a5c-bcbf-333333333333',
      employeeId: 'EMP-900001',
      firstName: 'John',
      lastName: 'Doe',
      jobTitle: 'Engineer',
      emailAddress: 'john.doe@example.com',
      dateOfHire: '2024-01-01',
      dateOfTermination: null
    };

    const employeeDetails = {
      id: 'f5e4c0f6-3f77-4a5c-bcbf-333333333333',
      employeeId: 'EMP-900001',
      firstName: 'John',
      lastName: 'Doe',
      jobTitle: 'Engineer',
      dateOfBirth: '1990-01-01',
      gender: 'Male',
      dateOfHire: '2024-01-01',
      dateOfTermination: null,
      homeAddress: '10 Main St',
      mailingAddress: '10 Main St',
      telephoneNumber: '1234567890',
      emailAddress: 'john.doe@example.com',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    };

    cy.intercept('GET', '**/api/v1/employees/EMP-900001', {
      statusCode: 200,
      body: employeeDetails
    }).as('getEmployeeDetails');

    cy.intercept('PUT', '**/api/v1/employees/EMP-900001', {
      statusCode: 200,
      body: {
        ...employeeSummary,
        jobTitle: 'Senior Engineer'
      }
    }).as('updateEmployee');

    cy.visit('/hr/employees/EMP-900001');
    cy.url().should('include', '/hr/employees/EMP-900001');
    cy.contains('h2', 'HR Employee Detail').should('be.visible');
    cy.wait('@getEmployeeDetails');

    cy.get('input[formcontrolname="jobTitle"]').clear().type('Senior Engineer');
    cy.contains('button', 'Save Employee').click();
    cy.wait('@updateEmployee');
    cy.contains('Employee updated successfully.').should('be.visible');

    cy.intercept('GET', '**/api/v1/employees/EMP-900001', {
      statusCode: 200,
      body: {
        ...employeeDetails,
        jobTitle: 'Senior Engineer'
      }
    }).as('getMyProfile');

    cy.intercept('PATCH', '**/api/v1/employees/EMP-900001/contact', {
      statusCode: 200,
      body: {
        ...employeeSummary,
        jobTitle: 'Senior Engineer',
        emailAddress: 'john.updated@example.com'
      }
    }).as('patchContact');

    cy.contains('a', 'My Profile').click();

    cy.wait('@getMyProfile');
    cy.get('textarea[formcontrolname="homeAddress"]').clear().type('101 New Street');
    cy.get('input[formcontrolname="emailAddress"]').clear().type('john.updated@example.com');
    cy.contains('button', 'Save Contact Info').click();
    cy.wait('@patchContact');
    cy.contains('Contact information updated successfully.').should('be.visible');
  });
});
