# PromQL Queries - DevOps Assignment

Bu fayl dasturdagi barcha metrikalar uchun PromQL so'rovlarini o'z ichiga oladi. Bu so'rovlarni Prometheus UI yoki Grafana dashboardlarda ishlatishingiz mumkin.

## 1. Custom Application Metrikalar

### Application Uptime
```promql
# Dastur ishlash vaqti (sekundlarda)
application_uptime_seconds

# Dastur ishlash vaqti (soatlarda)
application_uptime_seconds / 3600

# Dastur ishlash vaqti (kunlarda)
application_uptime_seconds / 86400
```

### Active Sessions
```promql
# Faol sessiyalar soni
application_active_sessions{type="http"}

# Faol sessiyalar o'rtacha qiymati (5 daqiqa)
avg_over_time(application_active_sessions{type="http"}[5m])

# Faol sessiyalar maksimal qiymati (1 soat)
max_over_time(application_active_sessions{type="http"}[1h])
```

### Cache Hit Ratio
```promql
# Cache hit ratio (foiz)
application_cache_hit_ratio{cache="main"} * 100

# Cache hit ratio o'rtacha qiymati (10 daqiqa)
avg_over_time(application_cache_hit_ratio{cache="main"}[10m]) * 100

# Cache hit ratio past bo'lsa (alert uchun)
application_cache_hit_ratio{cache="main"} < 0.7
```

## 2. User API Metrikalar

### User Requests Counter
```promql
# Jami user API so'rovlari soni
users_requests_total{api="users"}

# So'nggi 5 daqiqada user API so'rovlari soni
increase(users_requests_total{api="users"}[5m])

# So'nggi 1 soatda user API so'rovlari soni
increase(users_requests_total{api="users"}[1h])

# Daqiqada user API so'rovlari soni (rate)
rate(users_requests_total{api="users"}[5m]) * 60

# Soniyada user API so'rovlari soni
rate(users_requests_total{api="users"}[1m])
```

### User Created Counter
```promql
# Jami yaratilgan userlar soni
users_created_total{operation="create"}

# So'nggi 5 daqiqada yaratilgan userlar soni
increase(users_created_total{operation="create"}[5m])

# So'nggi 1 soatda yaratilgan userlar soni
increase(users_created_total{operation="create"}[1h])

# Daqiqada yaratilgan userlar soni
rate(users_created_total{operation="create"}[5m]) * 60
```

### User Errors Counter
```promql
# Jami user API xatolari soni
users_errors_total{api="users"}

# So'nggi 5 daqiqada user API xatolari soni
increase(users_errors_total{api="users"}[5m])

# So'nggi 1 soatda user API xatolari soni
increase(users_errors_total{api="users"}[1h])

# Xatolar foizi (jami so'rovlarga nisbatan)
(users_errors_total{api="users"} / users_requests_total{api="users"}) * 100

# Xatolar rate (daqiqada)
rate(users_errors_total{api="users"}[5m]) * 60
```

### User API Response Time (Timed Metrics)
```promql
# Barcha userlarni olish - o'rtacha vaqt
users_get_all_seconds_count

# Barcha userlarni olish - jami vaqt
users_get_all_seconds_sum

# Barcha userlarni olish - o'rtacha response time
rate(users_get_all_seconds_sum[5m]) / rate(users_get_all_seconds_count[5m])

# ID bo'yicha userni olish - o'rtacha vaqt
users_get_byid_seconds_count

# ID bo'yicha userni olish - o'rtacha response time
rate(users_get_byid_seconds_sum[5m]) / rate(users_get_byid_seconds_count[5m])

# User yaratish - o'rtacha vaqt
users_create_seconds_count

# User yaratish - o'rtacha response time
rate(users_create_seconds_sum[5m]) / rate(users_create_seconds_count[5m])

# Barcha user API endpointlarining o'rtacha response time
(
  rate(users_get_all_seconds_sum[5m]) / rate(users_get_all_seconds_count[5m]) +
  rate(users_get_byid_seconds_sum[5m]) / rate(users_get_byid_seconds_count[5m]) +
  rate(users_create_seconds_sum[5m]) / rate(users_create_seconds_count[5m])
) / 3
```

