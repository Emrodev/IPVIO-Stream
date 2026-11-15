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

  const createBackupsTable = `
    CREATE TABLE IF NOT EXISTS device_backups (
      device_id VARCHAR(100) PRIMARY KEY,
      account_key VARCHAR(150),
      summary TEXT,
      backup_data TEXT NOT NULL,
      updated_at BIGINT NOT NULL
    );
  `;

  const createAccountSyncTable = `
    CREATE TABLE IF NOT EXISTS account_sync (
      account_key VARCHAR(150) NOT NULL,
      sync_type VARCHAR(50) NOT NULL,
      item_id VARCHAR(100) NOT NULL,
      data_json TEXT NOT NULL,
      updated_at BIGINT NOT NULL,
      PRIMARY KEY (account_key, sync_type, item_id)
    );
  `;

  const createTestRequestsTable = `
    CREATE TABLE IF NOT EXISTS test_requests (
      id ${isPg ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
      phone VARCHAR(50) NOT NULL,
      name VARCHAR(150),
      type VARCHAR(50) DEFAULT 'without_adults',
      username VARCHAR(100),
      password VARCHAR(100),
      dns VARCHAR(255),
      package_name TEXT,
      expires_at_formatted VARCHAR(100),
      pay_url TEXT,
      raw_reply TEXT,
      ip VARCHAR(50),
      is_valid_wa INTEGER DEFAULT 0,
      wa_lid VARCHAR(100),
      created_at BIGINT NOT NULL
    );
  `;

  await dbDriver.exec(createDevicesTable);
  await dbDriver.exec(createOtaTable);
  await dbDriver.exec(createSettingsTable);
  await dbDriver.exec(createBackupsTable);
  await dbDriver.exec(createAccountSyncTable);
  await dbDriver.exec(createTestRequestsTable);

  // Migrações suaves para colunas adicionais
  const alterColumns = [
    'ALTER TABLE devices ADD COLUMN playlist_exp_date TEXT',
    'ALTER TABLE devices ADD COLUMN playlist_status TEXT',
    'ALTER TABLE devices ADD COLUMN playlist_max_cons INTEGER',
    'ALTER TABLE devices ADD COLUMN pairing_code TEXT',
    'ALTER TABLE devices ADD COLUMN now_playing TEXT',
    'ALTER TABLE devices ADD COLUMN now_playing_type TEXT',
    'ALTER TABLE devices ADD COLUMN now_playing_at BIGINT DEFAULT 0',
    'ALTER TABLE devices ADD COLUMN client_phone TEXT',
    'ALTER TABLE devices ADD COLUMN client_name TEXT',
    'ALTER TABLE devices ADD COLUMN notes TEXT',
    'ALTER TABLE devices ADD COLUMN last_expiry_notified_at BIGINT DEFAULT 0',
    'ALTER TABLE test_requests ADD COLUMN is_valid_wa INTEGER DEFAULT 0',
    'ALTER TABLE test_requests ADD COLUMN wa_lid VARCHAR(100)'
  ];
  for (const alterSql of alterColumns) {
    try {
      await dbDriver.exec(alterSql);
    } catch (_) {
      // Ignora se a coluna já existir
    }
  }

  console.log('[DB] Tabelas verificadas/criadas com sucesso.');
  await migrateFromSqliteIfPresent();
}

