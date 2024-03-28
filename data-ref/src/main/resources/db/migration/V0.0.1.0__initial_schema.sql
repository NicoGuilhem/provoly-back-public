--create sequence hibernate_sequence;
--DDL

create table oclass
(
    id   uuid primary key,
    slug varchar(100) unique not null,
    name varchar(100) unique not null
);

create table field
(
    id   uuid primary key,
    slug varchar(100) unique not null,
    name varchar(100) unique not null,
    type varchar(100)        not null
);

create table category
(
    id   uuid primary key,
    name varchar(100) unique not null
);


create table attribute_def
(
    id           uuid primary key,
    oclass_id    uuid                not null references oclass,
    slug         varchar(100) unique not null,
    name         varchar(100)        not null,
    field_id     uuid                not null references field,
    category_id  uuid                not null references category,
    multi_valued boolean             not null,
    unique (oclass_id, name)
);

create table dataset
(
    id         uuid primary key,
    name       varchar(100)        not null,
    o_class_id uuid                not null references oclass,
    slug       varchar(100) unique not null,
    type       varchar(30),
    unique (o_class_id, name)
);

create table dataset_version
(
    id                    uuid primary key,
    last_modified            timestamp,
    version               integer,
    state                 varchar(30) not null,
    dataset_id uuid        not null references dataset,
    with_file             boolean     not null default false
);


create table metadata_def
(
    id          uuid primary key,
    slug        varchar(100) unique not null,
    name        varchar(255) unique,
    type        varchar(31),
    description varchar(255)
);

create table metadata_def_allowed_value
(
    id                   uuid primary key,
    metadata_def_id uuid not null references metadata_def,
    value                varchar(255)
);



create table condition
(
    id uuid primary key,
    type varchar(31) not null,
    operator varchar(31),
    attribute_id uuid references attribute_def,
    metadata_def_id uuid references metadata_def,
    value varchar(255),
    upper_value varchar(255),
    location varchar(50),
    constraint operator_not_null check (
            (type in ('ATTRIBUTE', 'METADATA') AND operator is not null) OR
            (type in ('OR', 'AND', 'TRUE') AND operator is null)
        ),
    constraint value_not_null check (
            (type = 'METADATA' AND metadata_def_id is not null) OR
            (type = 'ATTRIBUTE' AND attribute_id is not null) OR
            (type in ('OR', 'AND', 'TRUE') AND metadata_def_id is null AND attribute_id is null)
        ),
    constraint location_not_null check (
            (operator = 'DISTANCE' AND location is not null) OR
            (operator != 'DISTANCE' AND location is null)
        )
);


create table condition_condition
(
    composed_id uuid unique not null references condition,
    composed_condition_id uuid not null references condition
);

create table search_request (
                                id uuid primary key,
                                type varchar(31) not null,
                                full_search_value varchar(255)
);

create table mono_class_search_request (
                                           id uuid primary key references search_request,
                                           condition_id uuid references condition,
                                           o_class_id uuid not null references oclass,
                                           sorted_attribute_id uuid references attribute_def,
                                           direction varchar(5),
                                           constraint sort_not_null check (
                                                   (sorted_attribute_id is not null AND direction is not null) OR
                                                   (sorted_attribute_id is null AND direction is null)
                                               )
);

create table multi_class_search_request (
                                            id uuid primary key references search_request,
                                            multi_type varchar(31)
);

create table field_condition (
                                 id uuid primary key,
                                 value varchar(255),
                                 operator varchar(31),
                                 field_id uuid references field,
                                 search_request_id uuid references multi_class_search_request
);

create table multi_class_search_request_o_classes (
                                                      multi_class_search_request_id uuid not null references multi_class_search_request,
                                                      o_classes uuid
);

create table predicate
(
    id uuid primary key,
    name varchar(50) not null,
    value varchar not null
);

create table abac_rule
(
    id uuid primary key,
    name varchar(50) not null,
    description varchar(90),
    active boolean,
    type varchar(31) not null,
    predicate_id uuid not null references predicate,
    condition_id uuid not null references condition,
    o_class_id uuid references oclass,
    constraint o_class_id_not_null check (
            (type = 'ATTRIBUTE' AND o_class_id is not null) OR
            (type in ('METADATA') AND o_class_id is null)
        )
);

create table context_variable
(
    name varchar primary key unique,
    type varchar(31) not null,
    value_char varchar,
    value_integer int,
    value_double float,
    value_date timestamp
);


create table named_query
(
    id              UUID primary key,
    name            varchar(50) not null,
    description     varchar,
    request_id      uuid        not null references search_request,
    visibility_type varchar(30) not null
);

create table user_profile
(
    id          uuid primary key,
    slug        varchar(100) unique not null,
    name        varchar(255) unique,
    type        varchar(31),
    description varchar(255)
);


create table user_profile_allowed_value
(
    id                   uuid primary key,
    user_profile_id uuid not null references user_profile,
    value                varchar(255)
);

create table provoly_user
(
    id    uuid primary key,
    claim varchar(50) unique not null
);

create table dashboard
(
    id                uuid primary key,
    name              varchar(50) not null,
    user_id           uuid        not null references provoly_user,
    creation_Date     timestamptz,
    modification_Date timestamptz,
    manifest          json,
    description       varchar(200),
    image             varchar(255),
    visibility_type   varchar(30) not null,
    cover             boolean not null default false
);

