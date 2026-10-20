
/**
 * ค่าที่รับมาจาก command line
 *
 * ไฟล์นี้เป็นโค้ดตั้งต้นที่อาจารย์แจก ไม่ต้องแก้
 * เหตุผลที่แจกให้: ผู้ตรวจต้องสั่งรันทุกกลุ่มด้วยคำสั่งรูปแบบเดียวกัน
 * และต้องรู้ชัดว่ากำลังรันด้วยค่าใด จึงไม่มีค่า default ซ่อนอยู่เลย
 *
 * รูปแบบการใช้งาน:
 * java -cp out Main workloads/jobs_standard.csv priority 3 1 2
 */

import java.util.Locale;

public final class Config {

    // ข้อความวิธีเรียกโปรแกรมเมื่อผู้ใช้ใส่ argument ผิด
    public static final String USAGE = "Usage: java -cp out Main <workload.csv> <fcfs|priority> <workers> <printerPermits> <databasePermits>\n"
            + "  workload.csv      ไฟล์ชุดงานทดสอบ\n"
            + "  fcfs | priority   นโยบายการจัดลำดับงาน\n"
            + "  workers           จำนวน Worker Thread (ตั้งแต่ 1 ขึ้นไป)\n"
            + "  printerPermits    จำนวนสิทธิ์ใช้ PRINTER พร้อมกัน (ตั้งแต่ 1 ขึ้นไป)\n"
            + "  databasePermits   จำนวนสิทธิ์ใช้ DATABASE พร้อมกัน (ตั้งแต่ 1 ขึ้นไป)\n"
            + "\n"
            + "ตัวอย่าง: java -cp out Main workloads/jobs_standard.csv priority 3 1 2";

    /** นโยบายการจัดลำดับงาน */
    public enum Policy {
        FCFS,
        PRIORITY
    }

    // เก็บค่าตั้งต้นของการรันหนึ่งครั้ง
    public final String workloadPath;
    public final Policy policy;
    public final int workers;
    public final int printerPermits;
    public final int databasePermits;

    private Config(String workloadPath, Policy policy, int workers,
            int printerPermits, int databasePermits) {
        // คัดลอกค่าที่ตรวจสอบแล้วมาเก็บใน Config
        this.workloadPath = workloadPath;
        this.policy = policy;
        this.workers = workers;
        this.printerPermits = printerPermits;
        this.databasePermits = databasePermits;
    }

    /**
     * แปลง argument จาก main() เป็น Config
     *
     * @throws IllegalArgumentException เมื่อจำนวนหรือค่าของ argument ไม่ถูกต้อง
     *                                  ข้อความของ exception
     *                                  เหมาะกับการแสดงต่อผู้ใช้โดยตรง
     */
    public static Config parse(String[] args) {
        // โปรแกรมต้องรับ argument ให้ครบ 5 ค่า
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "ต้องใส่ argument ให้ครบ 5 ตัว แต่ได้รับ " + args.length + " ตัว");
        }

        // ตัดช่องว่างของชื่อไฟล์ก่อนใช้งาน
        String workloadPath = args[0].trim();
        if (workloadPath.isEmpty()) {
            throw new IllegalArgumentException("ชื่อไฟล์ workload เป็นค่าว่าง");
        }

        // แปลงข้อความนโยบายเป็น enum
        Policy policy;
        String policyText = args[1].trim().toLowerCase(Locale.ROOT);
        if (policyText.equals("fcfs")) {
            policy = Policy.FCFS;
        } else if (policyText.equals("priority")) {
            policy = Policy.PRIORITY;
        } else {
            throw new IllegalArgumentException(
                    "นโยบายต้องเป็น fcfs หรือ priority เท่านั้น แต่พบ \"" + args[1].trim() + "\"");
        }

        // จำนวนทุกประเภทต้องเป็นจำนวนเต็มอย่างน้อย 1
        int workers = parseAtLeastOne(args[2], "workers");
        int printerPermits = parseAtLeastOne(args[3], "printerPermits");
        int databasePermits = parseAtLeastOne(args[4], "databasePermits");

        return new Config(workloadPath, policy, workers, printerPermits, databasePermits);
    }

    private static int parseAtLeastOne(String text, String name) {
        // แปลงข้อความเป็นจำนวนเต็มและตรวจค่าขั้นต่ำ
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    name + " ต้องเป็นจำนวนเต็ม แต่พบ \"" + text.trim() + "\"");
        }
        if (value < 1) {
            throw new IllegalArgumentException(
                    name + " ต้องมีค่าตั้งแต่ 1 ขึ้นไป แต่พบ " + value);
        }
        return value;
    }

    /** ข้อความบรรทัดเดียวสำหรับบันทึกลง log ว่ารันด้วยค่าใด */
    public String describe() {
        return String.format("workload=%s policy=%s workers=%d printer=%d database=%d",
                workloadPath, policy.name().toLowerCase(Locale.ROOT), workers, printerPermits, databasePermits);
    }
}
