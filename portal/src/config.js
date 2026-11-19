const path = require('path');
require('dotenv').config();

module.exports = {
  port: parseInt(process.env.PORT || '3000', 10),
  dbType: (process.env.DB_TYPE || (process.env.DATABASE_URL ? 'postgres' : 'sqlite')).toLowerCase(),
  sqlitePath: process.env.SQLITE_PATH || path.join(__dirname, '..', 'data', 'portal.db'),
  databaseUrl: process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/ipvio_portal',
  adminUser: process.env.ADMIN_USER || 'admin',
  adminPass: process.env.ADMIN_PASS || 'admin123',
  adminKey: process.env.ADMIN_KEY || 'admin123',
  sessionSecret: process.env.SESSION_SECRET || 'ipvio-stream-secret-token-key-2026',
  uploadDir: path.join(__dirname, '..', 'public', 'uploads'),
  apkDir: path.join(__dirname, '..', 'public', 'apks')
};
