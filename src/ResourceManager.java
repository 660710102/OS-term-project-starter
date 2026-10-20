import java.util.concurrent.Semaphore;

/**
 * ควบคุมสิทธิ์การใช้ทรัพยากรร่วมของทั้งระบบ
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 * - หัวข้อ 5: ใช้ Semaphore ควบคุม PRINTER และ DATABASE
 * จำนวน permit มาจาก command line (Config)
 * ในส่วนบังคับให้สร้าง Semaphore แบบ fair = true
 * - Worker ทุกตัวต้องใช้ ResourceManager object เดียวกัน
 * - หัวข้อ 7: permit ต้องไม่สูญหายหรือค้าง แม้เกิด exception
 * หรือถูก interrupt ระหว่างถือ resource
 *
 * คำถามที่จะถูกถามใน Demo:
 * - ทำไมต้อง fair = true และถ้าเปลี่ยนเป็น false จะเกิดอะไรขึ้น
 * - ถ้า Thread ถูก interrupt หลัง acquire สำเร็จแต่ก่อน release
 * โค้ดของกลุ่มยังคืน permit ได้หรือไม่
 */
public class ResourceManager {

    // จำนวน permit สูงสุดของ resource แต่ละชนิด
    private final int maxPrinterPermits;
    private final int maxDatabasePermits;
    // Semaphore ใช้จำกัดจำนวนงานที่ใช้ resource พร้อมกัน
    private final Semaphore printerSemaphore;
    private final Semaphore databaseSemaphore;
    private final Object printerEventLock = new Object();
    private final Object databaseEventLock = new Object();

    public ResourceManager(int printerPermits, int databasePermits) {
        // ใช้ fair semaphore เพื่อให้คิวรอมีความเป็นธรรม
        this.maxPrinterPermits = printerPermits;
        this.maxDatabasePermits = databasePermits;
        this.printerSemaphore = new Semaphore(printerPermits, true);
        this.databaseSemaphore = new Semaphore(databasePermits, true);
    }

    public void acquire(ResourceType type) throws InterruptedException {
        // จอง permit ก่อนเริ่มใช้ resource
        if (type == ResourceType.PRINTER) {
            printerSemaphore.acquire();
        } else if (type == ResourceType.DATABASE) {
            databaseSemaphore.acquire();
        }
    }

    public void release(ResourceType type) {
        // คืน permit หลังใช้ resource เสร็จ
        if (type == ResourceType.PRINTER) {
            printerSemaphore.release();
        } else if (type == ResourceType.DATABASE) {
            databaseSemaphore.release();
        }
    }

    /**
     * Serializes acquire/release event logging for one resource type without
     * holding the event lock while waiting for a permit.
     */
    public Object eventLock(ResourceType type) {
        if (type == ResourceType.PRINTER) {
            return printerEventLock;
        }
        if (type == ResourceType.DATABASE) {
            return databaseEventLock;
        }
        return this;
    }

    public String status() {
        // คำนวณจำนวน permit ที่กำลังถูกใช้
        int printerInUse = maxPrinterPermits - printerSemaphore.availablePermits();
        int dbInUse = maxDatabasePermits - databaseSemaphore.availablePermits();
        return "printer=" + printerInUse + "/" + maxPrinterPermits +
                " database=" + dbInUse + "/" + maxDatabasePermits;
    }
}
