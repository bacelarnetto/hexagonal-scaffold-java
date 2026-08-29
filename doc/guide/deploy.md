# Deploy — Docker e Kubernetes (GCP)

```
docker/Dockerfile     Build multi-stage self-contido -- so precisa do Docker instalado.
kubernetes/           Deployment + Service + ConfigMap + Secret (template), pensado para GKE.
```

Docker (local ou qualquer container runtime) e Kubernetes (GCP/GKE) resolvem problemas
diferentes: `docker-compose.yml` (gerado pelo skill de bootstrap, ver
[Skill de bootstrap](bootstrap)) é para desenvolvimento local — sobe o banco pra você rodar a app
pela IDE ou `mvn spring-boot:run`. `docker/` e `kubernetes/` aqui são para *empacotar e rodar a
própria aplicação* como container, culminando em GKE.

## Build da imagem

```bash
# a partir da raiz do repo -- o build precisa enxergar os 4 modulos Maven
docker build -f docker/Dockerfile -t REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/scaffold:TAG .
docker push REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/scaffold:TAG
```

O `Dockerfile` é multi-stage: primeiro estágio builda com Maven (`maven:3.9-eclipse-temurin-21`),
segundo estágio só copia o jar final para uma imagem `eclipse-temurin:21-jre-alpine` (JRE, não JDK
completo) e roda como usuário não-root. Não precisa rodar `mvn package` antes — o build inteiro
acontece dentro do container, então funciona direto num pipeline de CI que só tenha Docker.

## Por que não existe um `kubernetes/db/` com o banco dentro do cluster

Diferente de outros exemplos de referência hexagonal, que costumam rodar o Postgres/MySQL como
`Deployment` + `PersistentVolumeClaim` dentro do próprio Kubernetes, este scaffold **não** inclui
isso de propósito: banco stateful dentro de K8s sem operador dedicado significa sem backup
gerenciado, sem alta disponibilidade real, e PVC administrado manualmente — não é algo que se
recomenda pra produção. No GCP, o caminho correto é um banco gerenciado (Cloud SQL) fora do
cluster, acessado via **Cloud SQL Auth Proxy** como sidecar no mesmo Pod da aplicação.

## Padrão recomendado: Cloud SQL Auth Proxy como sidecar

Com o proxy rodando como um segundo container no mesmo Pod, a aplicação continua falando com
`127.0.0.1:3306` (é literalmente o que já está em `kubernetes/configmap.yaml`) — o proxy é quem
faz o túnel autenticado até a instância gerenciada. Requer
[Workload Identity](https://cloud.google.com/sql/docs/mysql/connect-instance-kubernetes) (uma
service account do GCP com a role `roles/cloudsql.client`, vinculada à service account do
Kubernetes usada pelo Deployment).

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scaffold
spec:
  replicas: 2
  selector:
    matchLabels:
      app: scaffold
  template:
    metadata:
      labels:
        app: scaffold
    spec:
      serviceAccountName: scaffold-ksa # vinculada via Workload Identity a uma SA do GCP com roles/cloudsql.client
      containers:
        - name: scaffold
          image: REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/scaffold:TAG
          ports:
            - containerPort: 8080
          env:
            - name: DB_HOST
              valueFrom:
                configMapKeyRef: { name: scaffold-config, key: db_host }
            - name: DB_PORT
              valueFrom:
                configMapKeyRef: { name: scaffold-config, key: db_port }
            - name: DB_NAME
              valueFrom:
                configMapKeyRef: { name: scaffold-config, key: db_name }
            - name: DB_USER
              valueFrom:
                secretKeyRef: { name: scaffold-db-secret, key: db_user }
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef: { name: scaffold-db-secret, key: db_password }
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            initialDelaySeconds: 15
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 30
        - name: cloud-sql-proxy
          image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2
          args:
            - "--structured-logs"
            - "--port=3306"
            - "PROJECT_ID:REGION:INSTANCE_CONNECTION_NAME"
          securityContext:
            runAsNonRoot: true
```

Isso substitui o `kubernetes/deployment.yaml` do scaffold (que não tem o sidecar, pra funcionar
como exemplo mínimo primeiro) uma vez que a instância Cloud SQL já exista.

## Aplicando no cluster

```bash
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/secret.yaml    # copiado de secret.example.yaml com valores reais -- nunca commitado
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml

kubectl get svc scaffold-svc   # espera o LoadBalancer do GCP atribuir um IP externo
```

## Probes: por que `spring-boot-starter-actuator`

`kubernetes/deployment.yaml` aponta `readinessProbe`/`livenessProbe` para
`/actuator/health/readiness` e `/actuator/health/liveness`. Esses dois endpoints só existem porque
`starter/pom.xml` depende de `spring-boot-starter-actuator` e
`management.endpoint.health.probes.enabled=true` está em `application.yml` — sem isso, só
haveria `/actuator/health` genérico (não distingue "container vivo mas ainda inicializando" de
"pronto pra receber tráfego", que é exatamente o que o Kubernetes precisa saber pra não rotear
tráfego pra um Pod que ainda está subindo).
