-- Online Student Attendance System - initial schema (portable across MySQL 8 and H2 in MySQL mode)

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'FACULTY', 'STUDENT'))
);

CREATE TABLE students (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    student_code  VARCHAR(30)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    department    VARCHAR(80)  NOT NULL,
    programme     VARCHAR(80)  NOT NULL,
    academic_year VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_students_user UNIQUE (user_id),
    CONSTRAINT uq_students_code UNIQUE (student_code),
    CONSTRAINT uq_students_email UNIQUE (email),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE faculty (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    faculty_code VARCHAR(30)  NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(120) NOT NULL,
    department   VARCHAR(80)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_faculty_user UNIQUE (user_id),
    CONSTRAINT uq_faculty_code UNIQUE (faculty_code),
    CONSTRAINT uq_faculty_email UNIQUE (email),
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE courses (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(120) NOT NULL,
    department VARCHAR(80)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_courses_code UNIQUE (code)
);

CREATE TABLE enrollments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT    NOT NULL,
    course_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses (id)
);
CREATE INDEX idx_enrollment_course ON enrollments (course_id);

CREATE TABLE course_assignments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id BIGINT    NOT NULL,
    course_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assignment UNIQUE (faculty_id, course_id),
    CONSTRAINT fk_assignment_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id),
    CONSTRAINT fk_assignment_course FOREIGN KEY (course_id) REFERENCES courses (id)
);
CREATE INDEX idx_assignment_course ON course_assignments (course_id);

-- One row per class meeting. status: HELD counts towards percentages; CANCELLED / HOLIDAY never do.
CREATE TABLE class_sessions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT      NOT NULL,
    session_date DATE        NOT NULL,
    slot         INT         NOT NULL DEFAULT 1,
    status       VARCHAR(20) NOT NULL,
    created_by   BIGINT      NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session UNIQUE (course_id, session_date, slot),
    CONSTRAINT ck_session_status CHECK (status IN ('HELD', 'CANCELLED', 'HOLIDAY')),
    CONSTRAINT fk_session_course FOREIGN KEY (course_id) REFERENCES courses (id)
);
CREATE INDEX idx_session_date ON class_sessions (session_date);

CREATE TABLE attendance_records (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT      NOT NULL,
    student_id BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    marked_by  BIGINT      NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance UNIQUE (session_id, student_id),
    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES class_sessions (id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students (id)
);
CREATE INDEX idx_attendance_student ON attendance_records (student_id);

CREATE TABLE audit_records (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NULL,
    username   VARCHAR(50)  NULL,
    action     VARCHAR(60)  NOT NULL,
    details    VARCHAR(500) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_created ON audit_records (created_at);
