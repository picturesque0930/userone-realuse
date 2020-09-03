# 주제 - 회의실 시스템

회의실 사용을 위해 예약/취소하고 관리자가 승인/거절하는 시스템입니다.
예약 후 관리자 승인을 통해 확정되며, 회의실을 추가 관리하며 리스트를 볼 수 있습니다.
------




# 구현 Repository
https//github.com/picturesque/
1. https//github.com/picturesque/cna-booking
2. https//github.com/picturesque/cna-confirm
3. https//github.com/picturesque/cna-room (추가)
4. https://github.com/aimmvp/cna-gateway ( 수정 )
5. https://github.com/aimmvp/cna-bookinglist



# 서비스 시나리오

## 기능적 요구사항

1. 예약을 하고 확정을 받는다.
2. 회의실 관리를 하며 회의실 사용불가시 예약도 취소된다.(bookingCancel)
3. 회의실 예약 상태를 확인할 수 있다. (bookingList)

## 비기능적 요구사항
1. 트랜잭션
  - 예약을 하였을 경우 확정을 한다.(Sync 호출)
  
2. 장애격리
  - 룸관리와 리스트 상태가 불가하여도 예약과 승인 기능은 가능하다.
  - Circuit Breaker
  
3. 성능
  - 회의실 사용 예약상태를  확인 가능하다.(CQRS)
  - 
  
