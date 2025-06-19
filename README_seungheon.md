# Apache Calcite 공간 함수 푸시다운 테스트 (Seungheon)

## 1. 프로젝트 개요

이 프로젝트는 Apache Calcite에서 **공간 함수(ST_*)를 MySQL 등 외부 DB로 푸시다운**하는 기능을 실험/확장한 코드입니다.
- Calcite의 JDBC Adapter를 확장하여, 공간 함수가 포함된 쿼리를 DB로 푸시다운합니다.
- MySQL의 GEOMETRY 타입을 올바르게 인식하도록 타입 매핑을 보정합니다.
- 실제 공간 연산(ST_AsText, ST_Contains 등)이 DB에서 실행되는지 테스트합니다.

---

## 2. 환경 및 요구사항

- **JDK 8, 11, 17, 21, 23 중 하나**
- **Gradle** (권장: 프로젝트 내 `./gradlew` 사용)
- **MySQL 5.7 이상** (공간 타입 지원)
- (테스트용) MySQL에 공간 데이터가 들어있는 테이블 필요

---

## 3. 빌드 및 테스트 실행 방법

### 3.1. 소스코드 클론 및 빌드

```bash
git clone https://github.com/seungheon123/calcite
cd calcite
./gradlew build
```

### 3.2. 테스트 실행

```bash
./gradlew :innodb:test
```

### 3.3. IDE에서 실행

- IntelliJ IDEA, Eclipse 등에서 `innodb/src/test/java/org/apache/calcite/adapter/innodb/QueryTest.java`를 직접 실행할 수 있습니다.

---
## 4. 데이터베이스 모델 및 샘플 데이터 준비

### 4.1. 테이블 생성 예시 (MySQL)

```sql
CREATE TABLE places (
  id INT PRIMARY KEY,
  name VARCHAR(255),
  location GEOMETRY NOT NULL,
  path LINESTRING,
  x DOUBLE,
  y DOUBLE
) ENGINE=InnoDB;
```

- `location`: POINT 또는 기타 GEOMETRY 타입 (SRID 4326 권장)
- `path`: LINESTRING 등 공간 경로 데이터
- `x`, `y`: 위경도 좌표값(옵션)

### 4.2. 샘플 데이터 삽입 예시

```sql
INSERT INTO places (id, name, location, path, x, y) VALUES
  (10211, '샘플장소 10211', ST_SRID(POINT(127.078178, 37.434954), 4326),
   ST_SRID(ST_GeomFromText('LINESTRING(127.078178 37.434954, 127.078678 37.435454, 127.079178 37.435954)'), 4326),
   127.078178, 37.434954);
```

- 여러 행을 추가하려면 위 INSERT 문을 반복하거나, VALUES에 여러 행을 추가하세요.
- 공간 타입 컬럼은 반드시 `ST_SRID`로 SRID(예: 4326)를 명시하는 것이 좋습니다.

---
## 5. 주요 코드/구성 설명

### 5.1. 공간 함수 푸시다운 관련 주요 수정

- **core/src/main/java/org/apache/calcite/adapter/jdbc/JdbcSchema.java**
    - MySQL의 GEOMETRY 컬럼을 Calcite 내부에서 GEOMETRY 타입으로 인식하도록 보정
    - (예시)
      ```java
      if ((dataType == Types.BINARY || dataType == Types.VARCHAR) && (
          typeString.equalsIgnoreCase("GEOMETRY") || ... )) {
        return typeFactory.createSqlType(SqlTypeName.GEOMETRY);
      }
      ```

- **core/src/main/java/org/apache/calcite/sql/dialect/MysqlSqlDialect.java**
    - MySQL에서 지원하는 공간 함수 목록 정의 및 지원 여부 판단
    - 공간 함수가 포함된 쿼리의 푸시다운 가능성 판단

- **core/src/main/java/org/apache/calcite/adapter/jdbc/JdbcRules.java**
    - `JdbcSpatialRule`: Project에 공간 함수가 있으면 무조건 JDBC로 푸시다운
    - `JdbcFilterRule`: Filter(WHERE)에 공간 함수가 있으면 JDBC로 푸시다운

### 5.2. 테스트 코드

- **innodb/src/test/java/org/apache/calcite/adapter/innodb/QueryTest.java**
    - 다양한 공간 함수 쿼리를 실행하여, 실제로 DB에서 공간 연산이 수행되는지 확인
    - 예시:
      ```java
      @Test
      void testAsText() { executeAndPrint("SELECT ST_AsText(\"location\") FROM \"places\""); }
      @Test
      void testContains() { executeAndPrint("SELECT ST_Contains(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
      ```

## 6. 실행 결과 예시

테스트를 실행하면, 각 쿼리별로 아래와 같이 결과가 출력됩니다.

```
==============================================
실행 쿼리: SELECT ST_AsText("location") FROM "places"

=== 데이터 ===
EXPR$0 = POINT(127 37)
EXPR$0 = POINT(128 38)
...
쿼리 성공
```

- 쿼리 실행 전/후 논리 계획, 물리 계획, 실제 결과를 로그/콘솔에서 확인할 수 있습니다.
- (DEBUG 로그가 너무 많을 경우, logback/log4j 설정을 통해 로그 레벨을 조정하세요.)

---

## 7. 참고/문제 해결

- **MySQL에 공간 데이터가 없으면 결과가 비어있을 수 있습니다.**
- **GEOMETRY 타입이 올바르게 인식되지 않으면, JdbcSchema.java의 타입 매핑 코드를 확인하세요.**
- **공간 함수가 푸시다운되지 않으면, MysqlSqlDialect.java의 supportsSpatialFunction 구현을 확인하세요.**
- **테스트가 실행되지 않으면, JDK/Gradle 환경, 의존성, DB 연결 정보를 점검하세요.**

---

## 8. 주요 참고 파일

- `core/src/main/java/org/apache/calcite/adapter/jdbc/JdbcSchema.java`
- `core/src/main/java/org/apache/calcite/sql/dialect/MysqlSqlDialect.java`
- `core/src/main/java/org/apache/calcite/adapter/jdbc/JdbcRules.java`
- `innodb/src/test/java/org/apache/calcite/adapter/innodb/QueryTest.java`

---
