# 🛠️ 트러블 슈팅: 식당 혼잡도 조회 시 N + 1 쿼리 문제 해결

## 1. 배경 및 문제 상황 (AS-IS)
* **상황**: 지도 화면에 표시되는 **45개의 식당 정보**와 각 식당의 **현재 혼잡도(Congestion) 데이터**를 함께 조회하여 클라이언트에 제공해야 함.
* **현상**: 식당 목록을 조회하는 API를 호출했을 때, 식당 목록을 조회하는 쿼리 **1번** 외에, 각 식당의 혼잡도 정보를 조회하기 위해 식당의 개수만큼(**45번**) 추가 쿼리가 발생하는 성능 저하가 발생함. (총 **46번**의 DB 조회 발생)
* **결과**: 다량의 DB 커넥션 점유 및 네트워크 I/O 증가로 인해 API 응답 속도가 급격히 느려짐.

---

## 2. 원인 분석
* **원인**: `Restaurant` 엔티티와 `Congestion` 엔티티는 `@OneToMany` (일대다) 관계로 매핑되어 있으며, 성능 최적화를 위해 지연 로딩(`FetchType.LAZY`)으로 설정되어 있음.
  ```java
  // Restaurant.java (개선 전)
  @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
  private List<Congestion> congestions = new ArrayList<>();
  ```
* **동작 방식**: 
  1. 먼저 식당 목록(45개)을 가져오는 메인 쿼리가 1회 실행됨.
  2. 프론트엔드 또는 API 직렬화 과정(Jackson Serializer 등)에서 각 식당 엔티티의 `congestions` 필드에 접근하는 시점에 프록시 객체가 초기화됨.
  3. 이때 식당마다 개별적으로 `SELECT * FROM congestion WHERE rest_idx = ?` 쿼리가 반복 실행되어 **N + 1 문제**가 발생함.

---

## 3. 해결 대안 및 기술적 의사결정 (Trade-off)

N + 1 문제를 해결하기 위해 대표적으로 아래 3가지 방법을 검토했습니다.

| 해결 대안 | 장점 | 단점 / 채택하지 않은 이유 |
| :--- | :--- | :--- |
| **1. Fetch Join** | 단 1번의 쿼리로 식당과 혼잡도를 즉시 로딩(`EAGER`)하여 가져옴. | • 페이징(`Pageable`) 쿼리와 함께 사용 시, Hibernate가 DB 레벨이 아닌 애플리케이션 메모리 상에서 페이징을 수행하므로 `OutOfMemoryError` 장애 위험이 있음.<br>• `Menu`, `Facilities` 등 다른 `@OneToMany` 연관 관계 컬렉션까지 한 번에 가져오려고 할 경우 `MultipleBagFetchException`이 발생함. |
| **2. EntityGraph** | 어노테이션 기반으로 간편하게 조인을 적용할 수 있음. | • Fetch Join과 마찬가지로 카테시안 곱(Cartesian Product) 문제 및 페이징 메모리 과부하 문제를 공유함. |
| **3. @BatchSize (채택)** | **연관된 컬렉션을 조회할 때 지정된 크기만큼 `IN` 절로 묶어서 나누어 조회함.** | • 쿼리가 1번이 아닌 묶음 단위만큼(예: 4.5회) 발생하지만, 카테시안 곱이 발생하지 않고 페이징 쿼리와 완벽히 호환되어 가장 안전함. |

### 💡 최종 결정: `@BatchSize(size = 10)` 적용
여러 개의 일대다 컬렉션(`menuList`, `facilities`, `congestions`)이 존재하고, 추후 식당 리스트에 대한 페이징 처리가 도입될 가능성이 높기 때문에, 가장 안전하고 메모리 효율적인 `@BatchSize` 방식을 채택했습니다.

---

## 4. 해결 내용 (TO-BE)
`Restaurant` 엔티티의 `congestions` 필드에 Hibernate의 `@BatchSize` 어노테이션을 적용하고 사이즈를 `10`으로 설정했습니다.

```java
// Restaurant.java (개선 후)
import org.hibernate.annotations.BatchSize;

@Entity
public class Restaurant {
    // ... 기존 코드
    
    @BatchSize(size = 10) // N+1 문제 해결을 위한 배치 사이즈 설정
    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Congestion> congestions = new ArrayList<>();
}
```

### 개선 후 쿼리 변화
* **개선 전 (46회)**:
  * `SELECT * FROM restaurant;` (1회)
  * `SELECT * FROM congestion WHERE rest_idx = 1;`
  * `SELECT * FROM congestion WHERE rest_idx = 2;`
  * ... (45회 반복)
* **개선 후 (6회)**:
  * `SELECT * FROM restaurant;` (1회)
  * `SELECT * FROM congestion WHERE rest_idx IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);` (1회)
  * `SELECT * FROM congestion WHERE rest_idx IN (11, 12, 13, 14, 15, 16, 17, 18, 19, 20);` (1회)
  * `SELECT * FROM congestion WHERE rest_idx IN (21, 22, 23, 24, 25, 26, 27, 28, 29, 30);` (1회)
  * `SELECT * FROM congestion WHERE rest_idx IN (31, 32, 33, 34, 35, 36, 37, 38, 39, 40);` (1회)
  * `SELECT * FROM congestion WHERE rest_idx IN (41, 42, 43, 44, 45);` (1회)

---

## 5. 결과 검증
* **쿼리 요청 수 감소**: **46회 ➡️ 6회** (약 **87% 감소**)
* **성능 및 안전성**:
  * 쿼리 수 감소로 인해 DB 커넥션 풀 경합이 완화되고 API 응답 시간이 단축됨.
  * 추후 `Pageable`을 통한 식당 페이징 조회를 도입하더라도 메모리 이슈 없이 안전하게 동작함.