## 3. HTTP Request Metrikalar (Spring Boot Actuator)

### HTTP Requests Overview
```promql
# Barcha HTTP so'rovlar soni
http_server_requests_seconds_count

# So'nggi 5 daqiqada HTTP so'rovlar soni
increase(http_server_requests_seconds_count[5m])

# HTTP so'rovlar rate (soniyada)
rate(http_server_requests_seconds_count[5m])

# HTTP so'rovlar rate (daqiqada)
rate(http_server_requests_seconds_count[5m]) * 60
```

### HTTP Requests by Status Code
```promql
# 2xx status kodli so'rovlar
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m]))

# 4xx status kodli so'rovlar (client errors)
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m]))

# 5xx status kodli so'rovlar (server errors)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# 5xx xatolar foizi
(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / 
 sum(rate(http_server_requests_seconds_count[5m]))) * 100

# 4xx xatolar foizi
(sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) / 
 sum(rate(http_server_requests_seconds_count[5m]))) * 100
```

### HTTP Requests by Method
```promql
# GET so'rovlari
sum(rate(http_server_requests_seconds_count{method="GET"}[5m]))

# POST so'rovlari
sum(rate(http_server_requests_seconds_count{method="POST"}[5m]))

# PUT so'rovlari
sum(rate(http_server_requests_seconds_count{method="PUT"}[5m]))

# DELETE so'rovlari
sum(rate(http_server_requests_seconds_count{method="DELETE"}[5m]))
```

### HTTP Response Time
```promql
# O'rtacha HTTP response time (barcha endpointlar)
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# O'rtacha HTTP response time (millisekundlarda)
(rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])) * 1000

# Maksimal HTTP response time (5 daqiqada)
max_over_time((rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]))[5m])

# 95-percentile HTTP response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 99-percentile HTTP response time
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

### HTTP Requests by URI
```promql
# Eng ko'p so'rov qabul qilgan endpointlar (top 10)
topk(10, sum by(uri) (rate(http_server_requests_seconds_count[5m])))

# /api/users endpoint so'rovlari
sum(rate(http_server_requests_seconds_count{uri="/api/users"}[5m]))

# /api/users/{id} endpoint so'rovlari
sum(rate(http_server_requests_seconds_count{uri=~"/api/users/.*"}[5m]))

# /actuator/prometheus endpoint so'rovlari
sum(rate(http_server_requests_seconds_count{uri="/actuator/prometheus"}[5m]))
```

## 4. JVM Metrikalar

### JVM Memory Usage
```promql
# JVM heap xotira foydalanish (bytes)
jvm_memory_used_bytes{area="heap"}

# JVM heap xotira maksimal (bytes)
jvm_memory_max_bytes{area="heap"}

# JVM heap xotira foydalanish foizi
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# JVM non-heap xotira foydalanish (bytes)
jvm_memory_used_bytes{area="nonheap"}

# JVM non-heap xotira maksimal (bytes)
jvm_memory_max_bytes{area="nonheap"}

# JVM non-heap xotira foydalanish foizi
(jvm_memory_used_bytes{area="nonheap"} / jvm_memory_max_bytes{area="nonheap"}) * 100

# JVM heap xotira bo'limlari (Eden, Survivor, Old)
jvm_memory_used_bytes{area="heap",id=~"PS Eden Space|PS Survivor Space|PS Old Gen"}

# JVM heap xotira bo'limlari foizi
(jvm_memory_used_bytes{area="heap",id=~"PS Eden Space|PS Survivor Space|PS Old Gen"} / 
 jvm_memory_max_bytes{area="heap",id=~"PS Eden Space|PS Survivor Space|PS Old Gen"}) * 100
