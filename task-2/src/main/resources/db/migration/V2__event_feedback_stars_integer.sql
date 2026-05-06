-- Hibernate maps Java int to INTEGER; V1 used SMALLINT.
ALTER TABLE event_feedback
    ALTER COLUMN stars TYPE INTEGER USING stars::integer;
