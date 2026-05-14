# IdleGame API

Android（Kotlin）製放置ゲームアプリと連携するためのローカル用 Spring Boot REST API サーバー。

## 技術スタック

| 項目 | 内容 |
|------|------|
| フレームワーク | Spring Boot 3.2.5 |
| 言語 | Kotlin 1.9.23 |
| Java | 17 |
| DB | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate 6 |
| 認証 | JWT (JJWT 0.12.6) |
| コンテナ | Docker / Docker Compose |

## プロジェクト構成

```
IdleGameApi/
├── src/main/kotlin/com/example/idlegameapi/
│   ├── config/          # SecurityConfig (CORS + JWT フィルター登録)
│   ├── controller/      # REST エンドポイント
│   ├── dto/
│   │   ├── request/     # リクエストDTO + バリデーション
│   │   └── response/    # レスポンスDTO
│   ├── entity/          # JPA エンティティ
│   ├── exception/       # カスタム例外 + グローバルハンドラー
│   ├── repository/      # Spring Data JPA リポジトリ
│   ├── security/        # JWT サービス + フィルター + UserDetailsService
│   └── service/         # ビジネスロジック
├── src/main/resources/
│   └── application.yml
├── docker-compose.yml
└── build.gradle.kts
```

## 起動手順

### 1. 前提条件

- JDK 17 以上
- Docker Desktop
- Gradle（Wrapper同梱のため不要）

### 2. PostgreSQL を Docker で起動

```bash
# IdleGameApi/ ディレクトリで実行
docker compose up -d

# 起動確認
docker compose ps
```

### 3. Spring Boot アプリを起動

```bash
# Windows (PowerShell)
.\gradlew.bat bootRun

# Mac / Linux
./gradlew bootRun
```

アプリが起動すると `http://localhost:8080` でアクセス可能になります。

### 4. 停止

```bash
# アプリ: Ctrl+C で停止
# DB コンテナ
docker compose down
```

---

## API エンドポイント一覧

### 認証（認証不要）

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/auth/register` | ユーザー登録 |
| POST | `/api/auth/login` | ログイン |

### ユーザー（Bearer トークン必須）

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/users/{id}` | ユーザー情報取得 |

### ゲームデータ（Bearer トークン必須）

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/game/state` | ゲーム状態取得 |
| POST | `/api/game/save` | ゲーム状態保存 |

### スコア（Bearer トークン必須）

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/score/update` | スコア更新 |

---

## リクエスト/レスポンス例

### POST /api/auth/register

**Request:**
```json
{
  "username": "player1",
  "email": "player1@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "player1"
  }
}
```

### POST /api/game/save

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "stage": 150,
  "coins": 500000,
  "gems": 120,
  "totalAttack": 9800,
  "weaponSlots": 8,
  "energy": 7
}
```

### POST /api/score/update

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "totalKills": 15000,
  "maxStageReached": 150,
  "totalCoinsEarned": 9999999
}
```

---

## Android エミュレーターからの接続

Android エミュレーター内から PC の localhost にアクセスするには `10.0.2.2` を使用します。

**Retrofit の baseUrl:**
```kotlin
// AndroidアプリのRetrofit設定
private const val BASE_URL = "http://10.0.2.2:8080/"
```

CORS は `10.0.2.2` と `localhost` を許可済みです。

---

## DB テーブル構成

```sql
-- users テーブル
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR      NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

-- game_state テーブル
CREATE TABLE game_state (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT UNIQUE NOT NULL REFERENCES users(id),
    stage         INTEGER NOT NULL DEFAULT 1,
    coins         BIGINT  NOT NULL DEFAULT 0,
    gems          INTEGER NOT NULL DEFAULT 0,
    total_attack  BIGINT  NOT NULL DEFAULT 1,
    weapon_slots  INTEGER NOT NULL DEFAULT 5,
    energy        INTEGER NOT NULL DEFAULT 10,
    last_saved_at TIMESTAMP NOT NULL
);

-- score テーブル
CREATE TABLE score (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT UNIQUE NOT NULL REFERENCES users(id),
    total_kills         BIGINT  NOT NULL DEFAULT 0,
    max_stage_reached   INTEGER NOT NULL DEFAULT 1,
    total_coins_earned  BIGINT  NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL
);
```

> `ddl-auto: update` を設定しているので、テーブルは初回起動時に自動作成されます。

---

## 本番環境（VPS）への移行

1. **JWT シークレットを環境変数で設定**
   ```bash
   export JWT_SECRET="本番用の長くランダムな文字列"
   ```
   `application.yml` の `app.jwt.secret` を `${JWT_SECRET}` に変更してください。

2. **application.yml の DB 接続先を本番 DB に変更**

3. **`ddl-auto` を `validate` または `none` に変更**（本番では Flyway/Liquibase 推奨）

4. **Docker Compose で一括起動する場合**
   ```yaml
   # docker-compose.yml に app サービスを追加してください
   app:
     build: .
     ports:
       - "8080:8080"
     depends_on:
       postgres:
         condition: service_healthy
   ```

---

## 開発環境確認コマンド

```bash
# DB 接続確認
docker exec -it idlegame_postgres psql -U idlegame_user -d idlegame_db

# テーブル一覧
\dt

# ユーザー確認
SELECT id, username, email FROM users;
```
