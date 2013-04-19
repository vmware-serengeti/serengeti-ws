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
   id                  bigint       not null unique DEFAULT nextval('cluster_seq'::regclass),
   name                varchar(255) not null unique,
   distro              varchar(255),
   distro_vendor       varchar(255),
   distro_version      varchar(255),
   topology            varchar(255) not null,
   status              varchar(255) not null,
   vc_datastore_names  text,
   vc_rp_names         text,
   network_id          bigint,
   start_after_deploy  boolean,
   automation_enable   boolean,
   vhm_min_num         integer,
   vhm_target_num      integer,
   latest_task_id      bigint,
   vhm_master_moid     varchar(255),
   vhm_jobtracker_port varchar(255),
   configuration       text,
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
   swap_ratio             real,
   defined_instance_num   integer not null,
   ha_flag                varchar(10),
   storage_type           varchar(255),
   storage_size           integer,
   ioshare_type           varchar(16),
   vhm_target_num         integer,
   vc_datastore_names     text,
   vc_rp_names            text,
   group_racks            text,
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

create sequence node_seq;
create table node (
   id           bigint       not null unique DEFAULT nextval('node_seq'::regclass),
   vm_name      varchar(255) not null unique,
   moid         varchar(255) unique,
   rack         varchar(255),
   host_name    varchar(255),
   status       varchar(255),
   action       varchar(255),
   power_status_changed       boolean,
   vc_datastores text,
   volumes       text,
   ip_address   varchar(255),
   guest_host_name  varchar(255),
   node_group_id bigint,
   vc_rp_id     bigint,
   primary key (id),
   foreign key(node_group_id) references node_group(id) ON DELETE CASCADE,
   foreign key(vc_rp_id) references vc_resource_pool(id) ON DELETE CASCADE
);

create sequence disk_seq;
create table disk (
   id             bigint       not null unique DEFAULT nextval('disk_seq'::regclass),
   name           varchar(255),
   size           integer,
   alloc_type     varchar(255),
   disk_type      varchar(255),
   external_addr  varchar(255),
   dev_name       varchar(255),
   ds_moid        varchar(255),
   ds_name        varchar(255),
   vmdk_path      varchar(255),
   node_id        bigint,
   primary key (id),
   foreign key(node_id) references node(id) ON DELETE CASCADE
);

create sequence rack_seq;
create table rack (
   id           bigint       not null unique DEFAULT nextval('rack_seq'::regclass),
   name         varchar(255) not null,
   primary key (id)
);

create sequence physical_host_seq;
create table physical_host (
   id           bigint       not null unique DEFAULT nextval('physical_host_seq'::regclass),
   name         varchar(255) not null,
   rack_id      bigint,
   primary key (id),
   foreign key(rack_id) references rack(id) ON DELETE CASCADE
);

create sequence server_info_seq;
create table server_info (
  id           bigint       not null unique DEFAULT nextval('server_info_seq'::regclass),
  resource_initialized boolean not null DEFAULT false
);
