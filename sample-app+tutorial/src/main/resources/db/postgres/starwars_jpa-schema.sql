-- create schema if not exists starwars_jpa;
--
-- create table if not exists starwars_jpa.character
-- (
--     id bigserial not null
--         constraint character_pkey primary key,
--     species varchar(31) not null,
--     name varchar(255) not null,
--     height real,
--     mass real,
--     metadata jsonb
-- );
-- alter table starwars_jpa.character owner to jql_demo;
--
-- --------------------------------------------------------
-- create table if not exists starwars_jpa.character_friend_link
-- (
--     character_id bigint not null
--         constraint fk_character_id_2_pk_character__id
--             references starwars_jpa.character,
--     friend_id bigint not null
--         constraint fk_friend_id_2_pk_character__id
--             references starwars_jpa.character
-- );
-- alter table starwars_jpa.character_friend_link owner to jql_demo;
-- create unique index if not exists character_id__friend_id__uindex
--     on starwars_jpa.character_friend_link (character_id, friend_id);
--
--
-- --------------------------------------------------------
-- create table if not exists starwars_jpa.episode
-- (
--     title varchar(255) not null
--         constraint episode_pkey
--             primary key,
--     published timestamp
-- );
-- alter table starwars_jpa.episode owner to jql_demo;
--
-- --------------------------------------------------------
-- create table if not exists starwars_jpa.character_episode_link
-- (
--     character_id bigint not null
--         constraint fk_character_id_2_pk_character__id
--             references starwars_jpa.character,
--     episode_id varchar(255) not null
--         constraint fk_episode_id_2_pk_episode__title
--             references starwars_jpa.episode
-- );
-- alter table starwars_jpa.character_episode_link owner to jql_demo;
-- create unique index if not exists character_id__episode_id__uindex
--     on starwars_jpa.character_episode_link (character_id, episode_id);
--
--
-- --------------------------------------------------------
-- create table if not exists starwars_jpa.starship
-- (
--     id bigserial not null
--         constraint starship_pkey
--             primary key,
--     pilot_id bigint
--         constraint fk_pilot_id_2_pk_character__id
--             references starwars_jpa.character,
--     length real,
--     name varchar(255) not null
-- );
-- alter table starwars_jpa.starship owner to jql_demo;
--
-- --------------------------------------------------------
--