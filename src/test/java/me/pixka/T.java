package me.pixka;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import me.pixka.device.c.DeviceControl;
import me.pixka.device.d.Device;

@RunWith(SpringRunner.class)
@SpringBootTest
public class T {

	@Autowired
	private DeviceControl dc;

	@Test
	public void test() {
		Device device = new Device();
		device.setName("1");
		device.setId(9L);
		device = (Device) dc.save(device);
		System.out.println(device);
		List<Device> list = dc.findAll();
		for (Device d : list) {
			
			System.out.println(d);
		}

	}
}
