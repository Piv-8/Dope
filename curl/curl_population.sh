#!/bin/sh

# User Population
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 1, "username": "techieFox92", "password": "Jackrussel1", "name": "Renato", "surname": "Zero", "birth_date": "1999-09-11"}}' "http://web:8080/dope-webapp/api/user"
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 2, "username": "codeMasterX", "password": "Bordercollie2", "name": "Ezio", "surname": "Auditore", "birth_date": "1973-09-11"}}' "http://web:8080/dope-webapp/api/user"
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 3, "username": "quantumDev42", "password": "Goldenretriever3", "name": "Federico", "surname": "Barbarossa", "birth_date": "1999-07-12"}}' "http://web:8080/dope-webapp/api/user"
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 4, "username": "pixelNexus99", "password": "Irishsetter4", "name": "Giulio", "surname": "Cesare", "birth_date": "2001-01-31"}}' "http://web:8080/dope-webapp/api/user"
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 5, "username": "cyberHawkAlpha", "password": "Frenchbulldog5", "name": "Giacomo", "surname": "Mattei", "birth_date": "1956-04-21"}}' "http://web:8080/dope-webapp/api/user"
curl -v -H "Content-Type: application/json" -d '{"user": {"id_user": 6, "username": "_under_score_", "password": "aswdg5hj", "name": "Paolo", "surname": "Paoli", "birth_date": "1988-04-12"}}' "http://web:8080/dope-webapp/api/user"

# Repository Population
curl -v -u quantumDev42:Goldenretriever3 -H "Content-Type: application/json" -d '{"repository": {"id_repo": 1, "name": "repo1", "visible": "public", "owner_id": 3}}' "http://web:8080/dope-webapp/api/protected/repository"
curl -v -u codeMasterX:Bordercollie2 -H "Content-Type: application/json" -d '{"repository": {"id_repo": 2, "name": "repo2", "visible": "private", "owner_id": 2}}' "http://web:8080/dope-webapp/api/protected/repository"
curl -v -u techieFox92:Jackrussel1 -H "Content-Type: application/json" -d '{"repository": {"id_repo": 3, "name": "repo3", "visible": "public", "owner_id": 1}}' "http://web:8080/dope-webapp/api/protected/repository"

# Image Population
curl -v -u quantumDev42:Goldenretriever3 -H "Content-Type: application/json" -d '{"image": {"id_image": 1, "tag": "hello-world:v1.0", "repo": 1, "description": "an image that runs Hello World1", "status": "building"}}' "http://web:8080/dope-webapp/api/protected/image"
curl -v -u codeMasterX:Bordercollie2 -H "Content-Type: application/json" -d '{"image": {"id_image": 2, "tag": "edit:v1.1", "repo": 2, "description": "an image that runs ad editing software", "status": "ready"}}' "http://web:8080/dope-webapp/api/protected/image"
curl -v -u techieFox92:Jackrussel1 -H "Content-Type: application/json" -d '{"image": {"id_image": 3, "tag": "db_store:latest", "repo": 3, "description": "an image that stores information from a database", "status": "building"}}' "http://web:8080/dope-webapp/api/protected/image"
curl -v -u codeMasterX:Bordercollie2 -H "Content-Type: application/json" -d '{"image": {"id_image": 4, "tag": "gamer:stable", "repo": 2, "description": "an image that runs a videogame", "status": "ready"}}' "http://web:8080/dope-webapp/api/protected/image"
curl -v -u quantumDev42:Goldenretriever3 -H "Content-Type: application/json" -d '{"image": {"id_image": 5, "tag": "ml:v2.0", "repo": 1, "description": "an image that runs a machine learning application", "status": "ready"}}' "http://web:8080/dope-webapp/api/protected/image"

# Image Building
curl -v -u codeMasterX:Bordercollie2 -H "Content-Type: application/json" -d '{"containerfile": {"name": "cfile_004", "from": "Ubuntu", "cmd": "echo 004 && sleep infinity", "json_obj": {}, "image_built": 4}}' "http://web:8080/dope-webapp/api/protected/buildimage"
curl -v -u techieFox92:Jackrussel1 -H "Content-Type: application/json" -d '{"containerfile": {"name": "cfile_003", "from": "Alpine", "cmd": "echo 003 && sleep infinity", "json_obj": {}, "image_built": 3}}' "http://web:8080/dope-webapp/api/protected/buildimage"
curl -v -u quantumDev42:Goldenretriever3 -H "Content-Type: application/json" -d '{"containerfile": {"name": "cfile_001", "from": "Debian", "cmd": "echo 001 && sleep infinity", "json_obj": {}, "image_built": 1}}' "http://web:8080/dope-webapp/api/protected/buildimage"
curl -v -u codeMasterX:Bordercollie2 -H "Content-Type: application/json" -d '{"containerfile": {"name": "cfile_002", "from": "Fedora", "cmd": "echo 002 && sleep infinity", "json_obj": {}, "image_built": 2}}' "http://web:8080/dope-webapp/api/protected/buildimage"
curl -v -u quantumDev42:Goldenretriever3 -H "Content-Type: application/json" -d '{"containerfile": {"name": "cfile_005", "from": "Ubuntu", "cmd": "echo 005 && sleep infinity", "json_obj": {}, "image_built": 5}}' "http://web:8080/dope-webapp/api/protected/buildimage"
