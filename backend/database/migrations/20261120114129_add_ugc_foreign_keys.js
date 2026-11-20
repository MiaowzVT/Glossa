// database/migration.stub
/**
 * @param { import("knex").Knex } knex
 */
export async function up(knex) {
  await knex.raw(`
    ALTER TABLE \`ugc_lecture\`
      ADD CONSTRAINT \`fk_ugc_course_id\` FOREIGN KEY (\`ugc_course_id\`) REFERENCES \`ugc_course\` (\`id\`)
        ON UPDATE NO ACTION ON DELETE NO ACTION;
  `);
}

/**
 * @param { import("knex").Knex } knex
 */
export async function down(knex) {
  await knex.raw(`ALTER TABLE ugc_lecture DROP FOREIGN KEY \`fk_ugc_course_id\``);
}