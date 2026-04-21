CREATE TABLE IF NOT EXISTS token (
    id INT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(45) NOT NULL,
    token TEXT NULL,
    expires_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_identifier (identifier)
);
