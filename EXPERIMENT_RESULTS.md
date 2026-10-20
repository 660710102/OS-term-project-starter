# ผลการทดลอง OS_Term_Project_Starter

ค่าด้านล่างอ่านจากการรันใหม่ตาม `workloads/README_workloads.txt` และตรวจเทียบกับ Raw Log ในโฟลเดอร์ `Logs` เมื่อวันที่ 18 ตุลาคม 2026 เวลาอาจเปลี่ยนเล็กน้อยเมื่อรันซ้ำ เพราะระบบมีหลาย Thread และขึ้นกับการจัดสรรเวลาของระบบปฏิบัติการ

| รอบ | Workload / Policy | Workers | Printer | DB | avg WT (ms) | avg TAT (ms) | Throughput (jobs/sec) | avg RW (ms) | Log |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | standard / FCFS | 3 | 1 | 2 | 2962 | 5468 | 1.14 | 77 | [01_standard_fcfs_w3_p1_d2.log](Logs/01_standard_fcfs_w3_p1_d2.log) |
| 2 | standard / Priority | 3 | 1 | 2 | 2339 | 4975 | 1.08 | 260 | [02_standard_priority_w3_p1_d2.log](Logs/02_standard_priority_w3_p1_d2.log) |
| 3 | standard / Priority | 1 | 1 | 2 | 8546 | 11003 | 0.41 | 1 | [03_standard_priority_w1_p1_d2.log](Logs/03_standard_priority_w1_p1_d2.log) |
| 4 | standard / Priority | 5 | 1 | 2 | 1131 | 4085 | 1.29 | 717 | [04_standard_priority_w5_p1_d2.log](Logs/04_standard_priority_w5_p1_d2.log) |
| 5 | printer / Priority | 3 | 1 | 2 | 1154 | 4256 | 0.80 | 1612 | [05_printer_priority_w3_p1_d2.log](Logs/05_printer_priority_w3_p1_d2.log) |
| 6 | printer / Priority | 3 | 2 | 2 | 765 | 2554 | 1.42 | 302 | [06_printer_priority_w3_p2_d2.log](Logs/06_printer_priority_w3_p2_d2.log) |
| 7 | standard / Priority (repeat) | 3 | 1 | 2 | 2329 | 4959 | 1.08 | 254 | [07_standard_priority_w3_p1_d2_repeat.log](Logs/07_standard_priority_w3_p1_d2_repeat.log) |

หมายเหตุ: ตัวเลขใช้หน่วยมิลลิวินาที ยกเว้น Throughput ที่ใช้ jobs/sec โดยค่าเฉลี่ย Resource Wait คำนวณเฉพาะ Job ที่ใช้ resource

## คำตอบคำถามการทดลอง

คำถามด้านล่างคงข้อความตามโจทย์เดิม และคำตอบอ้างอิงจากผลการทดลองในตารางด้านบน

### คำถามข้อ 1

เพิ่ม Worker จาก 3 เป็น 5 แล้ว Throughput เพิ่มตามสัดส่วนหรือไม่ และ avg Resource Wait เปลี่ยนอย่างไร เพราะอะไร?

**คำตอบ** เพิ่ม Worker จาก 3 เป็น 5 ทำให้ Throughput ที่วัดได้เพิ่มจาก 1.08 เป็น 1.29 jobs/sec ในชุดทดลองนี้ แต่ไม่เพิ่มตามสัดส่วน เพราะยังมีเวลาทำงานและ Resource ที่จำกัด ส่วน avg Resource Wait เพิ่มจาก 260 เป็น 717 ms เนื่องจากมี Worker ขอใช้ Resource ใกล้เคียงกันมากขึ้น ผลนี้เป็นข้อสังเกตจาก workload และการตั้งค่าชุดนี้ ไม่ใช่การรับประกันว่าจะเกิดผลแบบเดียวกันทุกกรณี

### คำถามข้อ 2

FCFS กับ Priority ทำให้ Job กลุ่มใดได้ประโยชน์หรือเสียประโยชน์จาก jobs_standard.csv? ให้อ้างอิง Waiting Time และลำดับ START ใน log และอธิบายด้วยว่าเหตุใด Priority Scheduling ในการทดลองนี้อาจมี Throughput ต่ำกว่า FCFS แม้ค่าเฉลี่ย Waiting Time จะดีกว่า

