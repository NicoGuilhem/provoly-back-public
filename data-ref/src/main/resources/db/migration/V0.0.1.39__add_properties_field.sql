ALTER TABLE field ADD COLUMN is_locale_format boolean default false NOT NULL;
ALTER TABLE field ADD COLUMN unit varchar(30);
ALTER TABLE field ADD COLUMN format varchar(50);
ALTER TABLE field ADD COLUMN decimal_precision integer;
