INSERT INTO passengers
SELECT * FROM CSVREAD('classpath:titanic.csv', null, 'fieldSeparator=,');