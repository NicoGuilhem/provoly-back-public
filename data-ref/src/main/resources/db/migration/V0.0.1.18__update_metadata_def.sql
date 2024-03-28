ALTER TABLE metadata_def ADD COLUMN read_only boolean default false;
ALTER TABLE metadata_def ADD COLUMN system boolean default false;

UPDATE metadata_def
SET read_only = true, system = true
where id IN ('99f6c00b-de55-4200-8427-6694530915f7', '9d9aba2c-fc3a-44d7-a508-287ad2d8cbb0', 'ccab6dc8-e655-48bb-b991-ee89b7705f26', '99c8912d-cc1d-440c-8b68-0c28d50bda19', 'bc85f134-8a8c-44e5-b750-06f7b3b27be1', '3223ff86-f1e4-42d3-b467-961d72cb09d7');

UPDATE metadata_def
SET system = true
where id IN ('dedfb486-4387-4906-8724-adf7bc637a33', '146536da-01da-4538-990a-78a11c4696d8', 'f34754e6-be23-43c0-bb78-3d351e26c033');