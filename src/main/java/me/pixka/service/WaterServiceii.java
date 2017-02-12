package me.pixka.service;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import me.pixka.c.Ds18b20;
import me.pixka.c.Led;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Deviceconfig;
import me.pixka.device.d.Watertiming;
import me.pixka.device.s.DeviceconfigService;
import me.pixka.device.s.WatertimingService;
import me.pixka.o.Ds;
/**
 * ใช้สำหรับ ควมคุบน้ำ
 * @author kykub
 *
 */
@Component
public class WaterServiceii {

	@Autowired
	private WatertimingService service;
	@Autowired
	private DeviceconfigService dvs;
	@Autowired
	private DeviceconfigUtil dcfu;
	@Autowired
	private Led led;
	@Autowired
	private Ds18b20 ds18b20;
	private long nextrun = 10 * 1000;
	private String baseUrl = "http://61.19.255.23:3333/";

	private GpioController gpio = null;
	private GpioPinDigitalOutput pin0;
	private GpioPinDigitalOutput pin1;
	private BigDecimal lasttmp;
	private Date lastupdate;
	private long timeout = 10 * 60 * 1000; // 10 นาที

	@Async
	public void run() {
		setup();
		while (true) {

			try {
				BigDecimal t = null;
				Deviceconfig deviceconfig = getConfig();
				if (deviceconfig != null && deviceconfig.getReaddsfrom_id() != null) {
					t = readBydeviceconfig(deviceconfig.getReaddsfrom_id());
					if (t != null) {
						masklastupdate(t);
					}
				}
				t = loadtime();
				if (t == null)
					t = readlocal();

				if (t == null)
					continue;
				Watertiming wt = service.findlast(t);
				if (wt != null) {
				
					runConfig(wt);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(nextrun);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * ใช้ตรวจสอบ ว่ายังใช้ค่าเก่าที่อ่านได้หรือเปล่า ถ้าเวลาไม่ถึง timeout
	 * ก็ให้ return lasttmp ออกไปเลย
	 * 
	 * @return
	 */
	private BigDecimal loadtime() {

		Date n = new Date();
		long p = n.getTime() - lastupdate.getTime();
		if (p > timeout)
			return null;
		return lasttmp;
	}

	/**
	 * ใช้สำหรับบอกว่าอ่านล่าสุดเมื่อไหร่เพื่อเวลาที่อ่านไม่ได้ จะเอาไปใช้กับแทน
	 * การอ่านจากระบบ ในเวลาที่ อ่านจาก เครื่องอื่นไม่ได้
	 * 
	 * @param t
	 */
	private void masklastupdate(BigDecimal t) {
		lastupdate = new Date();
		lasttmp = t;

	}

	private BigDecimal readBydeviceconfig(Long readdsfrom_id) {

		return readother(readdsfrom_id);
	}

	/**
	 * ดึงอันล่าสุดมา
	 * 
	 * @return
	 */
	private Deviceconfig getConfig() {

		return dvs.last();
	}

	private void runConfig(Watertiming wt) {

		runWaterconfigTime(wt.getOpenwatertime(), wt.getWaittime());

	}

	public void runWaterconfigTime(Long open, Long wait) {
		System.out.println("################################################################################### ");
		System.out.println("############################ Start Runconfig : #################################### ");
		System.out.println("################################################################################### ");
		/**
		 * ระบบจะทำการเปิดตามเวลา ถ้ามีการ set ไว้
		 */
		System.out.println("==================== Run  timing Config ==========================");
		System.out.println("============ OT:" + open + " WT:" + wait + " =====================");
		System.out.println("==================================================================");
		Long ot = open; // เวลาที่เปิด
		Long wtime = wait;// เวลาที่รอตรจสอบอีกครั้ง
		try {
			open();
			Thread.sleep(ot);
			close();
			Thread.sleep(wtime); // หยุดไปจนกว่าจะมีอีกรอบ
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // เปิดตามเวลาที่กำหนด
		finally {

			close();
		}

	}

	public void setup() {
		try {
			gpio = GpioFactory.getInstance();
			pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyDelay0", PinState.HIGH);
			pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyDelay1", PinState.HIGH);
			pin0.setShutdownOptions(true, PinState.HIGH);
			pin1.setShutdownOptions(true, PinState.HIGH);
			pin0.setState(PinState.LOW); // เปิดไฟเข้าเลย
			
			led.open();
			led.setRun(false);
			System.out.println("Setup device done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * เปิดน้ำ
	 */
	public void open() {
		System.out.println("open");
		led.setRun(true);
		pin1.low();
		led.setRun(false);

	}

	/**
	 * ปิดน้ำ
	 */
	public void close() {
		System.out.println("close");
		led.setRun(true);
		pin1.high();
		led.setRun(false);
	}

	private BigDecimal readlocal() {
		try {
			return ds18b20.read();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private BigDecimal readother(Long id) {

		// System.out.println("<===================Read from Device config : ");
		try {
			// System.out.println("Device Config:" + deviceconfig + " ID :" +
			// id);
			if (id != null) {
				Ds dsvalue = dcfu.read18Ds(baseUrl + "readds18b20/", id);
				System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				System.out.println("Read from Other device : ID:" + id + " value :" + dsvalue);
				System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				if (dsvalue != null)
					return dsvalue.getTmp();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("************** Can not read value : **************");
		return null;
	}
}