**คำตอบ** ใน log รอบ FCFS งานที่เข้าคิวก่อนถูกเลือกก่อน ส่วนรอบ Priority หลัง Worker ทั้งสามเริ่มงานชุดแรกแล้ว คิวเลือกงาน priority สูงกว่าจาก Ready Queue ก่อนงาน priority ต่ำกว่าบางงาน จึงช่วยงานสำคัญที่ยังรออยู่ แต่ทำให้งานสำคัญน้อยกว่าบางงานรอนานขึ้น ค่าเฉลี่ย Waiting Time ลดจาก 2962 เป็น 2339 ms ส่วน Throughput ที่วัดได้เปลี่ยนจาก 1.14 เป็น 1.08 jobs/sec ในสองรอบนี้ การจัดคิวไม่ได้เพิ่มจำนวน Worker หรือ permit และลำดับการทำงานพร้อมกันมีผลต่อเวลาจบ ผลนี้เป็นข้อสังเกตจากการทดลอง ไม่ใช่กฎว่า Priority จะมี Throughput ต่ำกว่า FCFS เสมอ

### คำถามข้อ 3

เมื่อเพิ่ม Printer permit จาก 1 เป็น 2 แล้ว avg Resource Wait และ Throughput เปลี่ยนอย่างไร และจุดคอขวดของระบบย้ายไปอยู่ที่ใด?

**คำตอบ** เมื่อเพิ่ม Printer permit จาก 1 เป็น 2 ค่าเฉลี่ย Resource Wait ลดจาก 1612 เป็น 302 ms และ Throughput เพิ่มจาก 0.80 เป็น 1.42 jobs/sec ในชุดทดลองนี้ แสดงว่า Printer จำกัดการทำงานอย่างชัดเจนเมื่อมี permit เดียว หลังเพิ่ม permit การรอ Printer ลดลง ส่วนจำนวน Worker และเวลาทำงานยังมีผลต่อเวลารวมของระบบ

### คำถามข้อ 4

เปรียบเทียบแถว 2 กับแถว 7: เหตุใด workload และชุดค่าที่ใช้รันเหมือนเดิม จึงอาจให้เวลาใน log ลำดับเหตุการณ์ และค่าที่วัดต่างกันเล็กน้อย?

**คำตอบ** รอบ 2 และ 7 ใช้ workload และค่ารันเหมือนกัน แต่เป็นคนละการรัน จังหวะการจัดเวลาและการสลับทำงานของหลาย Thread ต่างกัน จึงทำให้ timestamp และลำดับ event ต่างกันเล็กน้อย เช่น avg TAT 4975 กับ 4959 ms โดยแนวโน้มของผลใกล้เคียงกัน

## การทดลองตรวจกรณีพิเศษจาก Log เพิ่มเติม

| Log | จุดประสงค์ | ผลที่ตรวจได้ |
|---|---|---|
| [08_single_priority_w1_p1_d1.log](Logs/08_single_priority_w1_p1_d1.log) | ตรวจกรณีมีงานเดียวและการปิดระบบ | งาน `ONE01` เสร็จครบ `1/1`; WT 0 ms, TAT 819 ms |
| [09_database_priority_w3_p1_d2.log](Logs/09_database_priority_w3_p1_d2.log) | ตรวจการแย่งใช้ DATABASE | งานเสร็จครบ `6/6`; ใช้พร้อมกันสูงสุด `2/2`; Throughput 1.42 jobs/sec |
| [10_same_priority_w2_p1_d1.log](Logs/10_same_priority_w2_p1_d1.log) | ตรวจ tie-break เมื่องานมี priority เท่ากัน | ลำดับเริ่มงานเป็น `S01` ถึง `S06` ตาม CSV; เสร็จครบ `6/6`; Throughput 2.13 jobs/sec |

ผลการทดลองเพิ่มเติมนี้ใช้ตรวจกรณีพิเศษ ไม่ได้นำมาปนกับค่าเฉลี่ยของ 7 รอบหลักข้างต้น
