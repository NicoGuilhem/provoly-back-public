create table transfo
(
    id              uuid primary key,
    title           varchar     not null,
    description     varchar,
    creation_date   timestamptz not null,
    job_instance_id uuid,
    active          boolean     not null,
    unique (id, job_instance_id)
);

create table node
(
    id      uuid    not null primary key,
    type    varchar not null,
    title   varchar,
    spec    jsonb
);

create table transfo_links
(
    start_node uuid not null,
    start_slot int  not null,
    end_node   uuid not null,
    end_slot   int  not null,
    transfo_id uuid not null references transfo
);

create table transfo_node
(
    transfo_id uuid not null references transfo,
    nodes_id   uuid not null references node
);

create table node_size
(
    node_id uuid not null references node,
    size     integer
);

create table node_pos
(
    node_id uuid not null references node,
    pos integer
);


