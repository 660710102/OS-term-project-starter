# คู่มืออ่านโค้ด OS Term Project Starter
เอกสารนี้อธิบายหน้าที่ของแต่ละไฟล์และลำดับการทำงาน เพื่อช่วยทำความเข้าใจโครงการ

## ภาพรวมการทำงาน

```text
Main
  -> JobGenerator ปล่อย Job ตาม arrivalMs
  -> arrivalQueue รับ Job
  -> Scheduler ย้าย Job ไป ReadyQueue
  -> Worker ดึง Job ไปทำงาน
  -> ResourceManager คุม PRINTER / DATABASE
  -> Statistics เก็บ metrics
  -> Monitor รายงานสถานะทุกประมาณ 1 วินาที
```

ระบบนี้ใช้ Worker ที่สร้างจาก `Thread` โดยตรงและจัดการคิวเอง ไม่ได้ใช้ `ExecutorService` หรือ Virtual Thread

## ไฟล์ทั้งหมด

### `Main.java`

- จุดเริ่มต้นของโปรแกรม
- อ่าน command-line arguments
- โหลด workload
- สร้าง Queue, Worker, Scheduler, Monitor และ ResourceManager
- start Thread ทุกตัว
- join รอจนงานจบ
- สั่งหยุด Monitor
- คำนวณ makespan และพิมพ์ Summary

### `Config.java`

- แปลง arguments เป็นค่าที่โปรแกรมใช้
- ตรวจ policy ว่าเป็น `fcfs` หรือ `priority`
- ตรวจ workers และจำนวน permit ต้องไม่น้อยกว่า 1
- เก็บค่าการตั้งค่าของการรันหนึ่งครั้ง

### `Job.java`

- เก็บข้อมูลของ Job จาก CSV
- `arrivalMs` คือเวลาที่กำหนดให้เข้าระบบ
- `startMs` และ `completionMs` ใช้คำนวณ metrics
- `resourceWaitMs` เก็บเวลารอ Semaphore
- `state` แยกสถานะ Job ออกจาก `Thread.State`
- `POISON_PILL` ใช้บอก Worker ว่างานหมดแล้ว

### `JobGenerator.java`

- เป็น Thread ที่ปล่อย Job ตามเวลา `arrivalMs`
- เรียง Job ตาม arrival time ก่อนปล่อย
- ใช้ sequence เดิมเป็น tie-break เมื่อ arrival เท่ากัน
- บันทึก `actualArrivalMs`
- ส่ง poison pill เมื่อปล่อย Job ครบ

### `Scheduler.java`

- เป็น Thread ที่รับ Job จาก `arrivalQueue`
- ย้าย Job ไป `ReadyQueue`
- ไม่ให้ JobGenerator ข้าม Scheduler
- ส่งต่อ poison pill เพื่อเริ่ม shutdown ฝั่ง Worker

### `ReadyQueue.java`

- เก็บ Job ที่พร้อมทำงาน
- ใช้ FIFO เมื่อเลือก FCFS
- ใช้ PriorityBlockingQueue เมื่อเลือก Priority
- priority เลขน้อยกว่าถือว่าสำคัญกว่า
- ถ้า priority เท่ากัน ใช้ sequence ตัดสิน
- `take()` ทำให้ Worker รอแบบไม่กิน CPU

### `Worker.java`

- เป็น Thread ที่ทำ Job จริง
- รับ Job จาก ReadyQueue
- บันทึกเวลาเริ่ม
- ทำงานหลักด้วยเมธอดจำลองเวลา `sleepUninterruptibly(workMs)`
- ถ้ามี resource จะรอและ acquire Semaphore
- ทำงานในช่วงที่ถือ resource
- release resource ใน `finally`
- บันทึก completion และเพิ่มจำนวน Job ที่เสร็จ
- เมื่อเจอ poison pill จะหยุดตัวเอง

### `ResourceManager.java`

- เป็นเจ้าของ Semaphore ของ PRINTER และ DATABASE
- สร้าง Semaphore แบบ `fair=true`
- `acquire()` ใช้ขอ permit
- `release()` คืน permit
- `status()` ใช้รายงานจำนวน resource ที่กำลังถูกใช้
- event lock ช่วยให้ลำดับ log acquire/release อ่านได้ถูกต้อง

### `Statistics.java`

- นับจำนวน Job ที่กำลังทำและทำเสร็จ
- ใช้ `synchronized` ป้องกันตัวนับร่วม
- `Snapshot` รวมค่าที่ Monitor อ่านในจังหวะเดียวกัน
- คำนวณ Waiting Time, Turnaround Time, Throughput และ Resource Wait
- ค่าเฉลี่ย Resource Wait คิดเฉพาะ Job ที่ใช้ resource

### `Monitor.java`

- เป็น Thread ที่รายงานสถานะประมาณทุก 1,000 ms
- อ่าน ready, running, completed และ resource status
- ใช้ `shutdown()` เปลี่ยนสถานะและ interrupt ตัวเอง
- ไม่ทำงานต่อหลังถูกสั่ง shutdown

### `ProjectLogger.java`

- จัดรูปแบบและพิมพ์ log ของระบบ
- ใช้เวลาฐานเดียวกันจาก `now()`
- ใช้ lock ป้องกัน log จากหลาย Thread เขียนทับกัน
- บันทึก `JOB_ARRIVED`, `JOB_STARTED`, `RESOURCE_WAIT`, `RESOURCE_ACQUIRED`, `RESOURCE_RELEASED` และ `JOB_COMPLETED`

### `WorkloadLoader.java`

- อ่าน CSV จากไฟล์ภายนอก
- ตัดช่องว่างและรองรับ header/BOM
- ตรวจจำนวนคอลัมน์และค่าตัวเลข
- ตรวจ id ซ้ำ
- ตรวจ resource และ resourceMs
- คืนรายการ Job ที่แก้ไขไม่ได้

### `ResourceType.java`

- enum ของ resource ที่อนุญาต
- ค่าได้แก่ `NONE`, `PRINTER`, `DATABASE`
- แปลงข้อความจาก CSV เป็น enum

### `WorkloadFormatException.java`

- Exception สำหรับรายงาน CSV ที่มีรูปแบบผิด
- เก็บเลขบรรทัดและข้อความอธิบายปัญหา

## Metrics ที่ควรรู้

```text
Waiting Time       = startMs - actualArrivalMs
Turnaround Time    = completionMs - actualArrivalMs
Resource Wait Time = เวลาตั้งแต่เริ่มรอจน acquire สำเร็จ
Throughput         = จำนวน Job x 1,000 / makespanMs
```

สำหรับ Job ที่ไม่ใช้ resource ให้ Resource Wait Time เท่ากับ 0

## ลำดับที่ควรอธิบายใน Demo

1. Generator ปล่อย Job เข้า arrivalQueue ตามเวลา
2. Scheduler ย้าย Job เข้า ReadyQueue
3. Worker หลายตัวดึงจาก Queue เดียวกัน
4. Priority Queue ใช้ priority แล้วใช้ sequence เป็น tie-break
5. Semaphore จำกัดการใช้ resource พร้อมกัน
6. Statistics ใช้ synchronized ป้องกันข้อมูลร่วม
7. Poison pill และ join ทำให้ระบบปิดหลัง Job สุดท้ายเสร็จ
