-- Create tables for talent progress tracking
CREATE TABLE IF NOT EXISTS talents (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    sheet_id VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks (
    id SERIAL PRIMARY KEY,
    project_id INTEGER REFERENCES projects(id),
    task_code VARCHAR(255) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    task_category VARCHAR(255),
    pic_name VARCHAR(255),
    deadline DATE,
    estimated_completion_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, task_code)
);

CREATE TABLE IF NOT EXISTS daily_progress (
    id SERIAL PRIMARY KEY,
    talent_id INTEGER REFERENCES talents(id),
    task_id INTEGER REFERENCES tasks(id),
    date DATE NOT NULL,
    previous_effort INTEGER,
    additional_effort INTEGER,
    total_effort_spent INTEGER,
    base_estimate INTEGER,
    effort_adjustment INTEGER,
    final_estimate INTEGER,
    progress INTEGER,
    schedule_status VARCHAR(50),
    delay_reason TEXT,
    estimated_completion_date DATE,
    progress_from INTEGER,
    progress_to INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(talent_id, task_id, date)
);

-- Add unique constraint to talents table
ALTER TABLE talents ADD CONSTRAINT talents_name_unique UNIQUE (name);

-- Add unique constraint to projects table
ALTER TABLE projects ADD CONSTRAINT projects_name_unique UNIQUE (name);
ALTER TABLE projects ADD CONSTRAINT projects_sheet_id_unique UNIQUE (sheet_id);

-- Modify tasks unique constraint to include project_id
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_code_unique;
ALTER TABLE tasks ADD CONSTRAINT tasks_project_code_unique UNIQUE (project_id, task_code);

-- Add unique constraint to daily_progress table
ALTER TABLE daily_progress ADD CONSTRAINT daily_progress_unique UNIQUE (talent_id, task_id, date);

-- Adjust column lengths if needed
ALTER TABLE tasks ALTER COLUMN task_code TYPE VARCHAR(255);
ALTER TABLE tasks ALTER COLUMN task_category TYPE VARCHAR(255);
ALTER TABLE daily_progress ALTER COLUMN schedule_status TYPE VARCHAR(50); 