/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.adapter.innodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.junit.jupiter.api.*;

public class QueryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTest.class);
    static String url;

    @BeforeAll
    static void setup() {
        System.setProperty("calcite.debug", "true");
        String modelPath = Paths.get("src/test/resources/model2.json").toAbsolutePath().toString();
        url = "jdbc:calcite:model=" + modelPath + ";fun=spatial;";
    }

    private void executeAndPrint(String query) {
        System.out.println("==============================================");
        System.out.println("실행 쿼리: " + query);
        // LOGGER.info("Executing query: {}", query);
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // System.out.println("=== 컬럼 정보 ===");
                // for (int i = 1; i <= columnCount; i++) {
                //     String columnName = metaData.getColumnName(i);
                //     String columnTypeName = metaData.getColumnTypeName(i);
                //     int columnType = metaData.getColumnType(i);
                //     System.out.printf("[%d] %s (%s / JDBC 타입: %d)%n", i, columnName, columnTypeName, columnType);
                //     LOGGER.debug("Column info - name: {}, type: {}, jdbcType: {}", columnName, columnTypeName, columnType);
                // }

                System.out.println("\n=== 데이터 ===");
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        System.out.printf("%s = %s ", metaData.getColumnName(i), value);
                        LOGGER.debug("Data - column: {}, value: {}", metaData.getColumnName(i), value);
                    }
                    System.out.println();
                }
                if (!hasData) {
                    System.out.println("(결과 없음)");
                }
                System.out.println("쿼리 성공");
            }
        } catch (SQLException e) {
            System.out.println("쿼리 실패: " + e.getMessage());
            LOGGER.error("Error executing query", e);
            throw new RuntimeException("Query execution failed", e);
        }
    }

    @Test
    void testAsBinary() { executeAndPrint("SELECT ST_AsBinary(\"location\") FROM \"places\""); }
    @Test
    void testAsGeoJSON() { executeAndPrint("SELECT ST_AsGeoJSON(\"location\") FROM \"places\""); }
    @Test
    void testAsText() { executeAndPrint("SELECT ST_AsText(\"location\") FROM \"places\""); }
    @Test
    void testAsWKB() { executeAndPrint("SELECT ST_AsWKB(\"location\") FROM \"places\""); }
    @Test
    void testAsWKT() { executeAndPrint("SELECT ST_AsWKT(\"location\") FROM \"places\""); }
    @Test
    void testBuffer() { executeAndPrint("SELECT ST_Buffer(\"location\", 10) FROM \"places\""); }
    @Test
    void testContains() { executeAndPrint("SELECT ST_Contains(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testCrosses() { executeAndPrint("SELECT ST_Crosses(\"location\", ST_GeomFromText('LINESTRING(0 0,1 1)', 4326)) FROM \"places\""); }
    @Test
    void testDifference() { executeAndPrint("SELECT ST_Difference(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testDisjoint() { executeAndPrint("SELECT ST_Disjoint(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testDistance() { executeAndPrint("SELECT ST_Distance(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testExteriorRing() { executeAndPrint("SELECT ST_ExteriorRing(\"location\") FROM \"places\""); }
    @Test
    void testGeometryN() { executeAndPrint("SELECT ST_GeometryN(\"location\", 1) FROM \"places\""); }
    @Test
    void testGeomFromGeoJSON() { executeAndPrint("SELECT ST_GeomFromGeoJSON('{\"type\":\"Point\",\"coordinates\":[127,37]}')"); }
    @Test
    void testGeomFromText() { executeAndPrint("SELECT ST_GeomFromText('POINT(127 37)')"); }
    @Test
    void testGeomFromWKB() { executeAndPrint("SELECT ST_GeomFromWKB(ST_AsWKB(\"location\")) FROM \"places\""); }
    @Test
    void testIntersects() { executeAndPrint("SELECT ST_Intersects(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testIsEmpty() { executeAndPrint("SELECT ST_IsEmpty(\"location\") FROM \"places\""); }
    @Test
    void testIsValid() { executeAndPrint("SELECT ST_IsValid(\"location\") FROM \"places\""); }
    @Test
    void testLineFromText() { executeAndPrint("SELECT ST_LineFromText('LINESTRING(0 0,1 1)')"); }
    @Test
    void testMakeEnvelope() { executeAndPrint("SELECT ST_MakeEnvelope(0,0,1,1,4326)"); }
    @Test
    void testMLineFromText() { executeAndPrint("SELECT ST_MLineFromText('MULTILINESTRING((0 0,1 1))')"); }
    @Test
    void testMPointFromText() { executeAndPrint("SELECT ST_MPointFromText('MULTIPOINT((0 0),(1 1))')"); }
    // @Test
    // void testMPolyFromText() { executeAndPrint("SELECT ST_MPolyFromText('MULTIPOLYGON(((0 0,1 0,1 1,0 1,0 0)))')"); }
    @Test
    void testNumGeometries() { executeAndPrint("SELECT ST_NumGeometries(\"location\") FROM \"places\""); }
    @Test
    void testNumInteriorRing() { executeAndPrint("SELECT ST_NumInteriorRing(\"location\") FROM \"places\""); }
    @Test
    void testNumPoints() { executeAndPrint("SELECT ST_NumPoints(\"location\") FROM \"places\""); }
    // @Test
    // void testPointFromText() { executeAndPrint("SELECT ST_PointFromText('POINT(127 37)')"); }
    @Test
    void testPointFromWKB() { executeAndPrint("SELECT ST_PointFromWKB(ST_AsWKB(\"location\")) FROM \"places\""); }
    @Test
    void testTouches() { executeAndPrint("SELECT ST_Touches(\"location\", ST_GeomFromText('POINT(37 127)', 4326)) FROM \"places\""); }
    @Test
    void testTransform() { executeAndPrint("SELECT ST_Transform(\"location\", 4326) FROM \"places\""); }
    @Test
    void testX() { executeAndPrint("SELECT ST_X(\"location\") FROM \"places\""); }
    @Test
    void testY() { executeAndPrint("SELECT ST_Y(\"location\") FROM \"places\""); }
    @Test
    void testWhereNotNull() {
        executeAndPrint("SELECT ST_AsText(\"location\") FROM \"places\" WHERE \"location\" IS NOT NULL");
    }
    @Test
    void testWhereSpatialCondition() {
        executeAndPrint("SELECT ST_AsText(\"location\") FROM \"places\" WHERE ST_Within(\"location\", ST_GeomFromText('POINT(37 127)', 4326))");
    }

  @Test
  void testReducePrecision() { executeAndPrint("SELECT ST_AsText(ST_ReducePrecision(\"path\", 3)) FROM \"places\" WHERE \"id\" = 10"); }

  @Test
  void testReducePrecision2() { executeAndPrint("SELECT ST_ReducePrecision('LINESTRING(126.97 37.58, 126.975 37.585, 126.98 37.59)', 3)"); }

  @Test
  void testMinimumRectangle() {
    executeAndPrint("SELECT ST_AsText(ST_MinimumRectangle(\"path\")) FROM \"places\" WHERE \"id\" = 10");
  }

  @Test
  void testMinimumRectangle2() {
    executeAndPrint("SELECT ST_MinimumRectangle('LINESTRING(126.97 37.58, 126.975 37.585, 126.98 37.59)')");
  }

  @Test
  void testNPoints() {
    executeAndPrint("SELECT ST_NPoints(\"path\") FROM \"places\" WHERE \"id\" = 10");
  }

  @Test
  void testNPoints2() {
    executeAndPrint("SELECT ST_NPoints('LINESTRING(126.97 37.58, 126.975 37.585, 126.98 37.59)')");
  }

  @Test
  void testEnvelopesIntersect() {
    executeAndPrint("SELECT ST_EnvelopesIntersect(\"path\", ST_GeomFromText('LINESTRING(126.97 37.58, 126.98 37.59)')) FROM \"places\" WHERE \"id\" = 10");
  }

  @Test
  void testEnvelopesIntersect2() {
    executeAndPrint("SELECT ST_EnvelopesIntersect('LINESTRING(126.97 37.58, 126.975 37.585)', 'LINESTRING(126.98 37.59, 126.99 37.60)')");
  }
}
