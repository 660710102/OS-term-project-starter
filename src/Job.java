/**
 * ข้อมูลของงานหนึ่งชิ้น
 *
 * ฟิลด์ทั้งหมดในไฟล์นี้มาจากไฟล์ workload CSV โดยตรง และถูกกำหนดครั้งเดียว
 * ตอนโหลด จึงประกาศเป็น final และปลอดภัยเมื่อหลาย Thread อ่านพร้อมกัน
 */

public class Job {

    /** สถานะของ Job ในระบบ แยกจาก java.lang.Thread.State โดยตั้งใจ */
    public enum State {
        ARRIVED,
        READY,
        RUNNING,
        WAITING_RESOURCE,
        COMPLETED
    }

    // กำหนด priority และ sequence ให้เป็นค่ามากที่สุด (Integer.MAX_VALUE)
    // เพื่อป้องกันไม่ให้ POISON_PILL แซงคิวงานจริงตอนรันโหมด Priority
    // งานพิเศษสำหรับบอก Worker ว่าไม่มีงานใหม่แล้ว

    public static final Job POISON_PILL = new Job("STOP", 0, Integer.MAX_VALUE, 0, ResourceType.NONE, 0,
            Integer.MAX_VALUE);

    // ข้อมูลจาก workload CSV ที่ไม่เปลี่ยนระหว่างรัน
    public final String id;
    public final long arrivalMs;
    public final int priority;
    public final long workMs;
    public final ResourceType resource;
    public final long resourceMs;
    public final int sequence;

    // ข้อมูลสถานะที่หลาย Thread อ่านหรือแก้ไขร่วมกัน
    public volatile long actualArrivalMs;
    public volatile long startMs;
    public volatile long resourceWaitMs;
    public volatile long completionMs;
    public volatile State state;

    public Job(String id, long arrivalMs, int priority, long workMs,
            ResourceType resource, long resourceMs, int sequence) {
        // เก็บข้อมูลพื้นฐานของงาน
        this.id = id;
        this.arrivalMs = arrivalMs;
        this.priority = priority;
        this.workMs = workMs;
        this.resource = resource;
        this.resourceMs = resourceMs;
        this.sequence = sequence;
        // งานเริ่มต้นในสถานะมาถึงระบบแล้ว
        this.state = State.ARRIVED;
    }

    @Override
    public String toString() {
        // แสดงรายละเอียดสั้น ๆ สำหรับ log และ debug
        return String.format("%s(priority=%d, work=%dms, %s)",
                id, priority, workMs,
                resource == ResourceType.NONE ? "no resource"
                        : resource + " " + resourceMs + "ms");
    }
}
