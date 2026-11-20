// database/migration.stub
/**
 * @param { import("knex").Knex } knex
 */
export async function up(knex) {
  await knex.raw(`
    CREATE TABLE IF NOT EXISTS \`ugc_course\`
    (
      \`id\`
      INTEGER
      NOT
      NULL
      AUTO_INCREMENT,
      \`author_id\`
      INTEGER
      NOT
      NULL,
      \`course_name\`
      VARCHAR
    (
      255
    ) NOT NULL,
      \`course_description\` TEXT
    (
      65535
    ) NOT NULL,
      \`course_language\` VARCHAR
    (
      255
    ) NOT NULL,
      \`created_at\` TIMESTAMP NOT NULL,
      \`updated_at\` TIMESTAMP NOT NULL,
      PRIMARY KEY
    (
      \`id\`
    )
      );
  `);
}

/**
 * @param { import("knex").Knex } knex
 */
export async function down(knex) {
  await knex.raw(`DROP TABLE ugc_course`);
}