async function migrateFromSqliteIfPresent() {
  if (dbDriver.type !== 'postgres') return;
  const sqliteFile = config.sqlitePath;
  if (!fs.existsSync(sqliteFile)) return;

  const sqlite3 = require('sqlite3').verbose();
  let sdb;
  try {
    sdb = await new Promise((resolve, reject) => {
      const d = new sqlite3.Database(sqliteFile, sqlite3.OPEN_READONLY, (err) => {
        if (err) reject(err);
        else resolve(d);
      });
    });
  } catch (err) {
    console.warn('[DB Migration] Não foi possível abrir arquivo SQLite:', err.message);
    return;
  }

  const querySqlite = (sql) => new Promise((resolve) => {
    sdb.all(sql, [], (err, rows) => resolve(rows || []));
  });

  try {
    // 1. Configurações (admin_settings)
    const existingSettings = await dbDriver.query('SELECT count(*) as c FROM admin_settings');
    const settingsCount = parseInt(existingSettings.rows[0]?.c || '0', 10);
    if (settingsCount === 0) {
      const sqliteSettings = await querySqlite('SELECT key, value FROM admin_settings');
      for (const row of sqliteSettings) {
        await dbDriver.query(
          'INSERT INTO admin_settings (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value',
          [row.key, row.value]
        );
      }
      if (sqliteSettings.length > 0) {
        console.log(`[DB Migration] ✅ Migradas ${sqliteSettings.length} configurações do SQLite para o PostgreSQL.`);
      }
    }

    // 2. Atualizações OTA (ota_updates)
    const existingOta = await dbDriver.query('SELECT count(*) as c FROM ota_updates');
    const otaCount = parseInt(existingOta.rows[0]?.c || '0', 10);
    if (otaCount === 0) {
      const sqliteOta = await querySqlite('SELECT * FROM ota_updates');
      for (const row of sqliteOta) {
        await dbDriver.query(
          'INSERT INTO ota_updates (version_name, version_code, download_url, changelog, mandatory, released_at) VALUES (?, ?, ?, ?, ?, ?)',
          [row.version_name, row.version_code, row.download_url, row.changelog || '', row.mandatory || 0, row.released_at]
        );
      }
      if (sqliteOta.length > 0) {
        console.log(`[DB Migration] ✅ Migradas ${sqliteOta.length} atualizações OTA do SQLite para o PostgreSQL.`);
      }
    }

    // 3. Dispositivos (devices)
    const existingDevs = await dbDriver.query('SELECT count(*) as c FROM devices');
    const devCount = parseInt(existingDevs.rows[0]?.c || '0', 10);
    if (devCount === 0) {
      const sqliteDevs = await querySqlite('SELECT * FROM devices');
      for (const d of sqliteDevs) {
        await dbDriver.query(
          `INSERT INTO devices (
            id, mac, name, model, app_version, ip, status, playlist_type, playlist_url,
            xtream_host, xtream_user, xtream_pass, playlist_updated_at, message, message_id,
            message_type, created_at, last_seen, playlist_exp_date, playlist_status,
            playlist_max_cons, pairing_code, now_playing, now_playing_type, now_playing_at,
            client_phone, client_name, notes, last_expiry_notified_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT (id) DO NOTHING`,
          [
            d.id, d.mac, d.name, d.model, d.app_version, d.ip, d.status || 'active',
            d.playlist_type || 'none', d.playlist_url, d.xtream_host, d.xtream_user, d.xtream_pass,
            d.playlist_updated_at || 0, d.message, d.message_id, d.message_type || 'info',
            d.created_at || Date.now(), d.last_seen || Date.now(), d.playlist_exp_date,
            d.playlist_status, d.playlist_max_cons, d.pairing_code, d.now_playing,
            d.now_playing_type, d.now_playing_at || 0, d.client_phone, d.client_name,
            d.notes, d.last_expiry_notified_at || 0
          ]
        );
      }
      if (sqliteDevs.length > 0) {
        console.log(`[DB Migration] ✅ Migrados ${sqliteDevs.length} dispositivos do SQLite para o PostgreSQL.`);
      }
    }

    // 4. Test Requests (test_requests)
    const existingTests = await dbDriver.query('SELECT count(*) as c FROM test_requests');
    const testsCount = parseInt(existingTests.rows[0]?.c || '0', 10);
    if (testsCount === 0) {
      const sqliteTests = await querySqlite('SELECT * FROM test_requests');
      for (const t of sqliteTests) {
        await dbDriver.query(
          `INSERT INTO test_requests (
            phone, name, type, username, password, dns, package_name, expires_at_formatted,
            pay_url, raw_reply, ip, is_valid_wa, wa_lid, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
          [
            t.phone, t.name, t.type, t.username, t.password, t.dns, t.package_name,
            t.expires_at_formatted, t.pay_url, t.raw_reply, t.ip, t.is_valid_wa || 0,
            t.wa_lid || null, t.created_at
          ]
        );
      }
      if (sqliteTests.length > 0) {
        console.log(`[DB Migration] ✅ Migradas ${sqliteTests.length} solicitações de testes do SQLite para o PostgreSQL.`);
      }
    }

    // 5. Backups & Account Sync
    const existingBackups = await dbDriver.query('SELECT count(*) as c FROM device_backups');
    if (parseInt(existingBackups.rows[0]?.c || '0', 10) === 0) {
      const sqliteBackups = await querySqlite('SELECT * FROM device_backups');
      for (const b of sqliteBackups) {
        await dbDriver.query(
          'INSERT INTO device_backups (device_id, account_key, summary, backup_data, updated_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (device_id) DO NOTHING',
          [b.device_id, b.account_key, b.summary, b.backup_data, b.updated_at]
        );
      }
    }

    const existingSync = await dbDriver.query('SELECT count(*) as c FROM account_sync');
    if (parseInt(existingSync.rows[0]?.c || '0', 10) === 0) {
      const sqliteSync = await querySqlite('SELECT * FROM account_sync');
      for (const s of sqliteSync) {
        await dbDriver.query(
          'INSERT INTO account_sync (account_key, sync_type, item_id, data_json, updated_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (account_key, sync_type, item_id) DO NOTHING',
          [s.account_key, s.sync_type, s.item_id, s.data_json, s.updated_at]
        );
      }
    }
  } catch (err) {
    console.error('[DB Migration Warning] Erro durante migração automática:', err.message);
  } finally {
    sdb.close();
  }
}

module.exports = {
  init,
  query: (sql, params) => dbDriver.query(sql, params),
  exec: (sql) => dbDriver.exec(sql),
  close: () => dbDriver.close(),
  get driver() { return dbDriver; }
};
