import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * ปล่อยงานเข้าสู่ระบบตามเวลา arrivalMs ของแต่ละ Job
 *
 * หน้าที่ (หัวข้อ 3 ของโจทย์):
 *   - รอจนถึงเวลา arrivalMs ของแต่ละงาน แล้วส่งงานต่อไปยัง Scheduler
 *   - บันทึกเวลาที่งานเข้าสู่ระบบ "จริง" ลงใน Job
 *     (อาจไม่ตรงกับ arrivalMs เป๊ะ เพราะ Thread ถูกปลุกช้าได้)
 *   - เรียก logger.jobArrived(job) ทุกครั้งที่ปล่อยงาน
 *
 * ข้อควรคิด:
 *   - รายการงานที่ได้จาก WorkloadLoader เรียงตามลำดับในไฟล์ ไม่ได้เรียงตามเวลา
 *   - เมื่อปล่อยงานครบทุกชิ้นแล้ว ต้องมีวิธีบอกระบบว่า "จะไม่มีงานเข้ามาอีก"
 *     โดยส่ง poison pill ไปยัง Scheduler
 */

public class JobGenerator extends Thread {

    // รายการงานที่เรียงตามเวลาปล่อยงาน
    private final List<Job> jobs;
    private final BlockingQueue<Job> arrivalQueue;
    private final ProjectLogger logger;

    public JobGenerator(List<Job> jobs, BlockingQueue<Job> arrivalQueue, ProjectLogger logger) {
        // ตั้งชื่อ Thread เพื่อให้อ่าน log ได้ง่าย
        super("generator");
        // CSV rows need not be ordered by arrival time. Keep each Job's original
        // sequence for deterministic tie-breaking elsewhere, but release by time.
        // ทำสำเนาเพื่อไม่แก้ลิสต์ต้นฉบับของ Main
        this.jobs = new ArrayList<>(jobs);
        this.jobs.sort(Comparator.comparingLong((Job job) -> job.arrivalMs)
                .thenComparingInt(job -> job.sequence));
        this.arrivalQueue = arrivalQueue;
        this.logger = logger;
    }

    @Override
    public void run() {
        // จำไว้ว่าถูก interrupt ระหว่าง sleep หรือไม่
        boolean interrupted = false;
        try {
            // ปล่อยงานทีละรายการตาม arrival time
            for (Job job : jobs) {
                long sleepTime = job.arrivalMs - logger.now();
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // Finish producing the workload so downstream threads can
                        // drain and terminate instead of being stranded.
                        interrupted = true;
                    }
                }
                // บันทึกเวลาที่งานเข้าระบบจริง
                job.actualArrivalMs = logger.now();
                job.state = Job.State.ARRIVED;
                logger.jobArrived(job);
                // ส่งงานให้ Scheduler ผ่านคิวกลาง
                arrivalQueue.offer(job);
            }
        } finally {
            // แจ้ง Scheduler ว่าปล่อยงานครบแล้ว
            arrivalQueue.offer(Job.POISON_PILL);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
