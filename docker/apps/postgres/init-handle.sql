CREATE DATABASE handlesystem;
\c handlesystem;

-- Schema required by Handle.Net for PostgreSQL
CREATE TABLE nas (na bytea not null, primary key(na));

CREATE TABLE handles (
                         handle bytea not null, idx int4 not null, type bytea, data bytea,
                         ttl_type int2, ttl int4, timestamp int4, refs text,
                         admin_read bool, admin_write bool, pub_read bool, pub_write bool,
                         primary key(handle, idx)
);

-- Home the test prefix
INSERT INTO nas (na) VALUES (convert_to('0.NA/99999', 'UTF8'));

-- Create the Admin Handle with an HS_SECKEY (password: 'testsecret') at index 300
INSERT INTO handles (handle, idx, type, data, ttl_type, ttl, timestamp, admin_read, admin_write, pub_read, pub_write)
VALUES (
           convert_to('99999/ADMIN', 'UTF8'), 300,
           convert_to('HS_SECKEY', 'UTF8'), convert_to('testsecret', 'UTF8'),
           0, 86400, EXTRACT(EPOCH FROM NOW()), true, true, true, true
       );