// database/migration.stub
/**
 * @param { import("knex").Knex } knex
 */
export async function up(knex) {
  await knex.raw(`
    CREATE TABLE IF NOT EXISTS \`ugc_lecture\`
    (
      \`id\`
      INTEGER
      NOT
      NULL
      AUTO_INCREMENT,
      \`ugc_course_id\`
      INTEGER
      NOT
      NULL,
      \`lecture_name\`
      VARCHAR
    (
      255
    ) NOT NULL,
      \`lecture_contents\` TEXT
    (
      65535
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
  await knex.raw(`DROP TABLE ugc_lecture`);
}