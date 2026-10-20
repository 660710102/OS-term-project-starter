import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * จุดเริ่มต้นของโปรแกรม
 *
 * ประกอบส่วนต่าง ๆ ของระบบ เริ่ม Thread ทั้งหมด รอให้งานถูกประมวลผลจนครบ
 * แล้วจึงหยุด Monitor และแสดงผลสรุป
 *
 * วิธีรัน:
 *   java -cp out Main workloads/jobs_standard.csv priority 3 1 2
 */


public class Main {

    public static void main(String[] args) {
        // ---------- 1. รับค่าจาก command line ----------
        // ตรวจสอบค่าที่ผู้ใช้ส่งมาก่อนเริ่มระบบ
        Config config;
        try {
            config = Config.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(Config.USAGE);
            System.exit(1);
            return;
        }

        // ---------- 2. เริ่มจับเวลาและโหลด workload ----------
        // logger เป็นนาฬิกากลางและตัวบันทึกเหตุการณ์
        ProjectLogger logger = new ProjectLogger();
        List<Job> jobs;
        try {
            jobs = WorkloadLoader.load(config.workloadPath);
        } catch (WorkloadFormatException e) {
            System.err.println("Invalid workload file: " + e.getMessage());
            System.exit(1);
            return;
        } catch (java.io.IOException e) {
            System.err.println("Could not open file \"" + config.workloadPath + "\".");
            System.err.println("Check that the file exists and the path is correct for the current directory.");
            System.exit(1);
            return;
        }
        logger.systemStart(config);
        logger.systemEvent("Loaded " + jobs.size() + " jobs");

        // ---------- 3. สร้างส่วนประกอบของระบบ ----------
        // คิวระหว่าง JobGenerator กับ Scheduler
        BlockingQueue<Job> arrivalQueue = new LinkedBlockingQueue<>();
        ReadyQueue readyQueue = new ReadyQueue(config.policy);
        ResourceManager resources = new ResourceManager(config.printerPermits, config.databasePermits);
        Statistics statistics = new Statistics();

        // ---------- 4. สร้างและเริ่ม Thread ----------
        // สร้าง Worker ตามจำนวนที่กำหนด
        List<Worker> workers = new ArrayList<>();
        for (int i = 1; i <= config.workers; i++) {
            Worker w = new Worker("worker-" + i, readyQueue, resources, statistics, logger);
            workers.add(w);
            w.start();
        }

        Scheduler scheduler = new Scheduler(arrivalQueue, readyQueue, logger);
        Monitor monitor = new Monitor(readyQueue, resources, statistics, logger);
        JobGenerator generator = new JobGenerator(jobs, arrivalQueue, logger);

        // เริ่มทุก Thread หลังเตรียมส่วนประกอบครบแล้ว
        scheduler.start();
        monitor.start();
        generator.start();

        // ---------- 5 & 6. รอจนงานเสร็จครบและสั่งหยุดทุก Thread ----------
        boolean interrupted = false;
        // Complete the normal drain sequence even if the main thread receives
        // an interrupt while joining; otherwise it could print partial results.
        interrupted |= joinUninterruptibly(generator);
        interrupted |= joinUninterruptibly(scheduler);
        for (Worker w : workers) {
            interrupted |= joinUninterruptibly(w);
        }
        monitor.shutdown();
        interrupted |= joinUninterruptibly(monitor);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        // ---------- 7. สรุปผล ----------
        // หาเวลาที่งานสุดท้ายเสร็จ
        long makespanMs = 0;
        for (Job job : jobs) {
            if (job.completionMs > makespanMs) {
                makespanMs = job.completionMs;
            }
        }

        logger.systemStop(statistics.completedCount(), jobs.size());
        if (statistics.completedCount() != jobs.size()) {
            System.err.println("Error: Not all jobs completed ("
                    + statistics.completedCount() + "/" + jobs.size() + "); summary will not be displayed.");
            return;
        }
        statistics.printSummary(jobs, makespanMs);
    }

    private static boolean joinUninterruptibly(Thread thread) {
        // รอให้ Thread จบ แม้ Main จะถูก interrupt ระหว่างรอ
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        return interrupted;
    }
}
