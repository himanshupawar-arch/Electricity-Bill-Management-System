-- Electricity Bill Management System : MySQL schema
CREATE DATABASE IF NOT EXISTS ebms;
USE ebms;

DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS readings;
DROP TABLE IF EXISTS consumers;

CREATE TABLE consumers (
    consumer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL,
    address     VARCHAR(120),
    phone       VARCHAR(15),
    type        ENUM('DOMESTIC','COMMERCIAL') NOT NULL
);

CREATE TABLE readings (
    reading_id       INT AUTO_INCREMENT PRIMARY KEY,
    consumer_id      INT NOT NULL,
    reading_date     DATE NOT NULL,
    previous_reading INT NOT NULL,
    current_reading  INT NOT NULL,
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id)
);

CREATE TABLE bills (
    bill_id     INT AUTO_INCREMENT PRIMARY KEY,
    consumer_id INT NOT NULL,
    reading_id  INT NOT NULL UNIQUE,          -- one bill per reading
    units       INT NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    bill_date   DATE NOT NULL,
    due_date    DATE NOT NULL,
    status      ENUM('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID',
    paid_date   DATE NULL,
    FOREIGN KEY (consumer_id) REFERENCES consumers(consumer_id),
    FOREIGN KEY (reading_id)  REFERENCES readings(reading_id)
);

-- Slab bill calculation (called through JDBC CallableStatement)
-- Domestic  : fixed 50  | 0-100 @3 | 101-300 @5 | >300 @7
-- Commercial: fixed 150 | 0-100 @6 | 101-300 @8 | >300 @10
DROP PROCEDURE IF EXISTS calculate_bill;
DELIMITER //
CREATE PROCEDURE calculate_bill(IN  p_reading_id INT,
                                OUT p_bill_id    INT,
                                OUT p_units      INT,
                                OUT p_amount     DECIMAL(10,2))
BEGIN
    DECLARE v_cons INT;
    DECLARE v_type VARCHAR(12);
    DECLARE v_prev INT;
    DECLARE v_curr INT;
    DECLARE r1, r2, r3, fixed_chg DECIMAL(8,2);

    SELECT r.consumer_id, c.type, r.previous_reading, r.current_reading
      INTO v_cons, v_type, v_prev, v_curr
      FROM readings r JOIN consumers c ON c.consumer_id = r.consumer_id
     WHERE r.reading_id = p_reading_id;

    SET p_units = v_curr - v_prev;

    IF v_type = 'DOMESTIC' THEN
        SET fixed_chg = 50,  r1 = 3, r2 = 5, r3 = 7;
    ELSE
        SET fixed_chg = 150, r1 = 6, r2 = 8, r3 = 10;
    END IF;

    SET p_amount = fixed_chg
                 + LEAST(p_units, 100) * r1
                 + GREATEST(LEAST(p_units, 300) - 100, 0) * r2
                 + GREATEST(p_units - 300, 0) * r3;

    INSERT INTO bills(consumer_id, reading_id, units, amount, bill_date, due_date, status)
    VALUES (v_cons, p_reading_id, p_units, p_amount, CURDATE(),
            DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'UNPAID');
    SET p_bill_id = LAST_INSERT_ID();
END //
DELIMITER ;

-- Sample data (one already-overdue bill so the Defaulter list has something to show)
INSERT INTO consumers(name,address,phone,type) VALUES
 ('Ravi Kumar','12 Model Town, Ludhiana','9876500001','DOMESTIC'),
 ('Sharma Traders','Shop 4, Chaura Bazar','9876500002','COMMERCIAL');
INSERT INTO readings(consumer_id,reading_date,previous_reading,current_reading)
 VALUES (1, DATE_SUB(CURDATE(), INTERVAL 40 DAY), 0, 180);
INSERT INTO bills(consumer_id,reading_id,units,amount,bill_date,due_date,status)
 VALUES (1, 1, 180, 50+100*3+80*5, DATE_SUB(CURDATE(), INTERVAL 40 DAY),
         DATE_SUB(CURDATE(), INTERVAL 25 DAY), 'UNPAID');
