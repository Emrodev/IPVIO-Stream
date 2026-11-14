const fs = require('fs');
const path = require('path');
const config = require('../config');

let dbDriver = null;

// Normalize parameters from '?' to '$1, $2, ...' for Postgres
function toPostgresSql(sql) {
  let paramIndex = 1;
  return sql.replace(/\?/g, () => `$${paramIndex++}`);
}

async function init() {
  if (config.dbType === 'postgres' || config.dbType === 'postgresql') {
    const { Pool } = require('pg');
    const pool = new Pool({ connectionString: config.databaseUrl });
    
    dbDriver = {
      type: 'postgres',
      async query(sql, params = []) {
        const pgSql = toPostgresSql(sql);
        const res = await pool.query(pgSql, params);
        return { rows: res.rows };
      },
      async exec(sql) {
        await pool.query(sql);
      },
      async close() {
        await pool.end();
      }
    };
    console.log('[DB] Conectado ao PostgreSQL:', config.databaseUrl.replace(/:[^:@]+@/, ':***@'));
  } else {
    // SQLite
    const sqlite3 = require('sqlite3').verbose();
    const dbDir = path.dirname(config.sqlitePath);
    if (!fs.existsSync(dbDir)) {
      fs.mkdirSync(dbDir, { recursive: true });
    }

    const db = await new Promise((resolve, reject) => {
      const s = new sqlite3.Database(config.sqlitePath, (err) => {
        if (err) reject(err);
        else resolve(s);
      });
    });

    dbDriver = {
      type: 'sqlite',
      async query(sql, params = []) {
        const trimmed = sql.trim().toUpperCase();
        return new Promise((resolve, reject) => {
          if (trimmed.startsWith('SELECT') || trimmed.startsWith('PRAGMA')) {
            db.all(sql, params, (err, rows) => {
              if (err) reject(err);
              else resolve({ rows: rows || [] });
            });
          } else {
            db.run(sql, params, function(err) {
              if (err) reject(err);
              else resolve({ rows: [], lastID: this.lastID, changes: this.changes });
            });
          }
        });
      },
      async exec(sql) {
        return new Promise((resolve, reject) => {
          db.exec(sql, (err) => {
            if (err) reject(err);
            else resolve();
          });
        });
      },
      async close() {
        return new Promise((resolve, reject) => {
          db.close((err) => {
            if (err) reject(err);
            else resolve();
          });
        });
      }
    };
    console.log('[DB] Conectado ao SQLite em:', config.sqlitePath);
  }

  await createTables();
}

async function createTables() {
  const isPg = dbDriver.type === 'postgres';
  
  const createDevicesTable = `
    CREATE TABLE IF NOT EXISTS devices (
      id VARCHAR(100) PRIMARY KEY,
      mac VARCHAR(50),
      name VARCHAR(150),
      model VARCHAR(150),
      app_version VARCHAR(50),
      ip VARCHAR(50),
      status VARCHAR(20) DEFAULT 'active',
      playlist_type VARCHAR(20) DEFAULT 'none',
      playlist_url TEXT,
      xtream_host TEXT,
      xtream_user TEXT,
      xtream_pass TEXT,
      playlist_updated_at BIGINT DEFAULT 0,
      message TEXT,
      message_id VARCHAR(50),
      message_type VARCHAR(20) DEFAULT 'info',
      created_at BIGINT,
      last_seen BIGINT
    );
  `;

  const createOtaTable = `
    CREATE TABLE IF NOT EXISTS ota_updates (
      id ${isPg ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
      version_name VARCHAR(50) NOT NULL,
      version_code INTEGER NOT NULL,
      download_url TEXT NOT NULL,
      changelog TEXT,
      mandatory INTEGER DEFAULT 0,
      released_at BIGINT NOT NULL
    );
  `;

  const createSettingsTable = `
    CREATE TABLE IF NOT EXISTS admin_settings (
      key VARCHAR(100) PRIMARY KEY,
      value TEXT
    );
  `;

  await dbDriver.exec(createDevicesTable);
  await dbDriver.exec(createOtaTable);
  await dbDriver.exec(createSettingsTable);

  // Migrações suaves para colunas adicionais
  const alterColumns = [
    'ALTER TABLE devices ADD COLUMN playlist_exp_date TEXT',
    'ALTER TABLE devices ADD COLUMN playlist_status TEXT',
    'ALTER TABLE devices ADD COLUMN playlist_max_cons INTEGER'
  ];
  for (const alterSql of alterColumns) {
    try {
      await dbDriver.exec(alterSql);
    } catch (_) {
      // Ignora se a coluna já existir
    }
  }

  console.log('[DB] Tabelas verificadas/criadas com sucesso.');
}

module.exports = {
  init,
  query: (sql, params) => dbDriver.query(sql, params),
  exec: (sql) => dbDriver.exec(sql),
  close: () => dbDriver.close(),
  get driver() { return dbDriver; }
};
