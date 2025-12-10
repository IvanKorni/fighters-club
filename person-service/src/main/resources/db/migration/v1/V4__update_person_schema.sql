ALTER TABLE person.users
    DROP COLUMN address_id,
    DROP COLUMN last_name;

ALTER TABLE person.users
    RENAME COLUMN first_name TO nickname;

-- ALTER TABLE person.users
--     ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
--
-- ALTER TABLE person.users
--     ADD CONSTRAINT uk_users_email UNIQUE (email);

ALTER TABLE person.individuals
    DROP COLUMN passport_number,
    DROP COLUMN phone_number;

ALTER TABLE person_history.users_history
    DROP COLUMN address_id,
    DROP COLUMN last_name;

ALTER TABLE person_history.users_history
    RENAME COLUMN first_name TO nickname;

ALTER TABLE person_history.individuals_history
    DROP COLUMN passport_number,
    DROP COLUMN phone_number;

