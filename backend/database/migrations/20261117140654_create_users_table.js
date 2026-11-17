/**
 * @param { import("knex").Knex } knex
 * @returns { Promise<void> }
 */
export async function up(knex) {
    await knex.raw(`
        CREATE TABLE IF NOT EXISTS \`users\`
        (
            \`id\`
            INTEGER
            NOT
            NULL
            AUTO_INCREMENT
            UNIQUE,
            \`username\`
            VARCHAR
        (
            255
        ) NOT NULL UNIQUE,
            \`email\` VARCHAR
        (
            255
        ) NOT NULL UNIQUE,
            \`pw_hash\` VARCHAR
        (
            255
        ) NOT NULL,
            \`created_at\` TIMESTAMP,
            \`updated_at\` TIMESTAMP,
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
  await knex.raw(`DROP TABLE users`);
}
