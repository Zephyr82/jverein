ALTER TABLE abrechnung ADD CONSTRAINT fkAbrechnung1 FOREIGN KEY (mitglied) REFERENCES mitglied (id) ON DELETE RESTRICT;
