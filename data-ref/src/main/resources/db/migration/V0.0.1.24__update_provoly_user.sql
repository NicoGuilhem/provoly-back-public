ALTER TABLE provoly_user
    ADD COLUMN name varchar(100),
    ADD COLUMN last_name varchar(100),
    ADD COLUMN email varchar(100);
ALTER TABLE provoly_user RENAME COLUMN claim TO subject;
