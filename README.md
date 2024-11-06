# QueueTicketingFlow

## 1. 아키텍쳐

![대기열 서비스 아키텍쳐](/assets/아키텍쳐1.png)
1. 사용자는 타겟 웹사이트에 우선적으로 접속
2. 허용이 된 상태가 아니라면 대기 페이지로 이동
3. 허용이 되면 다시 타겟 웹사이트로 이동
## 2. Sequence Diagram

### 1. 대기열 등록 API (Spring Webflux → Redis)


![대기열 등록 API](/assets/대기열_등록.png)

1. `key: userId`, `value: unixTimestamp`로 Redis SortedSet 등록
2. Wait Queue에 값이 있다면 false
3. Wait Queue에 값이 없다면 true
> ### Redis Sorted Set 특징
> > Redis Sorted Set은 효율적인 데이터 정렬과 조회가 필요한 곳에서 강력하게 사용할 수 있는 유용한 데이터 구조
> - Redis 데이터 구조 중 하나로, 각 요소가 고유한 점수를 가지며 이 점수를 기준으로 정렬되는 컬렉션
> - 정렬기능 : 각 요소는 삽입될 때 할당된 점수에 따라 자동으로 오름차순으로 정렬된다. 이 점수를 통해 정렬된 상태로 데이터를 관리할 수 있다.
> - 고유 값 : Sorted Set에서는 요소의 값이 고유해야 한다. 동일한 요소를 여러번 추가하면, 기존 요소의 점수만 갱신됨
> - 빠른 조회: 특정 점수 구간 내에 있는 요소드르을 조회하거나, 순위(rank)를 기준으로 조회하는데 최적화 되어있음


### 2. 진입 요청 API
![진입요청 API](/assets/진입요청.png)
1. Wait Queue에서 최소 허용인원 수만큼 pop(제거)
2. Proceed Queue에 등록(추가) 후
3. pop을 요청한 인원 수, 실제 pop한 인원 수를 리턴
    - ex) pop을 요청한 인원 5명, 실제 처리된 인원 3일 경우)
        - requestCount: 5
        - allowedCount: 3

진입 가능 여부 확인 API
1. Proceed Queue에 찾고자 하는 User의 랭크를 확인 후
2. 없다면 False, 있다면 True 반환

### 3. 접속 대기 후 Redirect
![접속 대기 후 Redirect](/assets/접속대기.png)
1. isAllowed() 메서드로 Proceed Queue에 해당 유저가 존재하는 지 확인
2. 존재한다면 해당 페이지로 Redirect
3. 존재하지 않는다면(Wait Queue에 있거나, Wait Queue에 존재하지 않는다면)
   1.  Wait Queue에서의 현재의 대기 순위와 함께 대기 페이지에서 대기

### 4. 대기열 이탈의 경우
#### 기존
![기존](/assets/대기열이탈_기존.png)
#### 변경 후
![변경](/assets/대기열이탈_변경.png)
(기존) 대기 웹페이지에서 계속 대기 시 코드를 통해 자동적으로 페이지 이동이 일어남


(변경) 페이지가 이동하는 과정에서 검증할 수 있는 토큰을 전달한다면?

- 기존 : proceed queue에 rank 메서드로 존재하는지 확인
   - 문제점: 사용자가 대기열에서 이탈하는 경우(사이트를 벗어나는 경우)에도 wait queue에 존재하기 때문에 이후 proceed queue로 이동이 가능. 하지만 해당 사용자는 이미 이탈했으므로 옮길 필요 없다.
- 변경 :
   - front단에서 waiting-room에 접근 후 wait queue에서 proceed queue로 진입하는 시점에 /touch API를 통해 token 생성 후 쿠키에 저장
   - 생성된 token을 검증하는 작업을 거침
