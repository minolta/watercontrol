package me.pixka;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import me.pixka.c.Ds18b20;
import me.pixka.c.Font;
import me.pixka.c.Led;
import me.pixka.c.PiDevice;
import me.pixka.device.c.DeviceconfigUtil;
import me.pixka.device.d.Deviceconfig;

@Component
public class DisplayUnit {

	@Autowired
	private Led led;
	@Autowired
	private DeviceconfigUtil dcfu;
	@Autowired
	private PiDevice pi;
	@Autowired
	private Ds18b20 ds;
	private BigDecimal data;
	private BigDecimal last=BigDecimal.ZERO;

	
	private String url = "http://61.19.255.23:3333/getdeviceconfigmac/";

	public Deviceconfig load() {
		Deviceconfig deviceconfig = null;
		deviceconfig = dcfu.loadfromhttp(url, pi.wifi());

		if (deviceconfig == null)
			deviceconfig = dcfu.loadfromdevice();

		return deviceconfig;
	}

	@Scheduled(fixedRate = 10*1000)
	public void display() {
		Deviceconfig deviceconfig = load();
		if (deviceconfig != null) {// ปิดจอ
			if (!deviceconfig.getLcdon()) {
				led.print("   ");
				return;
			}
		}
		try {
			data = ds.read();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if(data==null)
		{
			led.print("***");
			return ;
		}
		System.out.println("print Value");
		if (data.compareTo(BigDecimal.ZERO) != 0) {// have data
			String value = data.setScale(0, RoundingMode.HALF_UP) + "";
			String f = "=";
			int cp = data.compareTo(last);
			if (cp > 0)
				f = "^";
			else if (cp < 0)
				f = "v";

			// led.letter((short)0, (short)f.charAt(0), Font.CP437_FONT, true);
			led.print(f + value);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			led.print(f + value);
			last = data;
		} else {
			led.print("---");
		}

	}

}
