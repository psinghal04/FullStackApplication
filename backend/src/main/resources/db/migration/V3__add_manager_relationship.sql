-- Add manager_id column to support manager-employee relationship
-- This allows an employee to have a manager (another employee)
ALTER TABLE employees ADD COLUMN manager_id UUID NULL;

-- Add foreign key constraint
ALTER TABLE employees 
    ADD CONSTRAINT fk_employees_manager 
    FOREIGN KEY (manager_id) 
    REFERENCES employees(id) 
    ON DELETE SET NULL;

-- Create index for manager lookups (finding all employees managed by a specific manager)
CREATE INDEX idx_employees_manager_id ON employees(manager_id);

-- Add comment for documentation
COMMENT ON COLUMN employees.manager_id IS 'References the employee who manages this employee. NULL indicates no manager (e.g., top-level executives).';
