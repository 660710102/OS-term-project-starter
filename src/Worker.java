import java.util.concurrent.TimeUnit;

/**
 * Thread ที่ดึงงานจาก Ready Queue ไปทำจนเสร็จ
 *
 * ลำดับการทำงานของ Job หนึ่งชิ้น บังคับตามหัวข้อ 6 ของโจทย์:
 * 1. รับงานจาก Ready Queue แล้วบันทึกเวลาเริ่ม
 * 2. จำลองงานหลักด้วย Thread.sleep(job.workMs)
 * 3. ถ้า job.resource != NONE ให้บันทึกเวลาเริ่มรอ แล้ว acquire
 * 4. จำลองการถือครองด้วย Thread.sleep(job.resourceMs)
 * 5. release แล้วบันทึกเวลาจบ
 *
 * ห้ามสลับขั้นที่ 2 กับ 3 เพราะจะทำให้ผลของทุกกลุ่มเทียบกันไม่ได้
 *
 * จุดที่มักพลาด:
 * - ถ้า exception หรือ interrupt เกิดขึ้นหลัง acquire แต่ก่อน release
 * permit จะค้างถาวรและระบบจะแขวน ต้องออกแบบให้คืนได้เสมอ
 * - Worker ต้องหยุดเองได้เมื่อไม่มีงานเหลือแล้ว ไม่ใช่วนรอตลอดไป
 */

public class Worker extends Thread {

    // ส่วนประกอบที่ Worker ใช้ทำงาน
    private final ReadyQueue readyQueue;
    private final ResourceManager resources;
    private final Statistics statistics;
    private final ProjectLogger logger;
    private boolean interrupted;

    public Worker(String name, ReadyQueue readyQueue, ResourceManager resources,
            Statistics statistics, ProjectLogger logger) {
        // ตั้งชื่อ Worker เพื่อระบุใน log
        super(name);
        this.readyQueue = readyQueue;
        this.resources = resources;
        this.statistics = statistics;
        this.logger = logger;
    }

    @Override
    public void run() {
        // ดึงงานและทำต่อจนพบ poison pill
        while (true) {
            Job job;
            try {
                // take() ทำให้ Thread รอโดยไม่ busy waiting
                job = readyQueue.take();
            } catch (InterruptedException e) {
                // Finish queued work and let the poison pill stop the worker.
                interrupted = true;
                continue;
            }
            // poison pill ใช้จบ Worker หลังงานจริงหมดแล้ว
            if (job == Job.POISON_PILL) {
                readyQueue.add(Job.POISON_PILL);
                break;
            }
            job.state = Job.State.RUNNING;
            processJob(job);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void processJob(Job job) {
        // ลำดับงานคือทำงานหลัก ขอ resource แล้วจึงจบงาน
        // 1. บันทึกเวลาเริ่มทำงาน และแจ้ง Statistics ว่ามีงานเริ่มรัน
        job.startMs = logger.now();
        if (statistics != null) {
            statistics.jobStarted();
        }

        try {
            logger.jobStarted(job);

            // 2. จำลองงานหลักด้วย Thread.sleep(job.workMs)
            sleepUninterruptibly(job.workMs);
            logger.workFinished(job);

            // 3. ถ้าต้องใช้ทรัพยากรร่วม ให้ขอสิทธิ์ผ่าน Semaphore
            // ขอ resource เฉพาะงานที่ระบุใน CSV
            if (job.resource != ResourceType.NONE) {
                long waitStart = logger.now();
                job.state = Job.State.WAITING_RESOURCE;
                logger.resourceWaitStarted(job);

                if (resources != null) {
                    acquireUninterruptibly(job.resource);
                }

                try {
                    // 4. บันทึกเวลารอ resource และจำลองการถือครอง
                    if (resources != null) {
                        synchronized (resources.eventLock(job.resource)) {
                            job.resourceWaitMs = logger.now() - waitStart;
                            job.state = Job.State.RUNNING;
                            logger.resourceAcquired(job, job.resourceWaitMs);
                        }
                    } else {
                        job.resourceWaitMs = logger.now() - waitStart;
                        job.state = Job.State.RUNNING;
                        logger.resourceAcquired(job, job.resourceWaitMs);
                    }
                    sleepUninterruptibly(job.resourceMs);
                } finally {
                    // 5. คืนสิทธิ์ใน finally เสมอเพื่อป้องกัน permit หายเมื่อถูก interrupt
                    if (resources != null) {
                        synchronized (resources.eventLock(job.resource)) {
                            resources.release(job.resource);
                            logger.resourceReleased(job);
                        }
                    } else {
                        logger.resourceReleased(job);
                    }
                }
            }

            // 6. บันทึกเวลาจบงาน และอัปเดตจำนวนงานที่ทำเสร็จ
            job.completionMs = logger.now();
            job.state = Job.State.COMPLETED;
            logger.jobCompleted(job);
            if (statistics != null) {
                statistics.recordCompletion(job);
            }
        } finally {
            // ลดจำนวนงานที่กำลังทำ (running) เสมอเมื่อจบการทำงานของ Job นี้
            if (statistics != null) {
                statistics.jobFinished();
            }
        }
    }

    private void sleepUninterruptibly(long millis) {
        // พยายามจำลองเวลาทำงานให้ครบ แม้ถูก interrupt
        long startNanos = System.nanoTime();
        long durationNanos = TimeUnit.MILLISECONDS.toNanos(millis);
        while (true) {
            long elapsedNanos = System.nanoTime() - startNanos;
            long remainingNanos = durationNanos - elapsedNanos;
            if (remainingNanos <= 0) {
                return;
            }
            long remainingMillis = Math.max(1L,
                    remainingNanos / 1_000_000L + (remainingNanos % 1_000_000L == 0 ? 0 : 1));
            try {
                Thread.sleep(remainingMillis);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
    }

    private void acquireUninterruptibly(ResourceType type) {
        // พยายามขอ permit จนสำเร็จ แล้วค่อยคืนสถานะ interrupt ภายหลัง
        while (true) {
            try {
                resources.acquire(type);
                return;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
    }
}
