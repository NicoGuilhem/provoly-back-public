create table dataset_version_message (
                                id uuid primary key,
                                level varchar(255) not null,
                                extract_error_code varchar(50),
                                name varchar(50),
                                type varchar(50),
                                dataset_version_id uuid references dataset_version,
                                record_id varchar(30)
);

drop table dataset_version_error;
