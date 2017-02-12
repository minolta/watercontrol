package me.pixka;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import me.pixka.device.c.DeviceControl;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Device;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Tbytmp {

	@Autowired
	private DeviceControl dc;
	@Autowired
	private DeviceconfigUtil dcfu;
	@Test
	public void test() {
		 dcfu.loadWatertiming("http://localhost:3333/water/readtiming/",1L, new BigDecimal("95.0"));

	}
}
