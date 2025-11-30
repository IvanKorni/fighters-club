-- Удаление таблицы individuals (объединена с users)
DROP TABLE IF EXISTS person.individuals CASCADE;

-- Удаление таблиц addresses и countries (не нужны для игры)
DROP TABLE IF EXISTS person.addresses CASCADE;
DROP TABLE IF EXISTS person.countries CASCADE;

-- Удаление поля active из users
ALTER TABLE person.users DROP COLUMN IF EXISTS active;

-- Удаление схемы person_history и всех таблиц аудита
DROP SCHEMA IF EXISTS person_history CASCADE;

-- Добавление уникальных ограничений для email и nickname (если их еще нет)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_users_email' 
        AND conrelid = 'person.users'::regclass
    ) THEN
        ALTER TABLE person.users ADD CONSTRAINT uk_users_email UNIQUE (email);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_users_nickname' 
        AND conrelid = 'person.users'::regclass
    ) THEN
        ALTER TABLE person.users ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
    END IF;
END $$;


