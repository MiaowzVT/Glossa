// database/migration.stub
/**
 * @param { import("knex").Knex } knex
 */
export async function up(knex) {
  await knex.raw(`
    ALTER TABLE \`ugc_course\`
      ADD CONSTRAINT \`fk_ugc_course_author\` FOREIGN KEY (\`author_id\`) REFERENCES \`users\` (\`id\`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
  `);
}

/**
 * @param { import("knex").Knex } knex
 */
export async function down(knex) {
  await knex.raw(`ALTER TABLE \`ugc_course\` DROP FOREIGN KEY \`fk_ugc_course_author\``);
}