-- INSERT Operations as dope_group
\c dope dope_group

SET SCHEMA 'dope';

-- Stored passwords are hashed using BCrypt. In order, the list of passwords is the following:
-- Jack-russel1
-- Border-collie2
-- Golden-retriever3
-- Irish-setter4
-- French-bulldog5
INSERT INTO dope."user"("username", "password", "name", "surname", "birth_date") VALUES
('techieFox92', '$2a$12$cMCoB/QhqEKD/WuptdKTIebT.e7N3k2xZuCqSNSiDXpLQeAGRFDku',  'Renato', 'Zero', '1992-12-25'),
('codeMasterX', '$2a$12$.OjzUccqmeZ7XNq9m4uYCuHbSY/TJxql4fE/k4cf9ejDYHHBxopMm', 'Ezio', 'Auditore', '1973-09-11'),
('quantumDev42', '$2a$12$zWH/9oQEHBmm7lFd6xN4VeZojKmLk3ngRz/ciCtvOL4JmYCjvssGu', 'Federico', 'Barbarossa', '1999-07-12'),
('pixelNexus99', '$2a$12$vYTnr.UzCdtZves9U3dfeeCJ6afOJxM/uGjRb0tuugQMHRlqVzNhK', 'Giulio', 'Cesare', '2001-01-31'),
('cyberHawkAlpha', '$2a$12$toEho/qE6Xt654W.RfbDBe/XGRyOc9tbb/xXh9znc/E96e0OS1YEO', 'Giacomo', 'Mattei', '1956-04-21');

-- Insert repositories with correct references to "user"
INSERT INTO dope.repository("name", "visible", "owner_id") VALUES
('repo1', 'public', 3),
('repo2', 'private', 2),
('repo3', 'public', 1);

-- Insert images with correct references to "repository" and "status_image" type
INSERT INTO dope.image("tag", "repo", "description", "status") VALUES
('hello-world:v1.0', 1, 'an image that runs Hello World1', 'building'),
('edit:v1.1', 2, 'an image that runs ad editing software', 'ready'),
('db_store:latest', 3, 'an image that stores information from a database', 'building'),
('gamer:stable', 2, 'an image that runs a videogame', 'ready'),
('ml:v2.0', 1, 'an image that runs a machine learning application', 'ready');

-- Insert containers with correct references to "image" and "status_container" type
INSERT INTO dope.container("status", "image_run", "user_instance") VALUES
('running', 4, 3),
('stopped', 5, 2),
('paused', 3, 4),
('running', 1, 2),
('stopped', 5, 5);

-- Insert layers with correct references to "image"
INSERT INTO dope.layer("id_layer", "size", "image_owner") VALUES
('L001', 100, 4),
('L002', 200, 5),
('L003', 150, 1),
('L004', 300, 2),
('L005', 50, 3);

-- Insert containerfiles with correct references to "image_built"
INSERT INTO dope.containerfile("name", "from", "cmd", "json_obj", "image_built") VALUES
('cfile_004', 'Ubuntu', 'bash', '{"run":["cp test test2"]}', 4),
('cfile_003', 'Alpine', 'sleep infinity', '{"run":["cp test test2"]}', 3),
('cfile_001', 'Debian', 'apache2ctl start', '{"run":["cp test test2"]}', 1),
('cfile_002', 'Fedora', 'avahi-daemon', '{"run":["cp test test2"]}', 2),
('cfile_005', 'CentOS', 'vscode-server', '{"run":["cp test test2"]}', 5);

-- TODO: remember to use the same PRIMARY KEYS of User table in Container parameter 'user_instance'
-- TODO: remember to use the same PRIMARY KEYS of Repository table in Image parameter 'repo'