create table provoly_user_dashboard
(
    user_id      uuid    not null references provoly_user,
    dashboard_id uuid    not null references dashboard,
    owner   boolean not null,
    is_default boolean,
    primary key (user_id, dashboard_id)
);

create table metadata_value
(
    id                                      uuid primary key,
    value                                   varchar(255) not null,
    entity_id                               uuid not null,
    entity_type                             varchar(31) not null,
    metadata_def_id                         uuid not null,
    unique (metadata_def_id,entity_id)
);

create table provoly_user_named_query
(
    user_id             uuid    not null references provoly_user,
    named_query_id      uuid    not null references named_query,
    favorite            boolean not null,
    color               varchar(35),
    last_execution_date timestamp,
    owner   boolean not null,
    primary key (user_id, named_query_id)
);

create table meta_provisioning
(
    id               uuid primary key,
    name             varchar(255) unique,
    metadata_def_id uuid not null references metadata_def,
    user_profile_id uuid not null references user_profile
);

create table relation_type_stats
(
    id    uuid primary key,
    nb_relation int not null
);

create table relation_type
(
    id    uuid primary key,
    slug varchar(100) unique not null,
    name  varchar(30) unique not null,
    modification_date timestamp not null,
    relation_type_stats_id uuid not null references relation_type_stats
);

create table link
(
    id          uuid primary key,
    relation_type_id     uuid not null references relation_type,
    attribute_source_id     uuid not null references attribute_def,
    attribute_destination_id uuid not null references attribute_def,
    unique (attribute_destination_id, attribute_source_id)
);

create table custom_class
(
    o_class uuid not null references oclass,
    domain varchar(30) not null,
    content varchar(1048576) not null,
    primary key (o_class,domain)
);

create table widget_catalog
(
    id              uuid primary key,
    name            varchar(50) unique not null,
    description     varchar(255),
    image           varchar(255),
    content         varchar(1048576)   not null,
    visibility_type varchar(30)        not null,
    cover           boolean            not null default false,
    creation_date   timestamptz not null default current_timestamp,
    modification_date timestamptz not null default current_timestamp
);

create table provoly_user_widget_catalog
(
    user_id           uuid    not null references provoly_user,
    widget_catalog_id uuid    not null references widget_catalog,
    owner          boolean not null,
    primary key (user_id, widget_catalog_id)
);

create table widget_catalog_datasource
(
    widget_catalog_id uuid not null references widget_catalog,
    datasource uuid not null
);


create table notification
(
    id    UUID primary key,
    message_code  varchar(20) not null,
    link  varchar,
    creation_date timestamptz not null
);

create table provoly_user_notification
(
    user_id         uuid not null references provoly_user,
    notification_id uuid not null references notification,
    primary key (user_id, notification_id)
);

create table notification_parameter
(
    id                   uuid primary key,
    notification_id uuid not null references notification,
    key                  varchar(255),
    value                varchar(255)
);

create table dataset_version_error
(
    id uuid primary key,
    extract_error_code varchar(50),
    name varchar(50) not null,
    type varchar(50),
    dataset_version_id uuid references dataset_version,
    record_id varchar(30)
);

-- DML

insert into category (id, name)
values ('cf666d66-838f-4d92-a4d2-a315df21fac9', 'default');

insert into metadata_def (id, slug, name, type, description)
values ('9d9aba2c-fc3a-44d7-a508-287ad2d8cbb0', 'd61cebf7ca__class' , '_class', 'UUID',
        'init MD _class');
insert into metadata_def (id,slug, name, type, description)
values ('ccab6dc8-e655-48bb-b991-ee89b7705f26', '3452a5d3a2__http_origin', '_http_origin',
        'STRING', 'init MD _http_origin');
insert into metadata_def (id,slug, name, type, description)
values ('99f6c00b-de55-4200-8427-6694530915f7', '99f6c00b_dataset_version_id', '_dataset_version_id', 'UUID',
        'init MD _dataset_version_id');
insert into metadata_def (id, slug, name, type, description)
values ('99c8912d-cc1d-440c-8b68-0c28d50bda19', '99c8912d__dataset_id' , 'dataset_id', 'UUID',
        'init MD dataset_id');
insert into metadata_def (id, slug, name, type, description)
values ('bc85f134-8a8c-44e5-b750-06f7b3b27be1', 'bc85f134__insertion_date' , '_insertion_date', 'DATE',
        'init MD _insertion_date');
insert into metadata_def (id, slug, name, type, description)
values ('3223ff86-f1e4-42d3-b467-961d72cb09d7', '3223ff86__item_id' , '_item_id', 'STRING',
        'init MD _item_id');

insert into metadata_def (id, slug, name, type, description)
values ('dedfb486-4387-4906-8724-adf7bc637a33', 'dedfb486__geoNamespace' , '_geoNamespace', 'STRING',
        'init MD _geoNamespace');

insert into metadata_def (id, slug, name, type, description)
values ('146536da-01da-4538-990a-78a11c4696d8', '146536da__geoKey' , '_geoKey', 'STRING',
        'init MD _geoKey');



INSERT INTO provoly_user
VALUES ('1fb6c658-c02e-4531-996e-c37b9eefc6b3', '1fb6c658-c02e-4531-996e-c37b9eefc6b3');