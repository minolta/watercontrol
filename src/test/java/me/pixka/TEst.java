package me.pixka;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.pixka.c.HttpControl;
import me.pixka.c.PiDevice;
import me.pixka.data.ISODateAdapter;
import me.pixka.device.c.DeviceControl;
import me.pixka.device.c.DeviceconfigControl;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Device;
import me.pixka.device.d.Deviceconfig;
import me.pixka.device.d.Ds18data;
import me.pixka.o.Ds;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TEst {

	@Autowired
	private HttpControl http;

	@Autowired
	private PiDevice pi;
	@Autowired
	private DeviceconfigControl dc;
	@Autowired
	private DeviceControl dic;

	@Autowired
	private DeviceconfigUtil ddd;
	private String url = "http://61.19.255.23:3333/getdeviceconfigmac/";

	private Deviceconfig currentconfig = null, deviceconfig = null;
	private static Gson g = new GsonBuilder().registerTypeAdapter(Date.class, new ISODateAdapter()).create();

	@Test
	public void xxxx() throws IOException {

		Ds re = ddd.read18Ds("http://pixka.me:3333/readds18b20/", 9L);

	}

	public void saveConfigtoDevice() {

		Deviceconfig d = dc.findRefId(deviceconfig.getId());
		if (d == null) {// ถ้าไม่เจอให้เก็บเข้าระบบ
			try {
				Device device = dic.findByRefid(deviceconfig.getDevice_id());

				if (device == null) {
					System.out.println("not found device:" + device);
					device = deviceconfig.getDevice();
					device.setRefid(device.getId());
					device = (Device) dic.save(device);
					deviceconfig.setDevice(device);
					System.out.println("New Device :" + device);
				} else {
					deviceconfig.setDevice(device);
				}

				deviceconfig.setRefid(deviceconfig.getId());
				deviceconfig = dc.save(deviceconfig);
				System.out.println("New Deviceconfig");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("End of save");
	}

}
