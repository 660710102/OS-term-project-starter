ชุดข้อมูลสำหรับโปรเจ็ค Operating Systems

ไฟล์ Workload
- jobs_standard.csv: เปรียบเทียบการจัดคิวและจำนวน Worker
- jobs_printer.csv: ทดลองคอขวดจากการใช้ PRINTER
- jobs_db.csv: ทดลองการใช้ DATABASE ที่มี permit จำกัด
- jobs_same_priority.csv: ตรวจลำดับเมื่อทุกงานมี priority เท่ากัน
- jobs_single.csv: ตรวจการทำงานและการปิดระบบเมื่อมีงานเดียว
- jobs_edge_cases.csv: ชุดใหญ่ขึ้นสำหรับดูงานมาพร้อมกัน/ต่างเวลา, priority หลายระดับ และการแย่งใช้ทรัพยากร

วิธีรัน
เปิด Terminal ที่โฟลเดอร์หลักของโปรเจ็ค แล้วคอมไพล์:
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out src\*.java

จากนั้นใช้รูปแบบคำสั่ง:
java -cp out Main workloads\<ไฟล์.csv> <fcfs|priority> <workers> <printerPermits> <databasePermits>

ตัวอย่างการทดลองหลัก
java -cp out Main workloads\jobs_standard.csv fcfs 3 1 2
java -cp out Main workloads\jobs_standard.csv priority 3 1 2
java -cp out Main workloads\jobs_standard.csv priority 1 1 2
java -cp out Main workloads\jobs_standard.csv priority 5 1 2
java -cp out Main workloads\jobs_printer.csv priority 3 1 2
java -cp out Main workloads\jobs_printer.csv priority 3 2 2
java -cp out Main workloads\jobs_standard.csv priority 3 1 2

ตัวอย่างตรวจกรณีพิเศษ
java -cp out Main workloads\jobs_single.csv priority 1 1 1
java -cp out Main workloads\jobs_db.csv priority 3 1 2
java -cp out Main workloads\jobs_same_priority.csv priority 2 1 1
java -cp out Main workloads\jobs_edge_cases.csv priority 3 1 1

รูปแบบ CSV
id,arrivalMs,priority,workMs,resource,resourceMs

เวลาทุกช่องใช้หน่วยมิลลิวินาที โปรแกรมตัดช่องว่างรอบค่าให้อัตโนมัติ และรับ resource ได้เป็น NONE, PRINTER หรือ DATABASE
