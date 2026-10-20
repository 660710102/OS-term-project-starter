/**
 * Thread ที่รายงานสถานะระบบเป็นระยะ
 *
 * ข้อกำหนดจากโจทย์ (หัวข้อ 10):
 * - รายงานประมาณทุก 1,000 ms ไม่ต้องแม่นตรงทุกครั้ง
 * - อย่างน้อยต้องมี ready, running, completed และสถานะการใช้ resource
 * - ข้อมูลที่อ่านต้องเป็น snapshot ที่ปลอดภัย
 * โดยเฉพาะตัวนับ running ซึ่ง Worker หลายตัวเพิ่ม/ลดพร้อมกัน
 * - ห้ามอ่าน collection หรือตัวนับที่กำลังถูกแก้ไขโดยไม่มีการป้องกัน

 * ให้พิมพ์ผ่าน logger.monitor(ready, running, completed, resources.status())
 * เพื่อให้รูปแบบตรงกับกลุ่มอื่น
 *
 * ข้อควรคิด: ตัวนับ running ควรอยู่ที่ไหน ใครเป็นคนเพิ่มและลด
 * และจะอ่านพร้อมกับ ready กับ completed ให้เป็นภาพเดียวกันได้อย่างไร
 */

public class Monitor extends Thread {

    // แหล่งข้อมูลสถานะที่ต้องอ่าน
    private final ReadyQueue readyQueue;
    private final ResourceManager resources;
    private final Statistics statistics;
    private final ProjectLogger logger;
    // flag สำหรับหยุดลูป Monitor อย่างปลอดภัย
    private volatile boolean running = true;

    public Monitor(ReadyQueue readyQueue, ResourceManager resources,
            Statistics statistics, ProjectLogger logger) {
        // ตั้งชื่อ Thread เพื่อให้อ่าน log ได้ง่าย
        super("monitor");
        this.readyQueue = readyQueue;
        this.resources = resources;
        this.statistics = statistics;
        this.logger = logger;
    }

    public void shutdown() {
        // เปลี่ยน flag และปลุก Thread ที่กำลัง sleep
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            while (running) {
                // รายงานสถานะประมาณทุก 1 วินาที
                Thread.sleep(1000);
                if (!running) {
                    break;
                }
                // อ่าน snapshot ของค่าที่เปลี่ยนจากหลาย Thread
                int ready = readyQueue.size();
                Statistics.Snapshot snapshot = statistics.snapshot();
                String resStatus = resources.status();

                logger.monitor(ready, snapshot.running, snapshot.completed, resStatus);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
