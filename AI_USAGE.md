# บันทึกการใช้ AI

## งานที่ใช้ AI ช่วย

กลุ่มใช้ ChatGPT Codex เพื่อช่วย:

- วางโครงสร้างโปรแกรม Java
- อธิบายการทำงานของ `Thread`, `BlockingQueue` และ `Semaphore`
- ตรวจแนวทางคำนวณ Metrics
- เสนอแนวทางทดสอบ Concurrency

## งานที่กลุ่มตรวจสอบเอง

กลุ่มตรวจสอบและปรับแก้ tie-break ของ Priority Queue ให้ใช้ `sequence` จากลำดับแถวใน CSV แทนลำดับที่ Thread เข้าถึงคิว จากนั้นทดสอบด้วย `jobs_same_priority.csv` เพื่อยืนยันว่า S01-S06 เริ่มตามลำดับที่กำหนด

นอกจากนี้ กลุ่มตรวจสอบค่า Resource Wait และ Throughput กับ Raw Log จริงก่อนบันทึกลงตารางผลการทดลองใน `EXPERIMENT_RESULTS.md`
