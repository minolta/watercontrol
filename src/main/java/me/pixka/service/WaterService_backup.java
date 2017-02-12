package me.pixka.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import me.pixka.c.Ds18b20;
import me.pixka.c.Led;
import me.pixka.c.PiDevice;
import me.pixka.device.c.DeviceControl;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Device;
import me.pixka.device.d.Deviceconfig;
import me.pixka.device.d.Watertiming;
import me.pixka.device.s.WatertimingService;
import me.pixka.o.Ds;

@Service
public class WaterService_backup {

	@Autowired
	private Led led;
	@Autowired
	private Ds18b20 ds;
	@Autowired
	private PiDevice pi;

	@Autowired
	private WatertimingService ws;
	@Autowired
	private DeviceconfigUtil dcfu;

	@Autowired
	private DeviceControl dc;

	private String url = "http://61.19.255.23:3333/getdeviceconfigmac/";
	private String baseUrl = "http://61.19.255.23:3333/";
	private Deviceconfig deviceconfig;
	private BigDecimal def = new BigDecimal("90.0");
	private GpioController gpio = null;
	private GpioPinDigitalOutput pin0;
	private GpioPinDigitalOutput pin1;
	// config ของการเปิดน้ำ
	private Watertiming wtconfig;
	// ค่าอุณหภูมิปัจจุบัน
	private BigDecimal value;
	private boolean opened;

