CREATE TABLE IF NOT EXISTS users (
    id serial primary key,
    telegram_id integer not null,
    created_at timestamp not null,
    last_action_at timestamp not null,
    state text
);

CREATE TABLE IF NOT EXISTS groups (
    id serial primary key,
    "name" text not null,
    created_at timestamp not null,
    created_by integer references users (id) default 1
);

CREATE TABLE IF NOT EXISTS exercises (
    id serial primary key,
    "name" text not null,
    group_id integer references groups (id),
    created_at timestamp not null,
    created_by integer references users (id) default 1
);

CREATE TABLE IF NOT EXISTS user_exercises (
    -- primary key (user_id, exercise_id)
    id serial primary key,
    user_id integer references users (id),
    exercise_id integer references exercises (id),
    normal_difficulty numeric(6, 2) not null,
    created_at timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS trainings (
    id serial primary key,
    user_id integer references users (id),
    exercise_id integer references exercises (id),
    difficulty numeric(6, 2) not null,
    repeats integer not null,
    grouped boolean not null default FALSE
    created_at timestamp not null default now(),
);
