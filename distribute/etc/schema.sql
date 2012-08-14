/*
 * Schema for Serengeti.
 */
DROP DATABASE serengeti;
DROP ROLE serengeti;

CREATE ROLE serengeti WITH LOGIN PASSWORD 'password';

CREATE DATABASE serengeti WITH OWNER = serengeti ENCODING = 'UTF8';

\c serengeti;
\c - serengeti;

create sequence cloud_provider_config_seq;
create table cloud_provider_config (
   id           bigint       not null unique DEFAULT nextval('cloud_provider_config_seq'::regclass),
   attribute    varchar(255) not null,
   cloud_type   varchar(255) not null,
   value        varchar(255),
   primary key (id)
);

create sequence vc_datastore_seq;
create table vc_datastore (
   id           bigint       not null unique DEFAULT nextval('vc_datastore_seq'::regclass),
   name         varchar(255) not null,
   type         varchar(255) not null,
   vc_datastore varchar(255) not null,
   primary key (id)
);

create sequence vc_resource_pool_seq;
create table vc_resource_pool (
   id           bigint       not null unique DEFAULT nextval('vc_resource_pool_seq'::regclass),
   name         varchar(255) not null unique,
   vc_cluster   varchar(255) not null,
   vc_rp        varchar(255) not null,
   primary key (id)
);

create sequence network_seq;
create table network (
   id           bigint       not null unique DEFAULT nextval('network_seq'::regclass),
   name         varchar(255) not null unique,
   port_group   varchar(255) not null,
   alloc_type   varchar(255) not null,
   netmask      varchar(255),
   gateway      varchar(255),
   dns1         varchar(255),
   dns2         varchar(255),
   total        bigint,
   free         bigint,
   primary key (id)
);

create sequence ip_block_seq;
create table ip_block (
   id           bigint       not null unique DEFAULT nextval('ip_block_seq'::regclass),
   type         varchar(255) not null,
   network_id   bigint       not null,
   owner_id     bigint       not null,
   begin_ip     bigint       not null,
   end_ip       bigint       not null,
   primary key (id),
   foreign key(network_id) references network(id) ON DELETE CASCADE
);

create sequence cluster_seq;
create table cluster (
   id           bigint       not null unique DEFAULT nextval('cluster_seq'::regclass),
   name         varchar(255) not null unique,
   distro       varchar(255),
   status       varchar(255) not null,
   vc_datastore_names text,
   vc_rp_names  text,
   network_id   bigint,
   start_after_deploy boolean,
   configuration text,
   primary key (id),
   foreign key(network_id) references network(id) ON DELETE CASCADE
);

create sequence node_group_seq;
create table node_group (
   id                     bigint       not null unique DEFAULT nextval('node_group_seq'::regclass),
   name                   varchar(255) not null,
   roles                  varchar(255),
   node_type              integer,
   cpu                    integer,
   memory                 integer,
   defined_instance_num   integer not null,
   ha_flag                varchar(10),
   storage_type           varchar(255),
   storage_size           integer,
   vc_datastore_names     text,
   vc_rp_names            text,
   configuration          text,
   instance_per_host      integer,
   cluster_id             bigint,
   primary key (id),
   foreign key(cluster_id) references cluster(id) ON DELETE CASCADE
);

create sequence node_group_association_seq;
create table node_group_association (
   id                 bigint       not null unique DEFAULT nextval('node_group_association_seq'::regclass),
   referenced_group   varchar(255) not null,
   association_type   varchar(255),
   node_group_id      bigint,
   primary key (id),
   foreign key(node_group_id) references node_group(id) ON DELETE CASCADE
);

create sequence hadoop_node_seq;
create table hadoop_node (
   id           bigint       not null unique DEFAULT nextval('hadoop_node_seq'::regclass),
   vm_name      varchar(255) not null unique,
   host_name    varchar(255),
   status       varchar(255),
   action       varchar(255),
   vc_datastores text,
   ip_address   varchar(255),
   node_group_id bigint,
   vc_rp_id     bigint,
   primary key (id),
   foreign key(node_group_id) references node_group(id) ON DELETE CASCADE,
   foreign key(vc_rp_id) references vc_resource_pool(id) ON DELETE CASCADE
);

create sequence task_seq;
create table task (
   id           bigint       not null unique DEFAULT nextval('task_seq'::regclass),
   status       varchar(255) not null,
   progress     float8       not null,
   cmd_array_json varchar(255) not null,
   ctime        timestamp with time zone,
   ftime        timestamp with time zone,
   cookie       varchar(255),
   error        text,
   listener     bytea,
   primary key (id)
);
