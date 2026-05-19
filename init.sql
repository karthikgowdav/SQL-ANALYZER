CREATE TABLE students(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    marks INT,
    department VARCHAR(50),
    cgpa DECIMAL(3,2)
);

INSERT INTO students(name, marks, department, cgpa)
VALUES
('Ram', 90, 'ISE', 8.75),
('Sam', 75, 'CSE', 7.20),
('John', 85, 'ECE', 8.10),
('Alex', 60, 'ME', 6.50),
('Kiran', 95, 'ISE', 9.10);