# 분석 설계
![설계 결과](https://user-images.githubusercontent.com/1927756/92066231-5d2c6500-eddc-11ea-9a0f-251279018d52.png)
1. 회의실을 예약한다 .()
2. 회의실 예약하면  확정 요청을 한다.(confirm) ```Saga Pattern```
3. 룸을 추가 관리하는데 기존 룸이 불가상태면 예약도 취소된다.

## 비 기능적 요구사항을 커버하는지 검증
1. 트랜잭션
  - 승인거절(confirmDenied) 되었을 경우 예약을 취소한다.(Sync 호출)
  
2. 장애격리
  - 룸관리와 리스트 상태가 불가하여도 예약과 승인 기능은 가능하다.
  - Circuit Breaker, fallback
  
3. 성능
  - 예약/승인 상태는 예약목록 시스템에서 확인 가능하다.(CQRS)
  - 
  
## 헥사고날 아키텍처 다이어그램 도출  
![핵사고날](https://user-images.githubusercontent.com/1927756/92069214-d11e3b80-ede3-11ea-9582-b343e06547e2.png)

- Inbound adaptor와 Outbound adaptor를 구분함
- 호출관계에서 PubSub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐

# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현함. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)
booking/  confirm/  gateway/  room/  bookinglist/

```
cd realuse
mvn spring-boot:run

cd reoomUseList
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언. 
  ```realuse```

```java
package userone;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Realuse_table")
public class Realuse {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookingId;
    private String realUseStartDtm;
    private String realUseEndDtm;
}
```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

```java
package userone;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface RealuseRepository extends PagingAndSortingRepository<Realuse, Long>{

}
```

- 적용 후 REST API 의 테스트
* [booking] 회의실 예약 내용 추가
```
http POST http://a31639437747a4661912515268e9f93c-815795959.ap-northeast-2.elb.amazonaws.com:8080/bookings roomId="121212" bookingUserId="8888" useStartDtm="202008240930" useEndDtm="202008241030“

```

* [realuse] 예약 확정 
```
http POST http://a31639437747a4661912515268e9f93c-815795959.ap-northeast-2.elb.amazonaws.com:8080/realuses bookingId="2" realUseStartDtm="20200902111111"
```

* [realuse] 룸 상태 변경
``` 
❯ http PATCH http://a31639437747a4661912515268e9f93c-815795959.ap-northeast-2.elb.amazonaws.com:8080/bookings/2 realUseEndDtm="123456"
```

* [booking] 회의실 예약정보 삭제 확인
```
❯ http http://a31639437747a4661912515268e9f93c-815795959.ap-northeast-2.elb.amazonaws.com:8080/booking/2
```


{
    "_links": {
        "roomUseList": {
            "href": "http://roomuselist:8080/roomUseLists/2"
        },
        "self": {
            "href": "http://roomuselist:8080/roomUseLists/2"
        }
    },
    "bookedUseEndDtm": "202008241030",
    "bookedUseStartDtm": "202008240930",
    "bookingId": 2,
    "bookingUserId": null,
    "realUseEndDtm": "111111",
    "realUseStartDtm": "202009051111",
    "roomId": 1
}
```
* [notification] 알림내용 확인
```json
{
    "_links": {
        "notification": {
            "href": "http://notification:8080/notifications/19"
        },
        "self": {
            "href": "http://notification:8080/notifications/19"
        }
    },
    "contents": "Booking Number[ null ] is use started",
    "sendDtm": "2020-09-03 04:24:32",
    "userId": "99999"
},
{
    "_links": {
        "notification": {
            "href": "http://notification:8080/notifications/20"
        },
        "self": {
            "href": "http://notification:8080/notifications/20"
        }
    },
    "contents": "Booking Number[ null ] is use started",
    "sendDtm": "2020-09-03 04:24:32",
    "userId": "99999"
}
```
## 동기식 호출 과 비동기식 

분석단계에서의 조건 중 하나로 사용종료(useEnded)->회의실 예약 취소(bookingCancel) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 

### 동기식 호출(FeignClient 사용)
```java
// realuse/../externnal/BookingService.java

// feign client 로 booking method 호출
// URL 은 application.yml 정의함(api.url.booking)
//@FeignClient(name="booking", url="http://booking:8080")
@FeignClient(name="booking", url="${api.url.booking}")
public interface BookingService {

    // Booking Cancel 을 위한 삭제 mapping
    @DeleteMapping(value = "/bookings/{id}")
    public void bookingCancel(@PathVariable long id);
}

// realuse/../Realuse.java
    @PostUpdate
    public void onPostUpdate(){
        UseEnded useEnded = new UseEnded();
        BeanUtils.copyProperties(this, useEnded);
        useEnded.publishAfterCommit();

        RealuseApplication.applicationContext.getBean(userone.external.BookingService.class).bookingCancel(this.getBookingId());
    }

```
### 비동기식 호출(Kafka Message 사용)
* Publish
```java
// realuse/../Realuse.java
   @PostPersist
    public void onPostPersist(){

        UseStarted useStarted = new UseStarted();
        BeanUtils.copyProperties(this, useStarted);
        useStarted.publishAfterCommit();
    }
```
* Subscribe
```java
// notification/../PolicyHandler.java
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUseStarted_SendNotification(@Payload UseStarted useStarted) {
        if(useStarted.isMe()) {
            Notification notification = new Notification();
            notification.setUserId("99999");
            notification.setContents("Booking Number[ " + String.valueOf(useStarted.getBookingId()) + " ] is use started");
            String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            notification.setSendDtm(nowDate);
            notificationRepository.save(notification);
            System.out.println("##### listener SendNotification : " + useStarted.toJson());
        }
    }
```

## Gateway 적용
각 서비스는 ClusterIP 로 선언하여 외부로 노출되지 않고, Gateway 서비스 만을 LoadBalancer 타입으로 선언하여 Gateway 서비스를 통해서만 접근할 수 있다.
```yml
## gateway/../resources/application.yml
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: booking
          uri: http://booking:8080
          predicates:
            - Path=/bookings/**
        - id: confirm
          uri: http://confirm:8080
          predicates:
            - Path=/confirms/** 
        - id: notification
          uri: http://notification:8080
          predicates:
            - Path=/notifications/** 
        - id: realuse
          uri: http://realuse:8080
          predicates:
            - Path=/realuses/** 
        - id: roomUseList
          uri: http://roomuselist:8080
          predicates:
            - Path= /roomUseLists/***
```

```yml
## gateway/../kubernetes/service.yml

apiVersion: v1
kind: Service
metadata:
  name: gateway
  labels:
    app: gateway
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway
  type:
    LoadBalancer
```
# 운영
## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS CodeBuild를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.
![CI/CD Pipeline](https://user-images.githubusercontent.com/3872380/91843678-1bdb6e80-ec91-11ea-87ac-dc2e90b24798.png)
1. 변경된 소스 코드를 GitHub에 push
2. CodeBuild에서 webhook으로 GitHub의 push 이벤트를 감지하고 build, test 수행
3. Docker image를 생성하여 ECR에 push
4. Kubernetes(EKS)에 도커 이미지 배포 요청
5. ECR에서 도커 이미지 pull

[ 구현 사항]
 * CodeBuild에 EKS 권한 추가
 ```json
         {
            "Action": [
                "ecr:BatchCheckLayerAvailability",
                "ecr:CompleteLayerUpload",
                "ecr:GetAuthorizationToken",
                "ecr:InitiateLayerUpload",
                "ecr:PutImage",
                "ecr:UploadLayerPart",
                "eks:DescribeCluster"
            ],
            "Resource": "*",
            "Effect": "Allow"
        }
 ```
  * EKS 역할에 CodeBuild 서비스 추가하는 내용을 EKS 의 ConfigMap 적용
```yaml
## aws-auth.yml
apiVersion: v1
data:
  mapRoles: |
    - groups:
      - system:bootstrappers
      - system:nodes
      rolearn: arn:aws:iam::496278789073:role/eksctl-skccuser01-nodegroup-stand-NodeInstanceRole-16HOEVRDHSYBQ
      username: system:node:{{EC2PrivateDNSName}}
    - rolearn: arn:aws:iam::496278789073:role/CodeBuildSkccUser01
      username: CodeBuildSkccUser01
      groups:
        -system:masters
  mapUsers: |
    []
kind: ConfigMap
metadata:
  creationTimestamp: "2020-09-02T05:27:35Z"
  name: aws-auth
  namespace: kube-system
  resourceVersion: "857"
  selfLink: /api/v1/namespaces/kube-system/configmaps/aws-auth
  uid: 3ff6ee71-2cb1-4ab4-b6ef-fb5e5d020d6c
```
```shell
kubectl apply -f aws-auth.yml --force
```
  * buildspec.yml
  ```yaml
  version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto8 # Amazon Corretto 8 - production-ready distribution of the OpenJDK
      docker: 18
    commands:
      - curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.15.11/2020-07-08/bin/darwin/amd64/kubectl # Download kubectl 
      - chmod +x ./kubectl
      - mkdir ~/.kube
      - aws eks --region $AWS_DEFAULT_REGION update-kubeconfig --name skccuser01 # Set cluster skccusesr01 as default cluster
  pre_build:
    commands:
      - echo Region = $AWS_DEFAULT_REGION # Check Environment Variables
      - echo Account ID = $AWS_ACCOUNT_ID # Check Environment Variables
      - echo ECR Repo = $IMAGE_REPO_NAME # Check Environment Variables
      - echo Docker Image Tag = $IMAGE_TAG # Check Environment Variables
      - echo Logging in to Amazon ECR...
      - $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION) # Login ECR
  build:
    commands:
      - echo Build started on `date`
      - echo Building the Docker image...
      - mvn clean
      - mvn package -Dmaven.test.skip=true # Build maven
      - docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG . # Build docker image
  post_build:
    commands:
      - echo Build completed on `date`
      - echo Pushing the Docker image...
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG # Push docker image to ECR
      - echo Deploy service into EKS
      - kubectl apply -f ./kubernetes/deployment.yml # Deploy
      - kubectl apply -f ./kubernetes/service.yml # Service

cache:
  paths:
    - '/root/.m2/**/*'
  ```
## CodeBuild 를 통한 CI/CD 동작 결과

아래 이미지는 aws pipeline에 각각의 서비스들을 올려, 코드가 업데이트 될때마다 자동으로 빌드/배포 하도록 하였다.
![CodeBuild 결과](https://user-images.githubusercontent.com/1927756/92070124-370bc280-ede6-11ea-8870-503a2a7a6dd6.png)
![K8S 결과](https://user-images.githubusercontent.com/1927756/92070188-5acf0880-ede6-11ea-82c8-ee8dc76cd113.png)

## Service Mesh
###  realuse service 에 istio 적용
 ```sh
 kubectl get deploy realuse -o yaml > realuse_deploy.yaml
 kubectl apply -f <(istioctl kube-inject -f realuse_deploy.yaml)
 ```
### confirm 에 Circuit Break 적용
```sh
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: realuse
spec:
  host: realuse
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 2
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
    outlierDetection:
      consecutiveErrors: 5
      interval: 1s
      baseEjectionTime: 30s
      maxEjectionPercent: 100
EOF
```
## HPA 적용
``` yaml
# hpa.yml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: realuse
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: realuse
  minReplicas: 1
  maxReplicas: 10
  targetCPUUtilizationPercentage: 50
```
![HPA 적용결과](https://user-images.githubusercontent.com/1927756/92070884-3bd17600-ede8-11ea-9e83-ee9f8da82fa2.png)

## Siege 테스트
```sh
siege -c2 -t60S  -v --content-type "application/json" 'http://a589eefdfb1ed468b8e8889aedebfc94-1575177534.ap-southeast-2.elb.amazonaws.com:8080/realuses POST {"status":"READY"}'
```
```
`Lifting the server siege...
Transactions:                      7 hits
Availability:                   3.03 %
Elapsed time:                  59.37 secs
Data transferred:               0.04 MB
Response time:                  8.67 secs
Transaction rate:               0.12 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                    1.02
Successful transactions:           7
Failed transactions:             224
Longest transaction:            3.67
Shortest transaction:           0.22
```
### 테스트 중 부하가 올라가면서 REPLICA 가 10개까지 올라가면서 부하가 줄어 들었다.

```
❯ kubectl get hpa -w
NAME      REFERENCE            TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
realuse   Deployment/realuse   <unknown>/50%   1         10        1          48m
realuse   Deployment/realuse   <unknown>/50%   1         10        1          58m



realuse   Deployment/realuse   250%/50%        1         10        1          59m
realuse   Deployment/realuse   250%/50%        1         10        4          59m
realuse   Deployment/realuse   250%/50%        1         10        5          59m
realuse   Deployment/realuse   218%/50%        1         10        5          60m
realuse   Deployment/realuse   218%/50%        1         10        10         60m
realuse   Deployment/realuse   133%/50%        1         10        10         61m
realuse   Deployment/realuse   4%/50%          1         10        10         62m
realuse   Deployment/realuse   3%/50%          1         10        10         64m
realuse   Deployment/realuse   4%/50%          1         10        10         65m
```
### Pod 종료 / 생성 됨
```
realuse-66d8b664d4-hbhtt       0/1     Pending   0          0s
realuse-66d8b664d4-hbhtt       0/1     Pending   0          0s
realuse-66d8b664d4-hbhtt       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-hbhtt       1/1     Running             0          4s
realuse-5f954bb98b-4btlt       1/1     Terminating         0          24m
realuse-5f954bb98b-4btlt       0/1     Terminating         0          25m
realuse-5f954bb98b-4btlt       0/1     Terminating         0          25m
realuse-5f954bb98b-4btlt       0/1     Terminating         0          25m


realuse-66d8b664d4-588md       0/1     Pending             0          0s
realuse-66d8b664d4-588md       0/1     Pending             0          0s
realuse-66d8b664d4-zmxrk       0/1     Pending             0          0s
realuse-66d8b664d4-kkjb2       0/1     Pending             0          0s
realuse-66d8b664d4-zmxrk       0/1     Pending             0          0s
realuse-66d8b664d4-kkjb2       0/1     Pending             0          0s
realuse-66d8b664d4-588md       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-kkjb2       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-zmxrk       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-zmxrk       1/1     Running             0          1s
realuse-66d8b664d4-588md       1/1     Running             0          2s
realuse-66d8b664d4-kkjb2       1/1     Running             0          2s
realuse-66d8b664d4-8rltq       0/1     Pending             0          0s
realuse-66d8b664d4-8rltq       0/1     Pending             0          0s
realuse-66d8b664d4-8rltq       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-8rltq       1/1     Running             0          2s
realuse-66d8b664d4-kd584       0/1     Pending             0          0s
realuse-66d8b664d4-c4rj4       0/1     Pending             0          0s
realuse-66d8b664d4-6h6np       0/1     Pending             0          0s
realuse-66d8b664d4-kd584       0/1     Pending             0          0s
realuse-66d8b664d4-c4rj4       0/1     Pending             0          0s
realuse-66d8b664d4-6h6np       0/1     Pending             0          0s
realuse-66d8b664d4-lrgqc       0/1     Pending             0          0s
realuse-66d8b664d4-j9wq9       0/1     Pending             0          0s
realuse-66d8b664d4-kd584       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-lrgqc       0/1     Pending             0          0s
realuse-66d8b664d4-j9wq9       0/1     Pending             0          0s
realuse-66d8b664d4-c4rj4       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-lrgqc       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-6h6np       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-j9wq9       0/1     ContainerCreating   0          0s
realuse-66d8b664d4-c4rj4       1/1     Running             0          2s
realuse-66d8b664d4-kd584       1/1     Running             0          2s
realuse-66d8b664d4-lrgqc       1/1     Running             0          2s
realuse-66d8b664d4-j9wq9       1/1     Running             0          3s
realuse-66d8b664d4-6h6np       1/1     Running             0          3s
```


## Readiness, Liveness 적용
```yaml
  readinessProbe:
    httpGet:
      path: '/actuator/health'
      port: 8080
    initialDelaySeconds: 10
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 10
  livenessProbe:
    httpGet:
      path: '/actuator/health'
      port: 8080
    initialDelaySeconds: 120
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 
```

### pod 구동 중 deploy 실행으 pod 재시작 됨 
```
kubectl apply -f deployment.yml
❯ k get pod -w
NAME                           READY   STATUS    RESTARTS   AGE
booking-5b64c6cdd8-89l8r       1/1     Running   0          3h48m
confirm-6cb69f5645-stnxl       1/1     Running   0          3h43m
gateway-6459546bf7-p85fn       1/1     Running   0          3h9m
notification-9bfcfc4d8-mkdx7   1/1     Running   0          129m
realuse-8499449fc7-t574h       2/2     Running   2          34m
roomuselist-986d4bb99-rsvcm    1/1     Running   0          3h32m
realuse-54b997bf56-mc2jj       0/1     Pending   0          0s
realuse-54b997bf56-mc2jj       0/1     Pending   0          0s
realuse-54b997bf56-mc2jj       0/1     ContainerCreating   0          0s
realuse-54b997bf56-mc2jj       0/1     Running             0          3s
```
