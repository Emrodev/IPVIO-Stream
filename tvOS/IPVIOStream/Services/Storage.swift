import CryptoKit
import Foundation
import Security

// MARK: - Keychain / AES-GCM

enum Vault {
    private static let service = "com.ipvio.stream.tvos"
    private static let account = "vault-key-v1"
    private static let lock = NSLock()
    private static var cachedKey: SymmetricKey?
    private static var usingFallback = false

    static func seal<T: Encodable>(_ value: T) throws -> Data {
        let clear = try JSONEncoder.ipvio.encode(value)
        let box = try AES.GCM.seal(clear, using: key())
        guard let combined = box.combined else {
            throw VaultError.encryptionFailed
        }
        return combined
    }

    static func open<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        let box = try AES.GCM.SealedBox(combined: data)
        let clear = try AES.GCM.open(box, using: key())
        return try JSONDecoder.ipvio.decode(type, from: clear)
    }

    static var isUsingFallbackKey: Bool {
        lock.lock()
        defer { lock.unlock() }
        return usingFallback
    }

    private static func key() -> SymmetricKey {
        lock.lock()
        defer { lock.unlock() }
        if let cachedKey { return cachedKey }

        let data: Data
        if let stored = readKeychainKey() {
            data = stored
        } else {
            let newKey = randomKey()
            if writeKeychainKey(newKey) {
                data = newKey
            } else {
                // Extremely rare (locked keychain during first launch). Keep the app usable and
                // still encrypt the data; the fallback key lives in the app sandbox.
                usingFallback = true
                data = fallbackKey()
            }
        }
        let key = SymmetricKey(data: data)
        cachedKey = key
        return key
    }

    private static func randomKey() -> Data {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = bytes.withUnsafeMutableBytes { buffer in
            SecRandomCopyBytes(kSecRandomDefault, buffer.count, buffer.baseAddress!)
        }
        if status != errSecSuccess {
            bytes = (0..<32).map { _ in UInt8.random(in: 0...255) }
        }
        return Data(bytes)
    }

    private static func fallbackKey() -> Data {
        let defaults = UserDefaults.standard
        if let value = defaults.data(forKey: "ipvio.vault.fallback"), value.count == 32 {
            return value
        }
        let value = randomKey()
        defaults.set(value, forKey: "ipvio.vault.fallback")
        return value
    }

    private static func readKeychainKey() -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data, data.count == 32 else { return nil }
        return data
    }

    private static func writeKeychainKey(_ data: Data) -> Bool {
        let base: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        SecItemDelete(base as CFDictionary)
        var add = base
        add[kSecValueData as String] = data
        return SecItemAdd(add as CFDictionary, nil) == errSecSuccess
    }
}

enum VaultError: Error {
    case encryptionFailed
}

extension JSONEncoder {
    static let ipvio: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()
}

extension JSONDecoder {
    static let ipvio: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
}

// MARK: - Persistence

actor Storage {
    static let shared = Storage()

    private let fileManager = FileManager.default

    private func rootURL() throws -> URL {
        let base = try fileManager.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let directory = base.appendingPathComponent("IPVIO", isDirectory: true)
        try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    private func stateEncryptedURL() throws -> URL {
        try rootURL().appendingPathComponent("state.enc")
    }

    private func statePlainURL() throws -> URL {
        try rootURL().appendingPathComponent("state.json")
    }

    private func catalogURL(_ id: UUID) throws -> URL {
        try rootURL().appendingPathComponent("catalog-\(id.uuidString.lowercased()).json")
    }

    private func metadataURL() throws -> URL {
        try rootURL().appendingPathComponent("metadata.json")
    }

    func loadState() -> PersistedState {
        do {
            let encryptedURL = try stateEncryptedURL()
            if fileManager.fileExists(atPath: encryptedURL.path) {
                let data = try Data(contentsOf: encryptedURL)
                return try Vault.open(PersistedState.self, from: data)
            }
            let plainURL = try statePlainURL()
            if fileManager.fileExists(atPath: plainURL.path) {
                let data = try Data(contentsOf: plainURL)
                return try JSONDecoder.ipvio.decode(PersistedState.self, from: data)
            }
        } catch {
            print("[IPVIO] Could not read state: \(error)")
        }
        return .empty
    }

    func saveState(_ state: PersistedState) {
        do {
            let data = try Vault.seal(state)
            let url = try stateEncryptedURL()
            try data.write(to: url, options: .atomic)
            let plain = try statePlainURL()
            if fileManager.fileExists(atPath: plain.path) {
                try? fileManager.removeItem(at: plain)
            }
        } catch {
            // Last resort: keep the app usable if the keychain is temporarily unavailable.
            do {
                let data = try JSONEncoder.ipvio.encode(state)
                try data.write(to: statePlainURL(), options: .atomic)
            } catch {
                print("[IPVIO] Could not save state: \(error)")
            }
        }
    }

    func loadCatalog(playlistID: UUID) -> [MediaItem] {
        do {
            let url = try catalogURL(playlistID)
            guard fileManager.fileExists(atPath: url.path) else { return [] }
            let data = try Data(contentsOf: url)
            return try JSONDecoder.ipvio.decode([MediaItem].self, from: data)
        } catch {
            print("[IPVIO] Could not read catalog \(playlistID): \(error)")
            return []
        }
    }

    func saveCatalog(playlistID: UUID, items: [MediaItem]) {
        do {
            let data = try JSONEncoder.ipvio.encode(items)
            try data.write(to: catalogURL(playlistID), options: .atomic)
        } catch {
            print("[IPVIO] Could not save catalog \(playlistID): \(error)")
        }
    }

    func deleteCatalog(playlistID: UUID) {
        if let url = try? catalogURL(playlistID) {
            try? fileManager.removeItem(at: url)
        }
    }

    func loadMetadataCache() -> [String: EnrichedMetadata] {
        guard let url = try? metadataURL(), fileManager.fileExists(atPath: url.path),
              let data = try? Data(contentsOf: url),
              let value = try? JSONDecoder.ipvio.decode([String: EnrichedMetadata].self, from: data) else {
            return [:]
        }
        return value
    }

    func saveMetadataCache(_ value: [String: EnrichedMetadata]) {
        guard let url = try? metadataURL(), let data = try? JSONEncoder.ipvio.encode(value) else { return }
        try? data.write(to: url, options: .atomic)
    }

    func clearMetadataCache() {
        if let url = try? metadataURL() {
            try? fileManager.removeItem(at: url)
        }
    }

    func clearAll() {
        if let root = try? rootURL() {
            try? fileManager.removeItem(at: root)
            try? fileManager.createDirectory(at: root, withIntermediateDirectories: true)
        }
    }
}

// MARK: - Errors

enum PlaylistError: LocalizedError {
    case invalidURL
    case http(Int)
    case notPlaylist
    case loginRejected
    case empty
    case decoding
    case network(String)
    case episodes
    case cancelled
    case unknown(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return L.t("error.invalidURL")
        case let .http(code): return L.f("error.http", code)
        case .notPlaylist: return L.t("error.notPlaylist")
        case .loginRejected: return L.t("error.loginRejected")
        case .empty: return L.t("error.empty")
        case .decoding: return L.t("error.decoding")
        case let .network(detail):
            let base = L.t("error.network")
            return detail.isEmpty ? base : base + " (" + detail + ")"
        case .episodes: return L.t("error.episodes")
        case .cancelled: return L.t("common.cancel")
        case let .unknown(detail): return detail.isEmpty ? L.t("error.unknown") : detail
        }
    }
}