```

### JVM Memory Pools
```promql
# Eden Space foydalanish
jvm_memory_used_bytes{id="PS Eden Space"}

# Survivor Space foydalanish
jvm_memory_used_bytes{id="PS Survivor Space"}

# Old Generation foydalanish
jvm_memory_used_bytes{id="PS Old Gen"}

# Metaspace foydalanish
jvm_memory_used_bytes{id="Metaspace"}

# Code Cache foydalanish
jvm_memory_used_bytes{id="Code Cache"}
```

### Garbage Collection
```promql
# GC jami vaqt (millisekundlarda)
jvm_gc_pause_seconds_sum * 1000

# GC o'rtacha vaqt (millisekundlarda)
rate(jvm_gc_pause_seconds_sum[5m]) * 1000

# GC soni
jvm_gc_pause_seconds_count

# GC rate (daqiqada)
rate(jvm_gc_pause_seconds_count[5m]) * 60

# GC turi bo'yicha (Young GC)
jvm_gc_pause_seconds_count{gc="G1 Young Generation"}

# GC turi bo'yicha (Old GC)
jvm_gc_pause_seconds_count{gc="G1 Old Generation"}
```

### JVM Threads
```promql
# JVM threadlar soni
jvm_threads_live_threads

# JVM daemon threadlar soni
jvm_threads_daemon_threads

# JVM peak threadlar soni
jvm_threads_peak_threads

# JVM threadlar o'rtacha soni (5 daqiqada)
avg_over_time(jvm_threads_live_threads[5m])
```

### JVM Classes
```promql
# Yuklangan klasslar soni
jvm_classes_loaded_classes

# Yuklangan klasslar o'rtacha soni (5 daqiqada)
avg_over_time(jvm_classes_loaded_classes[5m])
```

## 5. System va Process Metrikalar

### CPU Usage
```promql
# Process CPU foydalanish foizi
process_cpu_usage * 100

# Process CPU foydalanish o'rtacha qiymati (5 daqiqada)
avg_over_time(process_cpu_usage[5m]) * 100

# System CPU foydalanish
system_cpu_usage * 100

# System CPU count
system_cpu_count
```

### Process Uptime
```promql
# Process ishlash vaqti (sekundlarda)
process_uptime_seconds

# Process ishlash vaqti (soatlarda)
process_uptime_seconds / 3600

# Process ishlash vaqti (kunlarda)
process_uptime_seconds / 86400
```

### Process Memory
```promql
# Process xotira foydalanish (bytes)
process_resident_memory_bytes

# Process xotira foydalanish (MB)
process_resident_memory_bytes / 1024 / 1024

# Process xotira foydalanish (GB)
process_resident_memory_bytes / 1024 / 1024 / 1024

# Process virtual xotira (bytes)
process_virtual_memory_bytes
```

### Process File Descriptors
```promql
# Process file descriptorlar soni
process_files_open_files

# Process maksimal file descriptorlar soni
process_files_max_files
```

## 6. Alerting uchun foydali so'rovlar

### High Error Rate Alert
```promql
# 5 daqiqada xatolar foizi 5% dan yuqori bo'lsa
(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / 
 sum(rate(http_server_requests_seconds_count[5m]))) * 100 > 5
```

### High Memory Usage Alert
```promql
# Heap xotira foydalanish 80% dan yuqori bo'lsa
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 80
```

### High CPU Usage Alert
```promql
# CPU foydalanish 80% dan yuqori bo'lsa
process_cpu_usage * 100 > 80
```

### Application Down Alert
```promql
# Dastur ishlamayapti (uptime metrikasi yo'q)
up{job="devops-assignment"} == 0
```

### High Response Time Alert
```promql
# O'rtacha response time 1 soniyadan yuqori bo'lsa
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]) > 1
```

### Low Cache Hit Ratio Alert
```promql
# Cache hit ratio 70% dan past bo'lsa
application_cache_hit_ratio{cache="main"} < 0.7
```

### High Error Count Alert
```promql
# So'nggi 5 daqiqada 10 dan ko'p xatolar bo'lsa
increase(users_errors_total{api="users"}[5m]) > 10
```

## 7. Grafana Dashboard uchun misollar

### Request Rate Panel
```promql
# HTTP so'rovlar rate (soniyada)
sum(rate(http_server_requests_seconds_count[5m]))
```

### Error Rate Panel
```promql
# Xatolar rate (soniyada)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

