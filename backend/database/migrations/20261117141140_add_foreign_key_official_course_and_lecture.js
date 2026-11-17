/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function up(knex) {
    await knex.raw(`
        ALTER TABLE \`official_course_lecture\`
            ADD FOREIGN KEY (\`course_id\`) REFERENCES \`official_course\` (\`id\`)
                ON UPDATE NO ACTION ON DELETE NO ACTION;
    `);
}

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function down(knex) {
  
}