	public void setup() {
		try {
			gpio = GpioFactory.getInstance();
			pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyDelay0", PinState.HIGH);
			pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyDelay1", PinState.HIGH);
			pin0.setShutdownOptions(true, PinState.HIGH);
			pin1.setShutdownOptions(true, PinState.HIGH);
			System.out.println("Setup device done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// อ่าน DS18B20
	public BigDecimal read() {
		BigDecimal v = null;
		try {
			// อ่านจากตัวอื่น
			if (deviceconfig != null && deviceconfig.getReaddsfrom_id() != null) {
				v = readDSbyDeviceconfig();
				// System.out.println("Read from DS config: value = " + v);
			} else {
				// อ่านจากตัวเอง
				v = readDsByDevice();
				// System.out.println("Read from DS local Sensor : value = " +
				// v);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("*****************************************************************");
		System.out.println("****************** DS18B20 value : " + v + " **********************");
		System.out.println("*****************************************************************");
		return v;

	}

	/**
	 * Read from Local Sensor
	 * 
	 * @return
	 */
	private BigDecimal readDsByDevice() {
		try {
			BigDecimal v = ds.read();
			System.out.println("******************************************");
			System.out.println("Read Local Sensor DS18B20 : value " + v);
			System.out.println("******************************************");
			return v;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ถ้ามี Device config จะอ่าน จาก Device config
	 * 
	 * @return
	 */
	private BigDecimal readDSbyDeviceconfig() {

		// System.out.println("<===================Read from Device config : ");
		try {
			Long id = deviceconfig.getReaddsfrom_id();
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

			BigDecimal v = readDsByDevice();
			System.out.println("Not found read from try to read local sensor :" + v);
			return v;
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("************** Can not read value : **************");
		return null;
	}

	/**
	 * เอาไว้ตรวจสอบว่าอยู่ในช่วงที่ต้องการเปล่า
	 */
	@Async
	public void run() {
		try {
			while (true) {

				// อุณหภูมิปัจจุบัน
				// System.out.println("Read value : " + value);
				if (value == null) {
					System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					System.out.println("|||||||||||||||||  No Tmp Value ||||||||||||||||||||||");
					System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				} else {

					// Run
					if (wtconfig != null && (wtconfig.getTmplow() != null || wtconfig.getTmphigh() != null)) {
						runWaterconfig(wtconfig.getOpenwatertime(), wtconfig.getWaittime());

					} else {
						// ใช้ default config
						/*
						 * if (deviceconfig == null) {// ไม่มี config ให้ใช้
						 * runDevice(); } else { runDeviceconfig(); }
						 */
						// Run จาก DEfault เลย

						runDevice();
					}
				}

				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ใช้สำหรับ Run device config
	 */
	private void runDeviceconfig() {
		BigDecimal ht = deviceconfig.getHt();
		if (ht != null && ht.signum() != 0)
			if (checkvalue(value, ht) > 0) {
				open();
			} else {
				if (opened) {
					close();

				}
			}

		// resetLed();
	}

	/**
	 * Run from Default config
	 * 
	 * @throws InterruptedException
	 */
	private void runDevice() {
		try {
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!! Run Default config : " + def + " !!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			if (checkvalue(value, def) > 0) {
				// ถ้ามากกว่า
				open();
				Thread.sleep(2000); // เปิด สองวิ
				close();
				Thread.sleep(60000);
			} else {
				if (opened) {
					close();

				}
			}

			// resetLed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runWaterconfig(Long open, Long wait) {
		// ใช้ water config
		System.out.println("===================########### RUN With waterconfig ##########====================");
		open();
		runWaterconfigTime(open, wait);
		// resetLed();
	}

	/**
	 * ใช้สำหรับอ่านค่าจาก DS18B20
	 */
	@Async
	public void readDs() {
		while (true) {
			value = read();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Task to load Device config
	 */
	@Async
	public void loadDeviceConfig() {
		while (true) {

			try {
				System.out.println("===========================================================");
				System.out.println("============== Start to download Device config ============");
				System.out.println("===========================================================");
				Deviceconfig dvcf = dcfu.loadfromhttp(url, pi.wifi());
				if (dvcf == null) {
					System.out.println("####################################################################");
					System.out.println("####################################################################");
					System.out.println("* Can not get Device config from server: try to load local database *");
					System.out.println("####################################################################");
					System.out.println("####################################################################");
					dvcf = dcfu.loadfromdevice();
				} else {
					dvcf.setRefid(dvcf.getId());
				}

				if (dvcf != null) {
					deviceconfig = dvcf;
				} else {
					System.out.println("####################################################################");
					System.out.println(" ======>>>>>>      Not found devicefig  !!!!!!!!!!!!!!!!!!!");
					System.out.println("####################################################################");
				}

				if (dvcf != null)
					dcfu.saveConfigtoDevice(dvcf);
				else {
					System.out.println("No save deviceconfig to device");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Load water config ใช้สำหรับ LOAD water config
	 */
	@Async
	public void loadWaterConfig() {

		while (true) {

			try {
				System.out.println("Load Warter Config");
				// load ช่วงเวลา
				if (deviceconfig != null && deviceconfig.getDevice() != null) {
					if (deviceconfig.getDevice().getId() != null) {
						Watertiming wttt = this.loadWatertiming(deviceconfig.getDevice_id(), value);
						if (wttt != null) {
							System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
							System.out.println(" Waterconfig from Server:" + wttt);
							System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
							wtconfig = wttt;
							wtconfig = saveWatertiming(wttt); // ถ้าเป็นอันใหม่
																// ก็ต้อง Save
							// ไว้ข้างใน Device
						} else {
							System.out.println("XXXXXXXXXXXXX  Not Water config At this tmp" + value + " XXXXXXXXXXXX");

							// Load local
							// wtconfig =
							// ws.findlast(deviceconfig.getDevice_id(), value);
							wtconfig = ws.findlast(value);
							System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
							System.out.println("Load local Water config is=>> :" + wtconfig);
							System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

						}
						// ถ้า wttt ไม่มี ก็ใช้อันเดิม
					}
				} else {
					System.out.println(
							"**************************************************************************************");
					System.out.println(
							"*************                                                   **********************");
					System.out.println(
							"************* Not have Device ID try to load Local Water config **********************");
					System.out.println(
							"*************								                    **********************");
					System.out.println(
							"**************************************************************************************");

					wtconfig = this.findLocalWaterconfig(value);

					System.out.println("Local local Water config result: " + wtconfig);
					if (wtconfig == null) {
						System.out.println(
								"************************************************************************************");
						System.out.println(
								"***********************      Water config not Found    *****************************");
						System.out.println(
								"************************************************************************************");
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * ใช้สำหรับ save wttm ลงใน device
	 * 
	 * @param wtm
	 * @return
	 */

	public Watertiming saveWatertiming(Watertiming wtm) {
		Watertiming indevice = ws.findByRefid(wtm.getId());
		if (indevice != null) {
			return indevice;
		}
		Device device = dc.findByRefid(wtm.getDevice_id());
		if (device == null) {
			Device d = wtm.getDevice();
			d.setRefid(d.getId());
			d = dc.save(d);
			wtm.setDevice(d);
		}
		wtm.setRefid(wtm.getId());
		wtm = ws.save(wtm);
		return wtm;
	}

	public int checkvalue(BigDecimal s, BigDecimal e) {
		System.out.println("value for check:" + s + " " + e);
		return s.compareTo(e);
	}

	/**
	 * ปิดน้ำ
	 */
	public void close() {
		System.out.println("close");
		led.close();
		if (pin0 == null) {
			setup();
		}

		pin0.high();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		pin1.high();
		// resetLed();
		opened = false;
		led.open();
	}

	/**
	 * เปิดน้ำ
	 */
	public void open() {
		led.close();
		System.out.println("open");
		if (pin0 == null) {
			setup();
		}
		opened = true;
		pin1.low();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		pin0.low();
		led.open();
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
			Thread.sleep(ot);
			close();
			Thread.sleep(wtime); // หยุดไปจนกว่าจะมีอีกรอบ
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // เปิดตามเวลาที่กำหนด
		finally {
			if (opened)
				close();
			/// resetLed();
		}

	}

	/**
	 * Load ค่าเปิดปิดสำหรับ เปิดน้ำ ต่างๆ ถ้า load มาจาก net ได้ต้องไปตรวจสอบ
	 * ว่า มีในระบบแล้วหริอยังถ้ามีแล้วก็สงออะไรเลย ถ้าไม่มี ก็ Save ลงในระบบ
	 * 
	 * @param id
	 * @return
	 */
	public Watertiming loadWatertiming(Long id) {

		Watertiming ww = dcfu.loadWatertiming(this.baseUrl + "water/readtiming/", id);
		if (ww != null) {
			ww = saveWaterconfigtolocal(ww);
			return ww;
		}
		BigDecimal tmp = value;
		ww = findLocalWaterconfig(tmp);
		return ww;

		// ถ้า ติดต่อเนตไม่ได้ก็ load waterconfig จาก local

	}

	private Watertiming findLocalWaterconfig(BigDecimal tmp) {

		Watertiming wt = ws.findlast(tmp);
		return wt;
	}

	/**
	 * ใช้สำหรับ Save Waterconfig ลงเครื่อง
	 * 
	 * @param ww
	 * @return
	 */
	private Watertiming saveWaterconfigtolocal(Watertiming ww) {

		Watertiming ref = ws.findByRefid(ww.getId());

		if (ref != null) {
			System.out.println("Have local WC");
			return ref;
		}
		System.out.println("New Watertiming Config");
		ww.setRefid(ww.getId());
		Device device = finddevice(ww.getDevice_id());
		ww.setDevice(device);
		return ws.save(ww);
	}

	private Device finddevice(Long id) {
		Device df = (Device) dc.findById(id);
		if (df == null) {
			df = dc.save(df);
		}
		return df;
	}

	/**
	 * Load ค่าเปิดปิดสำหรับ เปิดน้ำ ต่างๆ
	 * 
	 * @param id
	 * @return
	 */
	public Watertiming loadWatertiming(Long id, BigDecimal v) {

		Watertiming wwwww = dcfu.loadWatertiming(this.baseUrl + "water/readtiming/", id, v);

		if (wwwww != null) {

			wwwww = saveWaterconfigtolocal(wwwww);

		} else {// try to load from local
			System.out.println("Load from Local");
			wwwww = ws.findlast(id, v);
		}

		System.out.println("WC:" + wwwww);
		return wwwww;
	}

	public void resetLed() {
		// led.clear();
		led.close();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		led.open();
		led.clear();
	}
}
