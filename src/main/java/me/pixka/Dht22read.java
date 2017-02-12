package me.pixka;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import me.pixka.c.DHT22;
import me.pixka.c.HttpControl;
import me.pixka.c.Led;
import me.pixka.c.PiDevice;

@Component
public class Dht22read {

	@Autowired
	private DHT22 dht22;
	@Autowired
	private HttpControl http;

	@Autowired
	private Led led;

	@Autowired
	private PiDevice device;
	private SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");

	@Scheduled(fixedDelay = 60 * 1000)
	public void read() {
		dht22.read();

		BigDecimal t = dht22.getT();
		BigDecimal h = dht22.getH();

	//	led.showMessage("H:" + h + " T:" + t + " " + df.format(new Date()));
		if (t != null && h != null) {
			try {
				String m = device.wifi();
				String url = "http://61.19.255.23:3333/add?t=" + t + "&h=" + h + "&m=" + m;
				System.out.println("Send data: " + url);
				http.get(url);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Can not send DHT22 Data:");
			}
		}
	}

}
