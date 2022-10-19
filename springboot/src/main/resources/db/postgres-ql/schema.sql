create table if not exists character
(
    dtype varchar(31) not null,
    id bigint generated by default as identity
        constraint character_pkey
            primary key,
    name varchar(255) not null,
    primary_function varchar(255),
    height real,
    home_planet varchar(255),
    mass real
);
alter table character owner to petclinic;

--------------------------------------------------------
create table if not exists character_friend_link
(
    character_id bigint not null
        constraint fk82widfjywpqyvg2bil5te0c7x
            references character,
    friend_id bigint not null
        constraint fkg0k2i85sbpb7t9xn764di4k57
            references character
);
alter table character_friend_link owner to petclinic;
create unique index if not exists character_friend_link_character_id_friend_id_uindex
    on character_friend_link (character_id, friend_id);


--------------------------------------------------------
create table if not exists episode
(
    title varchar(255) not null
        constraint episode_pkey
            primary key,
    published timestamp
);
alter table episode owner to petclinic;

--------------------------------------------------------
create table if not exists character_episode_link
(
    character_id bigint not null
        constraint fkjue7nlka5950gy1hcgqnmmyly
            references character,
    episode_id varchar(255) not null
        constraint fk69vmpbrqhce7vx6dpw3mdpajl
            references episode
);
alter table character_episode_link owner to petclinic;
create unique index if not exists character_episode_link_character_id_episode_id_uindex
    on character_episode_link (character_id, episode_id);


--------------------------------------------------------
create table if not exists starship
(
    id bigint generated by default as identity
        constraint starship_pkey
            primary key,
    length real,
    name varchar(255) not null
);
alter table starship owner to petclinic;

--------------------------------------------------------
create table if not exists human_starship_link
(
    starship_id bigint not null
        constraint fkajndmksah04mgswl5hge3l69u
            references starship,
    human_id bigint not null
        constraint fkes0hn8h1yby0wj5hmobwc87y4
            references character
);
alter table human_starship_link owner to petclinic;
create unique index if not exists human_starship_link_starship_id_human_id_uindex
    on human_starship_link (starship_id, human_id);


