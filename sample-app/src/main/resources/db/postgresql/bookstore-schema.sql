create schema if not exists bookstore;
create schema if not exists bookstore_jpa;

create table if not exists bookstore.author
(
    id  bigint not null
        constraint author_pkey primary key,
    name varchar(255) not null,
    profile jsonb
);
alter table bookstore.author owner to hql_demo;

--------------------------------------------------------
create table if not exists bookstore.customer
(
    id  bigint not null
        constraint customer_pkey primary key,
    name varchar(255) not null,
    height real,
    mass real,
    memo jsonb
);
alter table bookstore.customer owner to hql_demo;

--------------------------------------------------------
create table if not exists bookstore.customer_friend_link
(
    customer_id bigint not null
        constraint fk_customer_id_2_pk_customer__id
            references bookstore.customer,
    friend_id bigint not null
        constraint fk_friend_id_2_pk_customer__id
            references bookstore.customer
);
alter table bookstore.customer_friend_link owner to hql_demo;
create unique index if not exists customer_id__friend_id__uindex
    on bookstore.customer_friend_link (customer_id, friend_id);


--------------------------------------------------------
create table if not exists bookstore.book
(
    id  bigint not null
        constraint book_pkey
            primary key,
    title varchar(255) not null,
    author_id bigint
        constraint fk_author_id_2_pk_author__id
            references bookstore.author,
--     publisher_id bigint
--         constraint fk_publisher_id_2_pk_publisher__id
--             references bookstore.publisher,
    price real
);
alter table bookstore.book owner to hql_demo;

--------------------------------------------------------
-- create table if not exists bookstore.publisher
-- (
--     id  bigint not null
--         constraint publisher_pkey primary key,
--     name varchar(255) not null,
--     memo jsonb
-- );
-- alter table bookstore.publisher owner to hql_demo;

--------------------------------------------------------
create table if not exists bookstore.book_order
(
    customer_id bigint not null
        constraint fk_customer_id_2_pk_customer__id
            references bookstore.customer,
    book_id bigint not null
        constraint fk_book_id_2_pk_book__id
            references bookstore.book
    -- date date
);
alter table bookstore.book_order owner to hql_demo;
create unique index if not exists customer_id__book_id__uindex
    on bookstore.book_order (customer_id, book_id);
