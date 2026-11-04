-- Database Creation
CREATE DATABASE dope
OWNER "dope_group"
ENCODING="UTF8";

-- Connect to the Database as user dope_group
\c dope dope_group

-- Creation of Personalized Data Types
CREATE SCHEMA dope;
SET SCHEMA 'dope';

-- Define the ENUM types within the dope schema
CREATE TYPE dope.visibility AS ENUM (
'public',
'private'
);

CREATE TYPE dope.status_container AS ENUM (
'created',
'stopped',
'running',
'paused',
'restarting',
'exited',
'dead'
);

CREATE TYPE dope.status_image AS ENUM (
'building',
'ready'
);

-- Tables Creation
CREATE TABLE dope."user" (
"id_user" SERIAL PRIMARY KEY,
"username" VARCHAR(50) NOT NULL,
"password" VARCHAR(255) NOT NULL,
"name" VARCHAR(50) NOT NULL,
"surname" VARCHAR(50) NOT NULL,
"birth_date" DATE NOT NULL
);

CREATE TABLE dope.repository (
"id_repo" SERIAL PRIMARY KEY,
"name" VARCHAR(50) NOT NULL,
"visible" dope.visibility NOT NULL,
"owner_id" INTEGER NOT NULL,
FOREIGN KEY("owner_id") REFERENCES dope."user"("id_user")
);

CREATE TABLE dope.image (
"id_image" SERIAL PRIMARY KEY,
"tag" VARCHAR(50) NOT NULL,
"repo" INTEGER NOT NULL,
"description" VARCHAR(1000),
"status" dope.status_image NOT NULL,
FOREIGN KEY ("repo") REFERENCES dope.repository("id_repo")
);

CREATE TABLE dope.container (
"instance_id" SERIAL PRIMARY KEY,
"status" dope.status_container NOT NULL,
"image_run" INTEGER NOT NULL,
"user_instance" INTEGER NOT NULL,
FOREIGN KEY ("user_instance") REFERENCES dope."user"("id_user"),
FOREIGN KEY ("image_run") REFERENCES dope.image("id_image")
);

CREATE TABLE dope.layer (
"id_layer" VARCHAR(255) NOT NULL,
"size" INTEGER,
"image_owner" INTEGER NOT NULL,
PRIMARY KEY("id_layer", "image_owner"),
FOREIGN KEY ("image_owner") REFERENCES dope.image("id_image")
);

CREATE TABLE dope.containerfile (
"name" VARCHAR(50) PRIMARY KEY,
"from" VARCHAR(50) NOT NULL,
"cmd" VARCHAR(50) NOT NULL,
"json_obj" JSON NOT NULL,
"image_built" INTEGER NULL,
FOREIGN KEY ("image_built") REFERENCES dope.image("id_image")
);
