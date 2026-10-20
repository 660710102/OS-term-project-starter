import java.util.List;

/**
 * รวบรวมและคำนวณค่าที่ใช้วัดผลของการรันหนึ่งครั้ง
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้ (หัวข้อ 8):
 *   - Waiting Time, Turnaround Time, Throughput, Resource Wait Time
 *   - ต้องถูกอัปเดตจากหลาย Worker พร้อมกันได้อย่างปลอดภัย
 *   - ผลต้องสอดคล้องกับสมการตรวจสอบ:
 *       Turnaround = Waiting + workMs + Resource Wait + resourceMs
 *     ใช้สมการนี้ตรวจงานทีละชิ้นได้ว่าค่าไหนคำนวณผิด
 *
 * ข้อควรระวัง: ค่าเฉลี่ยของ Resource Wait ให้คิดเฉพาะงานที่ใช้ resource
 * ส่วนงานที่ resource = NONE ให้ถือว่า Resource Wait เป็น 0
 */

public class Statistics {

    /** Immutable point-in-time view used by Monitor. */
    public static final class Snapshot {
        // ค่าชุดนี้ไม่เปลี่ยนหลังสร้าง จึงอ่านได้อย่างปลอดภัย
        public final int running;
        public final int completed;

        private Snapshot(int running, int completed) {
            this.running = running;
            this.completed = completed;
        }
    }

    // ตัวนับถูกแก้จากหลาย Worker จึงใช้ synchronized กับเมธอด
    private int completed = 0;
    private int running = 0;

    public synchronized void recordCompletion(Job job) {
        // นับงานที่ทำเสร็จแล้ว
        completed++;
    }

    public synchronized int completedCount() {
        return completed;
    }

    public synchronized void jobStarted() {
        // เพิ่มจำนวนงานที่กำลังทำ
        running++;
    }

    public synchronized void jobFinished() {
        // ลดจำนวนงานที่กำลังทำ
        running--;
    }

    public synchronized int runningCount() {
        return running;
    }

    public synchronized Snapshot snapshot() {
        // คัดลอกค่าปัจจุบันให้ Monitor อ่านเป็นชุดเดียว
        return new Snapshot(running, completed);
    }

    public void printSummary(List<Job> allJobs, long makespanMs) {
        long totalWaiting = 0;
        long totalTurnaround = 0;
        long totalResourceWait = 0;
        int resourceJobCount = 0;

        System.out.println("\n================ SUMMARY REPORT ================");
        System.out.printf("%-6s %-8s %-8s %-8s %-8s %-8s %-8s%n",
                "Job", "Arrived", "Start", "End", "WT", "RW", "TAT");

        for (Job job : allJobs) {
            long wt = job.startMs - job.actualArrivalMs;
            long tat = job.completionMs - job.actualArrivalMs;
            long rw = (job.resource != ResourceType.NONE) ? job.resourceWaitMs : 0;

            totalWaiting += wt;
            totalTurnaround += tat;

            if (job.resource != ResourceType.NONE) {
                totalResourceWait += rw;
                resourceJobCount++;
            }

            System.out.printf("%-6s %-8d %-8d %-8d %-8d %-8d %-8d%n",
                    job.id, job.actualArrivalMs, job.startMs, job.completionMs, wt, rw, tat);
        }

        int n = allJobs.size();
        long avgWT = (n > 0) ? Math.round((double) totalWaiting / n) : 0;
        long avgTAT = (n > 0) ? Math.round((double) totalTurnaround / n) : 0;
        long avgRW = (resourceJobCount > 0) ? Math.round((double) totalResourceWait / resourceJobCount) : 0;
        double throughput = (makespanMs > 0) ? (n * 1000.0) / makespanMs : 0.0;

        System.out.println("------------------------------------------------");
        System.out.println("Average Waiting Time      : " + avgWT + " ms");
        System.out.println("Average Turnaround Time   : " + avgTAT + " ms");
        System.out.printf("Throughput                : %.2f jobs/sec%n", throughput);
        System.out.println("Average Resource Wait Time: " + avgRW + " ms");
        System.out.println("================================================");
    }
}
