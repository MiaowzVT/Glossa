/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function up(knex) {
    await knex.raw(`
        CREATE TABLE IF NOT EXISTS \`official_course\`
        (
            \`id\`
            INTEGER
            NOT
            NULL
            AUTO_INCREMENT
            UNIQUE,
            \`course_name\`
            VARCHAR
        (
            255
        ) NOT NULL,
            \`course_description\` TEXT
        (
            65535
        ) NOT NULL,
            \`course_level\` VARCHAR
        (
            255
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
 * @returns { Promise<void> }
 */
export async function down(knex) {
    await knex.raw(`DROP TABLE official_course`);
}
