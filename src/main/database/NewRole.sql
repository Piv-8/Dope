--Creating new Role

CREATE ROLE dope_group
    LOGIN
    CREATEROLE
    CREATEDB
    REPLICATION
    ENCRYPTED PASSWORD 'admin';