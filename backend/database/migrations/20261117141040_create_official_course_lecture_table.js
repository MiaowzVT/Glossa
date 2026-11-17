/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function up(knex) {
    await knex.raw(`
        CREATE TABLE IF NOT EXISTS \`official_course_lecture\`
        (
            \`id\`
            INTEGER
            UNSIGNED
            NOT
            NULL
            AUTO_INCREMENT,
            \`course_id\`
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
            \`created_at\`
            TIMESTAMP,
            \`updated_at\`
            TIMESTAMP,
            PRIMARY KEY
        (
            \`id\`
        )
            );
    `);
}

/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function down(knex) {
    await knex.raw(`DROP TABLE official_course_lecture`);
}