### Response Time Panel
```promql
# O'rtacha response time (millisekundlarda)
(rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])) * 1000
```

### Memory Usage Panel
```promql
# Heap xotira foydalanish foizi
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

### CPU Usage Panel
```promql
# CPU foydalanish foizi
process_cpu_usage * 100
```

### Active Sessions Panel
```promql
# Faol sessiyalar soni
application_active_sessions{type="http"}
```

### User Requests Panel
```promql
# User API so'rovlari rate (daqiqada)
rate(users_requests_total{api="users"}[5m]) * 60
```

### Cache Hit Ratio Panel
```promql
# Cache hit ratio (foiz)
application_cache_hit_ratio{cache="main"} * 100
```

## 8. Qo'shimcha foydali so'rovlar

### Top Endpoints by Request Count
```promql
# Eng ko'p so'rov qabul qilgan endpointlar (top 5)
topk(5, sum by(uri) (rate(http_server_requests_seconds_count[5m])))
```

### Top Endpoints by Response Time
```promql
# Eng sekin endpointlar (top 5)
topk(5, rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]))
```

### Request Distribution by Status
```promql
# Status kodlar bo'yicha so'rovlar taqsimoti
sum by(status) (rate(http_server_requests_seconds_count[5m]))
```

### Memory Usage Trend
```promql
# Heap xotira foydalanish tendentsiyasi (1 soat)
avg_over_time((jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"})[1h:5m]) * 100
```

### Application Health Score
```promql
# Dastur sog'liq ko'rsatkichi (0-100)
(
  (1 - (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])))) * 50 +
  (1 - clamp_max((jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) - 0.8, 0.2) / 0.2) * 30 +
  (1 - clamp_max(process_cpu_usage - 0.8, 0.2) / 0.2) * 20
) * 100
```

## 9. Foydalanish bo'yicha ko'rsatmalar

### Prometheus UI da ishlatish:
1. Prometheus UI ga kiring (odatda `http://localhost:9090`)
2. "Graph" bo'limiga o'ting
3. Yuqoridagi so'rovlardan birini kiriting
4. "Execute" tugmasini bosing

### Grafana Dashboard yaratish:
1. Grafana da yangi dashboard yarating
2. Yangi panel qo'shing
3. Panel uchun "Prometheus" data source ni tanlang
4. Yuqoridagi so'rovlardan birini kiriting
5. Visualization turini tanlang (Graph, Stat, Gauge, va hokazo)

### Alerting qoidalar yaratish:
1. Prometheus yoki Alertmanager da yangi alerting rule yarating
2. Yuqoridagi "Alerting uchun foydali so'rovlar" bo'limidagi so'rovlardan foydalaning
3. Threshold qiymatlarini o'z ehtiyojingizga moslashtiring

## 10. Eslatmalar

- Barcha vaqt oralig'i `[5m]` yoki `[1h]` kabi formatda ko'rsatilgan. Bu qiymatlarni o'z ehtiyojingizga moslashtirishingiz mumkin.
- Rate va increase funksiyalari counter metrikalar uchun ishlatiladi.
- Histogram metrikalar uchun `histogram_quantile` funksiyasidan foydalaning.
- Gauge metrikalar uchun `avg_over_time`, `max_over_time` kabi funksiyalardan foydalaning.
- Label filtrlash uchun `{label="value"}` yoki `{label=~"regex"}` sintaksisidan foydalaning.
