package me.pixka;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import me.pixka.c.Ds18b20;
import me.pixka.c.Font;
import me.pixka.c.HttpControl;
import me.pixka.c.Led;
import me.pixka.c.PiDevice;
import me.pixka.data.ISODateAdapter;
import me.pixka.device.c.DeviceControl;
import me.pixka.device.c.DeviceconfigControl;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Device;
import me.pixka.device.d.Deviceconfig;

/**
 * ใช้สำหรับ ส่ง ค่า ความร้อน ไปยัง service
 * 
 * @author kykub
 *
 */
@Component
public class ReaddsTask {
	@Autowired
	private Ds18b20 ds;
	@Autowired
	private HttpControl http;

	@Autowired
	private PiDevice pi;

	@Autowired
	private DeviceconfigControl dc;
	@Autowired
	private DeviceControl dic;

	@Autowired
	private DeviceconfigUtil dcfu;
	private String url = "http://61.19.255.23:3333/getdeviceconfigmac/";

	private Deviceconfig deviceconfig;
	private static Gson g = new GsonBuilder().registerTypeAdapter(Date.class, new ISODateAdapter()).create();
	private BigDecimal def = new BigDecimal("95.0");
	private GpioController gpio = null;

	// private BigDecimal data = BigDecimal.ZERO;
	// private BigDecimal last = BigDecimal.ZERO;
	private Deviceconfig currentconfig = null;

	private GpioPinDigitalOutput pin0;
	private GpioPinDigitalOutput pin1;

	public ReaddsTask() {

		// pin.high();
		// gpio.shutdown();
	}

	public void setup() {
		try {
			gpio = GpioFactory.getInstance();
			pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyDelay0", PinState.HIGH);
			pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyDelay1", PinState.HIGH);
			pin0.setShutdownOptions(true, PinState.HIGH);
			pin1.setShutdownOptions(true, PinState.HIGH);
			System.out.println("Start Thread");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BigDecimal read() {
		BigDecimal value = null;
		try {
			value = ds.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;

	}

	@Scheduled(fixedDelay = 5000)
	public void check() {
		System.out.println("check ");
		BigDecimal value = read();
		if (value == null)
			return; // อ่านค่าไม่ได้ก็ออกเลย

		if (deviceconfig == null) {// ไม่มี config ให้ใช้ 95องค์ศา
			System.out.println("Not found config check with default " + def);
			if (checkvalue(value, def) > 0) {
				// ถ้ามากกว่า
				open();
			} else
				close();
		} else {
			// System.out.println("Have config:" + deviceconfig);
			BigDecimal ht = deviceconfig.getHt();
			System.out.println("HT " + ht);
			if (ht.signum() != 0)
				if (checkvalue(value, ht) > 0) {
					open();
				} else {
					close();
				}
		}
	}

	@Scheduled(fixedRate = 5000)
	public void load() {

		deviceconfig = dcfu.loadfromhttp(url, pi.wifi());
		if (deviceconfig == null) {
			deviceconfig = dcfu.loadfromdevice();
		} else {
			deviceconfig.setRefid(deviceconfig.getId());
		}

		if (deviceconfig == null && currentconfig == null) {
			System.out.println("Not have config");
			return;// not have config
		}

		if (currentconfig == null) {
			currentconfig = deviceconfig;
		} else {// current config ไม่เท่ากับ null

			Long crefid = currentconfig.getRefid();
			Long refid = deviceconfig.getId();
			if (crefid.intValue() != refid.intValue()) {// new config
				currentconfig = deviceconfig;
				dcfu.saveConfigtoDevice(currentconfig);
			}

		}

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
		if (pin0 == null) {
			setup();
		}

		pin0.high();
		pin1.high();
	}

	/**
	 * เปิดน้ำ
	 */
	public void open() {
		System.out.println("open");
		if (pin0 == null) {
			setup();
		}
		pin1.low();
		pin0.low();
	}

	/*
	 * @Scheduled(fixedRate = 5000)
	 * 
	 * public Deviceconfig loadconfig() { System.out.println("load config ");
	 * Deviceconfig dfs = null; try { dfs = dcfu.loadfromhttp(url, pi.wifi()); }
	 * catch (IOException e) { }
	 * 
	 * if (dfs != null) { // ถ้า load มาจาก Server ได้แล้วเทียบกับ
	 * System.out.println("config load from server ");
	 * 
	 * if (currentconfig != null) {
	 * 
	 * Long refid = currentconfig.getRefid(); if (refid != null &&
	 * !refid.equals(dfs.getId())) { deviceconfig = dfs; currentconfig = dfs;
	 * dcfu.saveConfigtoDevice(deviceconfig); } else { deviceconfig = dfs;
	 * currentconfig = dfs; dcfu.saveConfigtoDevice(deviceconfig); }
	 * 
	 * // this.saveConfigtoDevice(); } else { currentconfig = dfs; deviceconfig
	 * = dfs; dcfu.saveConfigtoDevice(deviceconfig); } }
	 * 
	 * else if (currentconfig == null) {// ถ้า load ไม่ได้ก้ดึงจาก device ที่
	 * System.out.println("Load local"); // save ไว้ dfs =
	 * dcfu.loadfromdevice(); deviceconfig = currentconfig = dfs; } else {
	 * System.out.println("Not load config use current config"); }
	 * 
	 * if (dfs != null) return dfs;
	 * 
	 * return null;
	 * 
	 * }
	 */
	// ส่งข้อมูลทุก 1 นาที
	// @Scheduled(fixedRate = 60 * 1000)
	// public void send() {
	// send("61.19.255.23", "3333", data, pi.wifi());
	// }
	//
	// public void send(String host, String port, BigDecimal tmp, String mac) {
	// String s = "http://" + host + ":" + port + "/addds?t=" + tmp + "&m=" +
	// mac;
	// try {
	// http.get(s);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// }
}
