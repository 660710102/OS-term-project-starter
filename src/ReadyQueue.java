import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * คิวงานที่พร้อมถูกหยิบไปทำ
 *
 * สิ่งที่คลาสนี้ต้องทำได้:
 * - เก็บงานที่รอ Worker อยู่
 * - หยิบงานถัดไปตามนโยบายที่เลือก (FCFS หรือ Priority)
 * - ถูกเรียกจากหลาย Thread พร้อมกันได้อย่างปลอดภัย
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 * - หัวข้อ 4: priority = 1 สูงสุด เมื่อเท่ากันต้องมีกติกาตัดสินลำดับ
 * (tie-break)
 * ที่ตัดสินจากข้อมูลของ Job ไม่ขึ้นกับว่า Thread ใดเข้าถึงคิวก่อน
 * - หัวข้อ 7: ห้ามวนลูปเช็กแบบกิน CPU (busy waiting) — Worker ที่ไม่มีงานทำ
 * ต้องถูกพักไว้ ไม่ใช่วนถามซ้ำ ๆ
 *
 * จะออกแบบเป็นคลาสเดียวที่รับนโยบายเข้ามา หรือแยกเป็นสองคลาส
 * หรือใช้โครงสร้างข้อมูลสำเร็จรูปของ Java ก็ได้ ขอให้อธิบายเหตุผลได้ใน Demo
 */

public class ReadyQueue {

    // คิวจริงที่ใช้เก็บงานรอ Worker
    private final BlockingQueue<Job> queue;

    public ReadyQueue(Config.Policy policy) {
        // FCFS ใช้คิวเข้าก่อนออกก่อน
        if (policy == Config.Policy.FCFS) {
            this.queue = new LinkedBlockingQueue<>();
        } else {
            // Priority ใช้เลขน้อยเป็นความสำคัญสูงกว่า
            Comparator<Job> comp = (j1, j2) -> {
                if (j1.priority != j2.priority) {
                    return Integer.compare(j1.priority, j2.priority);
                }
                return Integer.compare(j1.sequence, j2.sequence);
            };
            this.queue = new PriorityBlockingQueue<>(11, comp);
        }
    }

    public void add(Job job) {
        // เพิ่มงานโดยไม่ทำให้ผู้เรียกต้องรอ
        queue.offer(job);
    }

    public Job take() throws InterruptedException {
        // รอแบบ blocking แทนการวนถามคิวซ้ำ ๆ
        return queue.take();
    }

    public int size() {
        // ถ้ารวม POISON_PILL อยู่ในคิวด้วย ให้ลบออก 1 เพื่อรายงานเฉพาะจำนวนงานจริงที่รออยู่
        int currentSize = queue.size();
        if (queue.contains(Job.POISON_PILL)) {
            return Math.max(0, currentSize - 1);
        }
        return currentSize;
    }
}
