ALTER TABLE oclass ADD COLUMN storage varchar(16);

UPDATE oclass SET storage = 'ELASTIC';