DROP TABLE IF EXISTS passengers;
CREATE TABLE passengers
(
    PassengerId INT PRIMARY KEY,
    Survived    INT,
    Pclass      INT,
    Name        VARCHAR(255),
    Sex         VARCHAR(10),
    Age         DECIMAL(5, 2),
    SibSp       INT,
    Parch       INT,
    Ticket      VARCHAR(50),
    Fare        DECIMAL(10, 4),
    Cabin       VARCHAR(50),
    Embarked    VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS stored_queries
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
