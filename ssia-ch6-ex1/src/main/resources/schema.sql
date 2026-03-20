CREATE SCHEMA IF NOT EXISTS my_spring_schema;

CREATE TABLE IF NOT EXISTS my_spring_schema.users (
    username VARCHAR(50) NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS my_spring_schema.authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users
        FOREIGN KEY (username) REFERENCES my_spring_schema.users(username)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username
    ON my_spring_schema.authorities (username, authority);