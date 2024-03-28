create table user_profile_value
(
    id                                      uuid primary key,
    value                                   varchar(255) not null,
    user_profile_id                         uuid not null references user_profile,
    provoly_user_id                         uuid not null references provoly_user,
    unique(user_profile_id, provoly_user_id)
);

alter table metadata_value
ADD CONSTRAINT metadata_def_id FOREIGN KEY (metadata_def_id) REFERENCES metadata_def(id)